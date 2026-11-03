package moe.matthew.homesnap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // The check saved before the reboot can be 15 minutes away, so start the short wait now.
        ((App) context.getApplicationContext()).sync();
    }
}
