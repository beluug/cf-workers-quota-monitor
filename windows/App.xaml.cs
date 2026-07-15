using System.Threading;
using System.Windows;

namespace CFQuotaMonitor.Windows;

public partial class App : System.Windows.Application
{
    private Mutex? _singleInstance;

    protected override void OnStartup(StartupEventArgs e)
    {
        _singleInstance = new Mutex(true, "CFQuotaMonitor.Windows.SingleInstance", out var created);
        if (!created)
        {
            System.Windows.MessageBox.Show("CF Quota Monitor is already running.", "CF Quota Monitor");
            Shutdown();
            return;
        }

        base.OnStartup(e);
        var window = new MainWindow();
        MainWindow = window;
        window.Show();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _singleInstance?.ReleaseMutex();
        _singleInstance?.Dispose();
        base.OnExit(e);
    }
}
