using System.Globalization;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;
using CFQuotaMonitor.Windows.Services;
using Microsoft.Win32;
using Forms = System.Windows.Forms;
using Button = System.Windows.Controls.Button;
using KeyEventArgs = System.Windows.Input.KeyEventArgs;

namespace CFQuotaMonitor.Windows;

public partial class MainWindow : Window
{
    private sealed record Choice<T>(T Value, string Label);

    private readonly StorageService _storage = new();
    private readonly CloudflareService _cloudflare = new();
    private readonly BackupService _backup = new();
    private readonly AuthenticationService _authentication = new();
    private readonly LocalizationService _loc;
    private readonly Dictionary<string, string> _accountErrors = new();
    private readonly DispatcherTimer _backgroundTimer = new() { Interval = TimeSpan.FromSeconds(30) };
    private Forms.NotifyIcon? _tray;
    private CancellationTokenSource? _refreshCancellation;
    private DateTimeOffset _nextBackgroundRefresh = DateTimeOffset.MaxValue;
    private DateTimeOffset _deactivatedAt = DateTimeOffset.UtcNow;
    private bool _loadingControls;
    private bool _refreshing;
    private bool _exiting;
    private bool _trayNoticeShown;
    private bool _unlocked;
    private string _activePage = "dashboard";

    public MainWindow()
    {
        _loc = new LocalizationService(_storage.Settings.Language);
        InitializeComponent();
        RootGrid.FlowDirection = _loc.FlowDirection;
        Loaded += MainWindow_Loaded;
        _backgroundTimer.Tick += BackgroundTimer_Tick;
    }

    private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        LocalizeInterface();
        LoadSettingsControls();
        RebuildAccountViews();
        InitializeTray();
        ScheduleNextRefresh();
        _backgroundTimer.Start();

        if (_storage.Settings.AppLockEnabled) LockApplication();
        else _unlocked = true;

