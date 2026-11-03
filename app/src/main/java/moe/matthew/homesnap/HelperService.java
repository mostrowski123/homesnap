package moe.matthew.homesnap;

import android.app.ActivityManager;
import android.app.TaskInfo;
import android.app.TaskStackListener;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runs as the shell uid in a process Shizuku starts.
 *
 * ColorOS keeps a third-party launcher's task hidden until its own home animation and a hardcoded
 * 500 ms scroll in the hidden recents view have ended. This ends the wait:
 * 1. An empty TRANSIT_SLEEP makes WM Shell cancel the recents transition and show the home task.
 * 2. ColorOS then moves recents above home. Recents is made translucent first, so home is not
 *    hidden and has nothing to redraw.
 * 3. The home root task is reordered back on top directly. A new transition would wait behind the
 *    sleep for about 120 ms, and without the translucent step this flashes black.
 *
 * This is an internal API used in an unintended way, so any update can break it. If this process
 * dies within the few milliseconds the flag is set, the flag stays until a later restore clears it,
 * which only keeps a hidden task alive behind recents.
 */
public final class HelperService extends Binder {
    private static final String TAG = "HomeSnap";
    // Shizuku's "destroy" call. Its docs give 16777114 for AIDL, which adds FIRST_CALL_TRANSACTION.
    private static final int TRANSACTION_DESTROY = 16777115;
    private static final int TRANSIT_SLEEP = 12;
    private static final int WINDOWING_MODE_UNDEFINED = 0;
    // Quick switch brings home up for 12-31 ms before the target app, so this leaves a margin.
    private static final long CANCEL_DELAY_MS = 60;
    // Covers a gesture that had no recents transition to cancel, so no restore follows.
    private static final long CLEAR_TRANSLUCENT_AFTER_MS = 100;

    private final HomeGate gate = new HomeGate();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Object atm;
    private final Object organizer;
    private final Method startNewTransition;
    private final Method applyTransaction;
    private final Method getRootTaskInfo;
    private final Class<?> transaction;
    private final Method reorder;
    private final Method setForceTranslucent;
    private final Field tokenField;
    private final Field topActivityTypeField;

    // Written on binder threads, read on the handler thread.
    private volatile Object homeRootToken;
    private volatile Object recentsToken;

    private final Runnable cancelAnimation = this::cancelAnimation;
    private final Runnable clearTranslucent = this::clearTranslucent;

    public HelperService() throws ReflectiveOperationException {
        atm = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
        organizer = method(atm, "getWindowOrganizerController").invoke(atm);
        startNewTransition = method(organizer, "startNewTransition");
        applyTransaction = method(organizer, "applyTransaction");
        getRootTaskInfo = method(atm, "getRootTaskInfo");
        transaction = Class.forName("android.window.WindowContainerTransaction");
        Class<?> token = Class.forName("android.window.WindowContainerToken");
        reorder = transaction.getMethod("reorder", token, boolean.class, boolean.class);
        setForceTranslucent = transaction.getMethod("setForceTranslucent", token, boolean.class);
        tokenField = TaskInfo.class.getField("token");
        topActivityTypeField = TaskInfo.class.getField("topActivityType");

        TaskStackListener listener = new TaskStackListener() {
            @Override
            public void onTaskMovedToFront(ActivityManager.RunningTaskInfo info) {
                onMovedToFront(info);
            }
        };
        method(atm, "registerTaskStackListener").invoke(atm, listener);
        Log.i(TAG, "listening");
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == TRANSACTION_DESTROY) {
            System.exit(0);
        }
        return super.onTransact(code, data, reply, flags);
    }

    private void onMovedToFront(ActivityManager.RunningTaskInfo info) {
        HomeGate.Action action = HomeGate.Action.NONE;
        try {
            int activityType = topActivityTypeField.getInt(info);
            action = gate.onMovedToFront(activityType, SystemClock.uptimeMillis());
            Log.i(TAG, "front task=" + info.taskId + " type=" + activityType + " -> " + action);

            // Quick switch brings home up for a moment before the target app, so a later task wins.
            handler.removeCallbacks(cancelAnimation);

            if (activityType == HomeGate.ACTIVITY_TYPE_RECENTS) {
                recentsToken = tokenField.get(info);
            }
            if (action == HomeGate.Action.CANCEL_ANIMATION) {
                // The launcher's own task sits inside the home root task, and that is what must move.
                Object homeRoot = getRootTaskInfo.invoke(atm, WINDOWING_MODE_UNDEFINED, HomeGate.ACTIVITY_TYPE_HOME);
                homeRootToken = tokenField.get(homeRoot);
                handler.postDelayed(cancelAnimation, CANCEL_DELAY_MS);
            } else if (action == HomeGate.Action.RESTORE_HOME) {
                handler.removeCallbacks(clearTranslucent);
                Object wct = newTransaction();
                reorder.invoke(wct, homeRootToken, true, true);
                setForceTranslucent.invoke(wct, recentsToken, false);
                applyTransaction.invoke(organizer, wct);
            }
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, action + " failed", e);
        }
    }

    private void cancelAnimation() {
        try {
            // Recorded first: recents can come to the front before the call returns.
            gate.onCanceled(SystemClock.uptimeMillis());
            handler.postDelayed(clearTranslucent, CLEAR_TRANSLUCENT_AFTER_MS);

            Object wct = newTransaction();
            setForceTranslucent.invoke(wct, recentsToken, true);
            applyTransaction.invoke(organizer, wct);

            startNewTransition.invoke(organizer, TRANSIT_SLEEP, newTransaction());
            Log.i(TAG, "canceled");
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    private void clearTranslucent() {
        try {
            Object wct = newTransaction();
            setForceTranslucent.invoke(wct, recentsToken, false);
            applyTransaction.invoke(organizer, wct);
            Log.i(TAG, "cleared translucent");
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "clear failed", e);
        }
    }

    private Object newTransaction() throws ReflectiveOperationException {
        return transaction.getConstructor().newInstance();
    }

    private static Method method(Object target, String name) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalStateException("No method " + name + " on " + target.getClass());
    }
}
