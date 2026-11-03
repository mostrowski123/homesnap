package moe.matthew.homesnap;

import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;

import rikka.shizuku.Shizuku;

/** Keeps the helper running whenever it is switched on and Shizuku is available. */
public final class App extends Application {
    interface StateListener {
        void onStateChanged();
    }

    private static final String KEY_ENABLED = "enabled";
    // Often while waiting, so the helper returns soon after Shizuku does following a reboot.
    private static final long CHECK_WHILE_WAITING_MS = 60_000;
    private static final long CHECK_WHILE_RUNNING_MS = 15 * 60_000;

    private SharedPreferences prefs;
    private Shizuku.UserServiceArgs helperArgs;
    private boolean bindRequested;
    private boolean helperConnected;
    private StateListener stateListener;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            helperConnected = binder != null && binder.pingBinder();
            onStateChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            helperConnected = false;
            bindRequested = false;
            sync();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("homesnap", MODE_PRIVATE);
        helperArgs = new Shizuku.UserServiceArgs(new ComponentName(this, HelperService.class))
                .daemon(true) // Keeps running when ColorOS kills this app.
                .processNameSuffix("helper")
                .version(BuildConfig.VERSION_CODE);

        Shizuku.addBinderReceivedListenerSticky(this::sync);
        Shizuku.addBinderDeadListener(() -> {
            helperConnected = false;
            bindRequested = false;
            onStateChanged();
        });
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> sync());
    }

    boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled && isShizukuRunning() && !hasPermission()) {
            Shizuku.requestPermission(0);
        }
        sync();
    }

    boolean isShizukuRunning() {
        return Shizuku.pingBinder();
    }

    boolean hasPermission() {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    boolean isHelperConnected() {
        return helperConnected;
    }

    void setStateListener(StateListener listener) {
        stateListener = listener;
    }

    void sync() {
        if (isShizukuRunning() && hasPermission()) {
            if (!isEnabled()) {
                Shizuku.unbindUserService(helperArgs, connection, true);
                helperConnected = false;
                bindRequested = false;
            } else if (!bindRequested) {
                bindRequested = true;
                Shizuku.bindUserService(helperArgs, connection);
            }
        }
        onStateChanged();
    }

    private void onStateChanged() {
        if (isEnabled()) {
            HelperCheckJob.schedule(this, helperConnected ? CHECK_WHILE_RUNNING_MS : CHECK_WHILE_WAITING_MS);
        } else {
            HelperCheckJob.cancel(this);
        }
        if (stateListener != null) {
            stateListener.onStateChanged();
        }
    }
}