        var backgroundLaunch = Environment.GetCommandLineArgs().Any(a => a == "--background");
        if (backgroundLaunch && _storage.Settings.BackgroundRefreshEnabled)
            Hide();
        await RefreshAllAsync();
    }

    private void LocalizeInterface()
    {
        RootGrid.FlowDirection = _loc.FlowDirection;
        Title = _loc.T("app_name");
        SidebarTitle.Text = _loc.T("app_name");
        SidebarPrivacy.Text = _loc.T("privacy_desc");
        DashboardNav.Content = "◫   " + _loc.T("nav_dashboard");
        AccountsNav.Content = "☷   " + _loc.T("nav_accounts");
        TransferNav.Content = "⇄   " + _loc.T("nav_transfer");
        SettingsNav.Content = "⚙   " + _loc.T("nav_settings");
        HelpNav.Content = "?   " + _loc.T("nav_help");
        AddButton.Content = "+  " + _loc.T("add_account");
        RefreshButton.Content = _refreshing ? _loc.T("refreshing") : "↻  " + _loc.T("refresh");

        TodayOverviewLabel.Text = _loc.T("today_overview");
        UsedTotalLabel.Text = _loc.T("used_total");
        RemainingTotalLabel.Text = _loc.T("remaining_total");
        AccountCountLabel.Text = _loc.T("account_count");
        ResetTimeLabel.Text = _loc.T("reset_time");
        AccountUsageLabel.Text = _loc.T("account_usage");

        ExportTitle.Text = _loc.T("export_title");
        ExportDescription.Text = _loc.T("export_desc");
        SelectAllAccounts.Content = _loc.T("select_all");
        BackupPasswordLabel.Text = _loc.T("backup_password");
        ConfirmPasswordLabel.Text = _loc.T("confirm_password");
        ExportButton.Content = _loc.T("export_button");
        ImportTitle.Text = _loc.T("import_title");
        ImportDescription.Text = _loc.T("import_desc");
        DuplicateModeLabel.Text = _loc.T("duplicate_mode");
        ImportButton.Content = _loc.T("import_button");

        SecuritySection.Text = _loc.T("security");
        AppLockCheck.Content = _loc.T("app_lock");
        AppLockDescription.Text = _loc.T("app_lock_desc");
        SetPinButton.Content = _loc.T("set_pin");
        LockAfterLabel.Text = _loc.T("lock_after");
        BackgroundSection.Text = _loc.T("background");
        BackgroundRefreshCheck.Content = _loc.T("background_refresh");
        BackgroundDescription.Text = _loc.T("background_desc");
        StartWindowsCheck.Content = _loc.T("start_windows");
        MinimizeTrayCheck.Content = _loc.T("minimize_tray");
        RefreshIntervalLabel.Text = _loc.T("refresh_interval");
        LanguageSection.Text = _loc.T("language");
        AppLanguageLabel.Text = _loc.T("app_language");
        LanguageDescription.Text = _loc.T("language_desc");
        PrivacySection.Text = _loc.T("privacy");
        PrivacyDescription.Text = _loc.T("privacy_desc");
        HelpSteps.Text = _loc.T("help_steps");
        UnlockTitle.Text = _loc.T("unlock_title");
        UnlockDescription.Text = _loc.T("unlock_desc");
        UnlockButton.Content = _loc.T("unlock");

        PopulateChoiceControls();
        SetPage(_activePage);
        BuildTrayMenu();
    }

    private void PopulateChoiceControls()
    {
        _loadingControls = true;
        var duplicate = DuplicateModeCombo.SelectedValue is DuplicateMode mode ? mode : DuplicateMode.Skip;
        DuplicateModeCombo.ItemsSource = new[]
        {
            new Choice<DuplicateMode>(DuplicateMode.Skip, _loc.T("duplicate_skip")),
            new Choice<DuplicateMode>(DuplicateMode.Replace, _loc.T("duplicate_replace")),
            new Choice<DuplicateMode>(DuplicateMode.KeepBoth, _loc.T("duplicate_keep"))
        };
        DuplicateModeCombo.DisplayMemberPath = "Label";
        DuplicateModeCombo.SelectedValuePath = "Value";
        DuplicateModeCombo.SelectedValue = duplicate;

        LockAfterCombo.ItemsSource = new[] { 0, 1, 5, 15, 30, 60 }
            .Select(value => new Choice<int>(value, value == 0 ? _loc.T("immediately") : _loc.T("minutes", value))).ToList();
        LockAfterCombo.DisplayMemberPath = "Label";
        LockAfterCombo.SelectedValuePath = "Value";
        LockAfterCombo.SelectedValue = _storage.Settings.LockAfterMinutes;

        RefreshIntervalCombo.ItemsSource = new[] { 15, 30, 60, 180, 360, 720, 1440 }
            .Select(value => new Choice<int>(value, value < 60 ? _loc.T("minutes", value) :
                value == 60 ? "1 h" : $"{value / 60} h")).ToList();
        RefreshIntervalCombo.DisplayMemberPath = "Label";
        RefreshIntervalCombo.SelectedValuePath = "Value";
        RefreshIntervalCombo.SelectedValue = _storage.Settings.RefreshIntervalMinutes;

        LanguageCombo.ItemsSource = LocalizationService.SupportedTags.Select((tag, index) =>
            new Choice<string>(tag, LocalizationService.LanguageNames[index])).ToList();
        LanguageCombo.DisplayMemberPath = "Label";
        LanguageCombo.SelectedValuePath = "Value";
        LanguageCombo.SelectedValue = _storage.Settings.Language;
        _loadingControls = false;
    }

    private void LoadSettingsControls()
    {
        _loadingControls = true;
        AppLockCheck.IsChecked = _storage.Settings.AppLockEnabled;
        BackgroundRefreshCheck.IsChecked = _storage.Settings.BackgroundRefreshEnabled;
        StartWindowsCheck.IsChecked = _storage.Settings.StartWithWindows;
        MinimizeTrayCheck.IsChecked = _storage.Settings.MinimizeToTray;
        LockAfterCombo.SelectedValue = _storage.Settings.LockAfterMinutes;
        RefreshIntervalCombo.SelectedValue = _storage.Settings.RefreshIntervalMinutes;
        LanguageCombo.SelectedValue = _storage.Settings.Language;
        _loadingControls = false;
    }

    private void SetPage(string page)
    {
        _activePage = page;
        DashboardPage.Visibility = page == "dashboard" ? Visibility.Visible : Visibility.Collapsed;
        AccountsPage.Visibility = page == "accounts" ? Visibility.Visible : Visibility.Collapsed;
        TransferPage.Visibility = page == "transfer" ? Visibility.Visible : Visibility.Collapsed;
        SettingsPage.Visibility = page == "settings" ? Visibility.Visible : Visibility.Collapsed;
        HelpPage.Visibility = page == "help" ? Visibility.Visible : Visibility.Collapsed;
        (PageTitle.Text, PageDescription.Text) = page switch
        {
            "accounts" => (_loc.T("accounts_title"), _loc.T("accounts_desc")),
            "transfer" => (_loc.T("transfer_title"), _loc.T("transfer_desc")),
            "settings" => (_loc.T("settings_title"), _loc.T("settings_desc")),
            "help" => (_loc.T("help_title"), _loc.T("help_desc")),
            _ => (_loc.T("dashboard_title"), _loc.T("dashboard_desc"))
        };
        AddButton.Visibility = page is "dashboard" or "accounts" ? Visibility.Visible : Visibility.Collapsed;
        RefreshButton.Visibility = page is "dashboard" or "accounts" ? Visibility.Visible : Visibility.Collapsed;
    }

    private void RebuildAccountViews()
    {
        DashboardCards.Children.Clear();
        AccountsList.Children.Clear();
        ExportAccountList.Children.Clear();

        if (_storage.Accounts.Count == 0)
        {
            DashboardCards.Children.Add(CreateEmptyCard());
            AccountsList.Children.Add(CreateEmptyCard());
        }
        else
        {
            foreach (var account in _storage.Accounts)
            {
                DashboardCards.Children.Add(CreateAccountCard(account));
                AccountsList.Children.Add(CreateAccountRow(account));
                ExportAccountList.Children.Add(new CheckBox
                {
                    Content = $"{account.Name}   ·   {MaskAccountId(account.AccountId)}",
                    Tag = account, IsChecked = true, Margin = new Thickness(0, 6, 0, 6),
                    Foreground = new SolidColorBrush(Color.FromRgb(52, 64, 84))
                });
            }
        }
        UpdateSummary();
    }

    private Border CreateEmptyCard()
    {
        var panel = new StackPanel { Margin = new Thickness(8) };
        panel.Children.Add(new TextBlock { Text = _loc.T("empty_title"), FontSize = 18, FontWeight = FontWeights.Bold, Foreground = Brush("#182033") });
        panel.Children.Add(new TextBlock { Text = _loc.T("empty_desc"), TextWrapping = TextWrapping.Wrap, Foreground = Brush("#667085"), Margin = new Thickness(0, 8, 0, 18), MaxWidth = 520 });
        var button = StyledButton(_loc.T("add_account"), true);
        button.HorizontalAlignment = HorizontalAlignment.Left;
        button.Click += AddButton_Click;
        panel.Children.Add(button);
        return new Border { Style = (Style)FindResource("Card"), Child = panel, Margin = new Thickness(0, 0, 14, 14), MinWidth = 380 };
    }

    private Border CreateAccountCard(AccountRecord account)
    {
        var outer = new Border { Style = (Style)FindResource("Card"), Width = 375, Margin = new Thickness(0, 0, 14, 14) };
        var panel = new StackPanel();
        outer.Child = panel;
        var header = new Grid();
        header.ColumnDefinitions.Add(new ColumnDefinition());
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var title = new StackPanel();
        title.Children.Add(new TextBlock { Text = account.Name, FontSize = 17, FontWeight = FontWeights.Bold, Foreground = Brush("#182033"), TextTrimming = TextTrimming.CharacterEllipsis });
        title.Children.Add(new TextBlock { Text = MaskAccountId(account.AccountId), FontSize = 11, Foreground = Brush("#98A2B3"), Margin = new Thickness(0, 4, 0, 0) });
        header.Children.Add(title);
        var percent = new TextBlock { Text = $"{account.Progress:P0}", Foreground = account.Progress >= .9 ? Brush("#D92D20") : Brush("#5B5FEF"), FontWeight = FontWeights.Bold, VerticalAlignment = VerticalAlignment.Center };
        Grid.SetColumn(percent, 1); header.Children.Add(percent); panel.Children.Add(header);

        panel.Children.Add(new ProgressBar
        {
            Minimum = 0, Maximum = 1, Value = account.Progress, Height = 8, BorderThickness = new Thickness(0),
            Foreground = account.Progress >= .9 ? Brush("#F04438") : account.Progress >= .7 ? Brush("#F79009") : Brush("#5B5FEF"),
            Background = Brush("#EAECF0"), Margin = new Thickness(0, 18, 0, 16)
        });
        var metrics = new UniformGrid { Columns = 3 };
        metrics.Children.Add(Metric(_loc.T("used"), FormatNumber(account.Used)));
        metrics.Children.Add(Metric(_loc.T("remaining"), FormatNumber(account.Remaining)));
        metrics.Children.Add(Metric(_loc.T("daily_limit"), FormatNumber(account.DailyLimit)));
        panel.Children.Add(metrics);

        if (_accountErrors.TryGetValue(account.LocalId, out var error))
            panel.Children.Add(new TextBlock { Text = error, Foreground = Brush("#D92D20"), TextWrapping = TextWrapping.Wrap, FontSize = 11, Margin = new Thickness(0, 15, 0, 0) });
        else panel.Children.Add(new TextBlock
        {
            Text = account.FetchedAt.HasValue ? _loc.T("updated", account.FetchedAt.Value.ToLocalTime().ToString("g")) : _loc.T("never_updated"),
            Foreground = Brush("#98A2B3"), FontSize = 11, Margin = new Thickness(0, 15, 0, 0)
        });
        return outer;
    }

    private Border CreateAccountRow(AccountRecord account)
    {
        var border = new Border { Style = (Style)FindResource("Card"), Margin = new Thickness(0, 0, 0, 12) };
        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(2, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(3, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var info = new StackPanel();
        info.Children.Add(new TextBlock { Text = account.Name, FontSize = 16, FontWeight = FontWeights.Bold, Foreground = Brush("#182033") });
        info.Children.Add(new TextBlock { Text = account.AccountId, FontSize = 11, Foreground = Brush("#98A2B3"), Margin = new Thickness(0, 4, 0, 0) });
        grid.Children.Add(info);
        var usage = new StackPanel { Margin = new Thickness(20, 0, 20, 0), VerticalAlignment = VerticalAlignment.Center };
        usage.Children.Add(new TextBlock { Text = $"{_loc.T("used")} {FormatNumber(account.Used)} / {FormatNumber(account.DailyLimit)}", Foreground = Brush("#344054") });
        usage.Children.Add(new ProgressBar { Minimum = 0, Maximum = 1, Value = account.Progress, Height = 7, Foreground = Brush("#5B5FEF"), Background = Brush("#EAECF0"), BorderThickness = new Thickness(0), Margin = new Thickness(0, 7, 0, 0) });
        Grid.SetColumn(usage, 1); grid.Children.Add(usage);
        var actions = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        var edit = StyledButton(_loc.T("edit"), false); edit.Click += (_, _) => EditAccount(account);
        var delete = StyledButton(_loc.T("delete"), false); delete.Margin = new Thickness(8, 0, 0, 0); delete.Foreground = Brush("#D92D20"); delete.Click += (_, _) => DeleteAccount(account);
        actions.Children.Add(edit); actions.Children.Add(delete);
        Grid.SetColumn(actions, 2); grid.Children.Add(actions);
        border.Child = grid;
        return border;
    }

    private StackPanel Metric(string label, string value)
    {
        var panel = new StackPanel();
        panel.Children.Add(new TextBlock { Text = label, Foreground = Brush("#98A2B3"), FontSize = 10 });
        panel.Children.Add(new TextBlock { Text = value, Foreground = Brush("#344054"), FontWeight = FontWeights.SemiBold, Margin = new Thickness(0, 4, 0, 0) });
        return panel;
    }

    private void UpdateSummary()
    {
        UsedTotalValue.Text = FormatNumber(_storage.Accounts.Sum(a => a.Used));
        RemainingTotalValue.Text = FormatNumber(_storage.Accounts.Sum(a => a.Remaining));
        AccountCountValue.Text = _storage.Accounts.Count.ToString(CultureInfo.CurrentCulture);
    }

    private async Task RefreshAllAsync()
    {
        if (_refreshing || _storage.Accounts.Count == 0) return;
        _refreshing = true;
        _refreshCancellation?.Cancel();
        _refreshCancellation = new CancellationTokenSource();
        RefreshButton.IsEnabled = false;
        RefreshButton.Content = _loc.T("refreshing");
        StatusText.Text = _loc.T("refreshing");
        var accounts = _storage.Accounts.ToList();
        var tasks = accounts.Select(async account =>
        {
            var token = _storage.GetToken(account);
            if (string.IsNullOrWhiteSpace(token)) return (account, (long?)null, "token_unreadable");
            try
            {
                var used = await _cloudflare.FetchTodayUsageAsync(account.AccountId, token, _refreshCancellation.Token);
                return (account, (long?)used, (string?)null);
            }
            catch (CloudflareException ex) { return (account, (long?)null, ex.ResourceKey); }
            catch (OperationCanceledException) { return (account, (long?)null, (string?)null); }
        }).ToArray();
        var results = await Task.WhenAll(tasks);
        foreach (var result in results)
        {
            if (result.Item2.HasValue)
            {
                _storage.SaveUsage(result.account, result.Item2.Value);
                _accountErrors.Remove(result.account.LocalId);
            }
            else if (result.Item3 != null) _accountErrors[result.account.LocalId] = _loc.T(result.Item3);
        }
        _refreshing = false;
        RefreshButton.IsEnabled = true;
        RefreshButton.Content = "↻  " + _loc.T("refresh");
        StatusText.Text = DateTime.Now.ToString("G", CultureInfo.CurrentCulture);
        ScheduleNextRefresh();
        RebuildAccountViews();
    }

    private void AddButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new AccountEditorDialog(this, _loc, null);
        if (dialog.ShowDialog() == true && dialog.Accepted)
        {
            _storage.AddAccount(dialog.AccountName, dialog.AccountId, dialog.DailyLimit, dialog.Token);
            RebuildAccountViews();
            _ = RefreshAllAsync();
        }
    }

    private void EditAccount(AccountRecord account)
    {
        var dialog = new AccountEditorDialog(this, _loc, account);
        if (dialog.ShowDialog() == true && dialog.Accepted)
        {
            _storage.UpdateAccount(account, dialog.AccountName, dialog.AccountId, dialog.DailyLimit, dialog.Token);
            _accountErrors.Remove(account.LocalId);
            RebuildAccountViews();
            _ = RefreshAllAsync();
        }
    }

    private void DeleteAccount(AccountRecord account)
    {
        if (MessageBox.Show(_loc.T("confirm_delete", account.Name), _loc.T("delete"), MessageBoxButton.YesNo, MessageBoxImage.Warning) != MessageBoxResult.Yes) return;
        _storage.DeleteAccount(account);
        _accountErrors.Remove(account.LocalId);
        RebuildAccountViews();
    }

    private void ExportButton_Click(object sender, RoutedEventArgs e)
    {
        var selected = ExportAccountList.Children.OfType<CheckBox>()
            .Where(check => check.IsChecked == true).Select(check => (AccountRecord)check.Tag).ToList();
        if (selected.Count == 0) { ShowError(_loc.T("backup_select_account")); return; }
        if (ExportPassword.Password.Length < 8) { ShowError(_loc.T("backup_password_short")); return; }
        if (ExportPassword.Password != ExportPasswordConfirm.Password) { ShowError(_loc.T("password_mismatch")); return; }
        var dialog = new SaveFileDialog
        {
            Filter = BackupService.SaveFileDialogFilter,
            FileName = $"CF-Quota-Backup-{DateTime.Now:yyyy-MM-dd}.cfqm",
            AddExtension = true, DefaultExt = ".cfqm"
        };
        if (dialog.ShowDialog(this) != true) return;
        try
        {
            _backup.Export(dialog.FileName, selected, _storage, ExportPassword.Password);
            ExportPassword.Clear(); ExportPasswordConfirm.Clear();
            MessageBox.Show(_loc.T("export_success"), _loc.T("app_name"), MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (BackupException ex) { ShowError(_loc.T(ex.ResourceKey)); }
        catch { ShowError(_loc.T("backup_invalid")); }
    }

    private void ImportButton_Click(object sender, RoutedEventArgs e)
    {
        var file = new OpenFileDialog { Filter = BackupService.ImportFileDialogFilter, CheckFileExists = true, Multiselect = false };
        if (file.ShowDialog(this) != true) return;
        var password = new PasswordPromptDialog(this, _loc);
        if (password.ShowDialog() != true) return;
        try
        {
            var payload = _backup.Import(file.FileName, password.Password);
            var names = string.Join(Environment.NewLine, payload.Accounts.Take(20).Select(a => "• " + a.Name));
            if (payload.Accounts.Count > 20) names += Environment.NewLine + "…";
            if (MessageBox.Show(_loc.T("import_confirm", payload.Accounts.Count, names), _loc.T("import_title"), MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
            var mode = DuplicateModeCombo.SelectedValue is DuplicateMode selected ? selected : DuplicateMode.Skip;
            var result = _storage.ImportAccounts(payload.Accounts, mode);
            RebuildAccountViews();
            MessageBox.Show(_loc.T("import_success", result.Imported, result.Skipped), _loc.T("app_name"), MessageBoxButton.OK, MessageBoxImage.Information);
            _ = RefreshAllAsync();
        }
        catch (BackupException ex) { ShowError(_loc.T(ex.ResourceKey)); }
        catch { ShowError(_loc.T("backup_invalid")); }
    }

    private void AppLockCheck_Click(object sender, RoutedEventArgs e)
    {
        if (_loadingControls) return;
        var enabled = AppLockCheck.IsChecked == true;
        if (enabled && string.IsNullOrEmpty(_storage.Settings.PinHash))
        {
            var dialog = new PinSetupDialog(this, _loc);
            if (dialog.ShowDialog() != true)
            {
                _loadingControls = true; AppLockCheck.IsChecked = false; _loadingControls = false; return;
            }
            AuthenticationService.SetPin(_storage.Settings, dialog.Pin);
        }
        _storage.Settings.AppLockEnabled = enabled;
        if (!enabled) { _storage.Settings.PinHash = ""; _storage.Settings.PinSalt = ""; }
        _storage.SaveSettings();
    }

    private void SetPinButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new PinSetupDialog(this, _loc);
        if (dialog.ShowDialog() != true) return;
        AuthenticationService.SetPin(_storage.Settings, dialog.Pin);
        _storage.Settings.AppLockEnabled = true;
        _storage.SaveSettings();
        LoadSettingsControls();
    }

    private void BackgroundRefreshCheck_Click(object sender, RoutedEventArgs e)
    {
        if (_loadingControls) return;
        _storage.Settings.BackgroundRefreshEnabled = BackgroundRefreshCheck.IsChecked == true;
        _storage.SaveSettings(); ScheduleNextRefresh();
    }

    private void StartWindowsCheck_Click(object sender, RoutedEventArgs e)
    {
        if (_loadingControls) return;
        _storage.Settings.StartWithWindows = StartWindowsCheck.IsChecked == true;
        _storage.SaveSettings();
    }

    private void MinimizeTrayCheck_Click(object sender, RoutedEventArgs e)
    {
        if (_loadingControls) return;
        _storage.Settings.MinimizeToTray = MinimizeTrayCheck.IsChecked == true;
        _storage.SaveSettings();
    }

    private void LockAfterCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_loadingControls || LockAfterCombo.SelectedValue is not int value) return;
        _storage.Settings.LockAfterMinutes = value; _storage.SaveSettings();
    }

    private void RefreshIntervalCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_loadingControls || RefreshIntervalCombo.SelectedValue is not int value) return;
        _storage.Settings.RefreshIntervalMinutes = value; _storage.SaveSettings(); ScheduleNextRefresh();
    }

    private void LanguageCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_loadingControls || LanguageCombo.SelectedValue is not string value) return;
        _storage.Settings.Language = value;
        _storage.SaveSettings();
        _loc.SetLanguage(value);
        LocalizeInterface();
        LoadSettingsControls();
        RebuildAccountViews();
    }

    private void SelectAllAccounts_Changed(object sender, RoutedEventArgs e)
    {
        var selected = SelectAllAccounts.IsChecked == true;
        foreach (var check in ExportAccountList.Children.OfType<CheckBox>()) check.IsChecked = selected;
    }

    private void DashboardNav_Click(object sender, RoutedEventArgs e) => SetPage("dashboard");
    private void AccountsNav_Click(object sender, RoutedEventArgs e) => SetPage("accounts");
    private void TransferNav_Click(object sender, RoutedEventArgs e) => SetPage("transfer");
    private void SettingsNav_Click(object sender, RoutedEventArgs e) => SetPage("settings");
    private void HelpNav_Click(object sender, RoutedEventArgs e) => SetPage("help");
    private async void RefreshButton_Click(object sender, RoutedEventArgs e) => await RefreshAllAsync();

    private void LockApplication()
    {
        if (!_storage.Settings.AppLockEnabled) return;
        _unlocked = false;
        UnlockPin.Clear(); UnlockError.Text = "";
        LockOverlay.Visibility = Visibility.Visible;
        Dispatcher.BeginInvoke(() => UnlockPin.Focus(), DispatcherPriority.Input);
    }

    private async void UnlockButton_Click(object sender, RoutedEventArgs e)
    {
        UnlockError.Text = "";
        if (!string.IsNullOrEmpty(UnlockPin.Password))
        {
            if (AuthenticationService.VerifyPin(_storage.Settings, UnlockPin.Password)) { UnlockApplication(); return; }
            UnlockError.Text = _loc.T("wrong_pin"); UnlockPin.Clear(); return;
        }
        UnlockButton.IsEnabled = false;
        var result = await _authentication.TryWindowsHelloAsync(_loc.T("hello_prompt"));
        UnlockButton.IsEnabled = true;
        if (result == true) UnlockApplication();
        else { UnlockError.Text = _loc.T("pin_prompt"); UnlockPin.Focus(); }
    }

    private void UnlockApplication()
    {
        _unlocked = true; UnlockPin.Clear(); UnlockError.Text = "";
        LockOverlay.Visibility = Visibility.Collapsed;
    }

    private void UnlockPin_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter) UnlockButton_Click(sender, e);
    }

    private void Window_Deactivated(object? sender, EventArgs e) => _deactivatedAt = DateTimeOffset.UtcNow;

    private void Window_Activated(object? sender, EventArgs e)
    {
        if (!_storage.Settings.AppLockEnabled || !_unlocked) return;
        var away = DateTimeOffset.UtcNow - _deactivatedAt;
        if (_storage.Settings.LockAfterMinutes == 0 || away >= TimeSpan.FromMinutes(_storage.Settings.LockAfterMinutes)) LockApplication();
    }

    private async void BackgroundTimer_Tick(object? sender, EventArgs e)
    {
        if (_storage.Settings.BackgroundRefreshEnabled && DateTimeOffset.UtcNow >= _nextBackgroundRefresh)
            await RefreshAllAsync();
    }

    private void ScheduleNextRefresh() => _nextBackgroundRefresh = _storage.Settings.BackgroundRefreshEnabled
        ? DateTimeOffset.UtcNow.AddMinutes(_storage.Settings.RefreshIntervalMinutes) : DateTimeOffset.MaxValue;

    private void InitializeTray()
    {
        _tray = new Forms.NotifyIcon
        {
            Text = "CF Quota Monitor", Icon = System.Drawing.SystemIcons.Information, Visible = true
        };
        _tray.DoubleClick += (_, _) => ShowFromTray();
        BuildTrayMenu();
    }

    private void BuildTrayMenu()
    {
        if (_tray == null) return;
        var menu = new Forms.ContextMenuStrip();
        menu.Items.Add(_loc.T("show"), null, (_, _) => ShowFromTray());
        menu.Items.Add(_loc.T("refresh"), null, async (_, _) => await Dispatcher.InvokeAsync(RefreshAllAsync));
        menu.Items.Add(new Forms.ToolStripSeparator());
        menu.Items.Add(_loc.T("exit"), null, (_, _) => Dispatcher.Invoke(ExitApplication));
        _tray.ContextMenuStrip?.Dispose();
        _tray.ContextMenuStrip = menu;
    }

    private void ShowFromTray()
    {
        Dispatcher.Invoke(() =>
        {
            Show(); WindowState = WindowState.Normal; Activate();
            if (_storage.Settings.AppLockEnabled) LockApplication();
            _ = RefreshAllAsync();
        });
    }

    private void ExitApplication()
    {
        _exiting = true;
        _refreshCancellation?.Cancel();
        _tray?.Dispose();
        System.Windows.Application.Current.Shutdown();
    }

    private void Window_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
    {
        if (_exiting) return;
        if (_storage.Settings.MinimizeToTray || _storage.Settings.BackgroundRefreshEnabled)
        {
            e.Cancel = true; Hide();
            if (!_trayNoticeShown && _tray != null)
            {
                _tray.ShowBalloonTip(2500, _loc.T("app_name"), _loc.T("app_running_tray"), Forms.ToolTipIcon.Info);
                _trayNoticeShown = true;
            }
            return;
        }
        _exiting = true;
        _tray?.Dispose();
        Dispatcher.BeginInvoke(() => System.Windows.Application.Current.Shutdown());
    }

    private void ShowError(string message) => MessageBox.Show(message, _loc.T("app_name"), MessageBoxButton.OK, MessageBoxImage.Warning);
    private static string MaskAccountId(string id) => id.Length < 12 ? id : $"{id[..6]}…{id[^6..]}";
    private static string FormatNumber(long value) => value.ToString("N0", CultureInfo.CurrentCulture);
    private static SolidColorBrush Brush(string value) => new((Color)ColorConverter.ConvertFromString(value));
    private Button StyledButton(string content, bool primary)
    {
        var button = new Button { Content = content };
        button.SetResourceReference(StyleProperty, primary ? "PrimaryButton" : "SecondaryButton");
        return button;
    }
}
