package moe.matthew.homesnap;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class MainActivity extends Activity implements App.StateListener {
    private App app;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();

        Switch toggle = new Switch(this);
        toggle.setText("Skip the wait on the home gesture");
        toggle.setChecked(app.isEnabled());
        toggle.setOnCheckedChangeListener((view, checked) -> app.setEnabled(checked));

        status = new TextView(this);

        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(padding, padding, padding, padding);
        column.addView(toggle);
        column.addView(status);

        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        root.addView(column);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.setStateListener(this);
        app.sync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.setStateListener(null);
    }

    @Override
    public void onStateChanged() {
        status.setText(statusText());
    }

    private String statusText() {
        if (!app.isEnabled()) {
            return "Off.";
        }
        if (!app.isShizukuRunning()) {
            return "Waiting for Shizuku. Start it in the Shizuku app.";
        }
        if (!app.hasPermission()) {
            return "Allow HomeSnap when Shizuku asks. If it doesn't ask, allow it in the Shizuku app.";
        }
        return app.isHelperConnected() ? "On." : "Starting...";
    }
}
