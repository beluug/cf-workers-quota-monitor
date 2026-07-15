using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using CFQuotaMonitor.Windows.Services;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;

namespace CFQuotaMonitor.Windows;

internal static class DialogUi
{
    public static TextBlock Label(string text) => new()
    {
        Text = text, Foreground = new SolidColorBrush(Color.FromRgb(52, 64, 84)),
        Margin = new Thickness(0, 10, 0, 6)
    };

    public static Button Button(string text, bool primary, RoutedEventHandler handler)
    {
        var button = new Button { Content = text, MinWidth = 105, Margin = new Thickness(6, 0, 0, 0) };
        button.SetResourceReference(FrameworkElement.StyleProperty, primary ? "PrimaryButton" : "SecondaryButton");
        button.Click += handler;
        return button;
    }

    public static Window Window(string title, LocalizationService loc, double width = 500, double height = 590) => new()
    {
        Title = title, Width = width, Height = height, MinWidth = width,
        WindowStartupLocation = WindowStartupLocation.CenterOwner, ResizeMode = ResizeMode.NoResize,
        FlowDirection = loc.FlowDirection, Background = new SolidColorBrush(Color.FromRgb(244, 246, 251))
    };
}

public sealed class AccountEditorDialog
{
    private readonly Window _window;
    private readonly LocalizationService _loc;
    private readonly TextBox _name = new();
    private readonly TextBox _accountId = new();
    private readonly PasswordBox _token = new();
    private readonly TextBox _limit = new();
    private readonly TextBlock _error = new() { Foreground = Brushes.Firebrick, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 10, 0, 0) };
    private readonly bool _editing;

    public string AccountName => _name.Text.Trim();
    public string AccountId => _accountId.Text.Trim();
    public string Token => _token.Password.Trim();
    public long DailyLimit => long.Parse(_limit.Text.Trim());
    public bool Accepted { get; private set; }

    public AccountEditorDialog(Window owner, LocalizationService loc, AccountRecord? account)
    {
        _loc = loc;
        _editing = account != null;
        _window = DialogUi.Window(account == null ? loc.T("add_account") : loc.T("edit"), loc);
        _window.Owner = owner;
        var root = new Border
        {
            Background = Brushes.White, CornerRadius = new CornerRadius(14), Margin = new Thickness(18),
            Padding = new Thickness(24)
        };
        var panel = new StackPanel();
        root.Child = panel;
        panel.Children.Add(new TextBlock
        {
            Text = account == null ? loc.T("add_account") : loc.T("edit"),
            FontSize = 22, FontWeight = FontWeights.Bold, Foreground = new SolidColorBrush(Color.FromRgb(24, 32, 51))
        });
        panel.Children.Add(DialogUi.Label(loc.T("account_name")));
        panel.Children.Add(_name);
        panel.Children.Add(DialogUi.Label(loc.T("account_id")));
        panel.Children.Add(_accountId);
        panel.Children.Add(DialogUi.Label(account == null ? loc.T("api_token") : loc.T("new_token")));
        panel.Children.Add(_token);
        panel.Children.Add(DialogUi.Label(loc.T("daily_limit")));
        panel.Children.Add(_limit);
        panel.Children.Add(_error);
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 20, 0, 0) };
        buttons.Children.Add(DialogUi.Button(loc.T("cancel"), false, (_, _) => _window.Close()));
        buttons.Children.Add(DialogUi.Button(loc.T("save"), true, Save));
        panel.Children.Add(buttons);
        _window.Content = root;

        if (account != null)
        {
            _name.Text = account.Name;
            _accountId.Text = account.AccountId;
            _limit.Text = account.DailyLimit.ToString();
        }
        else _limit.Text = "100000";
    }

    public bool? ShowDialog() => _window.ShowDialog();

    private void Save(object sender, RoutedEventArgs e)
    {
        if (!Regex.IsMatch(_accountId.Text.Trim(), "^[a-fA-F0-9]{32}$"))
        { _error.Text = _loc.T("validation_account"); return; }
        if (!long.TryParse(_limit.Text.Trim(), out var limit) || limit <= 0)
        { _error.Text = _loc.T("validation_limit"); return; }
        if (!_editing && _token.Password.Trim().Length < 20)
        { _error.Text = _loc.T("validation_token"); return; }
        if (_editing && _token.Password.Length > 0 && _token.Password.Trim().Length < 20)
        { _error.Text = _loc.T("validation_token"); return; }
        Accepted = true;
        _window.DialogResult = true;
    }
}

public sealed class PasswordPromptDialog
{
    private readonly Window _window;
    private readonly PasswordBox _password = new();
    public string Password => _password.Password;

    public PasswordPromptDialog(Window owner, LocalizationService loc)
    {
        _window = DialogUi.Window(loc.T("backup_password"), loc, 450, 250);
        _window.Owner = owner;
        var panel = new StackPanel { Margin = new Thickness(26) };
        panel.Children.Add(new TextBlock { Text = loc.T("backup_password"), FontSize = 18, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 0, 0, 14) });
        panel.Children.Add(_password);
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 22, 0, 0) };
        buttons.Children.Add(DialogUi.Button(loc.T("cancel"), false, (_, _) => _window.Close()));
        buttons.Children.Add(DialogUi.Button(loc.T("ok"), true, (_, _) => _window.DialogResult = true));
        panel.Children.Add(buttons);
        _window.Content = new Border { Background = Brushes.White, CornerRadius = new CornerRadius(14), Margin = new Thickness(14), Child = panel };
        _window.Loaded += (_, _) => _password.Focus();
    }
    public bool? ShowDialog() => _window.ShowDialog();
}

public sealed class PinSetupDialog
{
    private readonly Window _window;
    private readonly LocalizationService _loc;
    private readonly PasswordBox _pin = new() { MaxLength = 32 };
    private readonly PasswordBox _confirm = new() { MaxLength = 32 };
    private readonly TextBlock _error = new() { Foreground = Brushes.Firebrick, Margin = new Thickness(0, 10, 0, 0) };
    public string Pin => _pin.Password;

    public PinSetupDialog(Window owner, LocalizationService loc)
    {
        _loc = loc;
        _window = DialogUi.Window(loc.T("set_pin_title"), loc, 460, 360);
        _window.Owner = owner;
        var panel = new StackPanel { Margin = new Thickness(26) };
        panel.Children.Add(new TextBlock { Text = loc.T("set_pin_title"), FontSize = 20, FontWeight = FontWeights.Bold });
        panel.Children.Add(DialogUi.Label(loc.T("pin_prompt")));
        panel.Children.Add(_pin);
        panel.Children.Add(DialogUi.Label(loc.T("pin_confirm")));
        panel.Children.Add(_confirm);
        panel.Children.Add(_error);
        var buttons = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 20, 0, 0) };
        buttons.Children.Add(DialogUi.Button(loc.T("cancel"), false, (_, _) => _window.Close()));
        buttons.Children.Add(DialogUi.Button(loc.T("ok"), true, Save));
        panel.Children.Add(buttons);
        _window.Content = new Border { Background = Brushes.White, CornerRadius = new CornerRadius(14), Margin = new Thickness(14), Child = panel };
    }
    public bool? ShowDialog() => _window.ShowDialog();

    private void Save(object sender, RoutedEventArgs e)
    {
        if (!Regex.IsMatch(_pin.Password, "^[0-9]{6,32}$")) { _error.Text = _loc.T("pin_prompt"); return; }
        if (_pin.Password != _confirm.Password) { _error.Text = _loc.T("password_mismatch"); return; }
        _window.DialogResult = true;
    }
}
