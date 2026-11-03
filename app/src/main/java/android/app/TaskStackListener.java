package android.app;

/**
 * Compile-time stand-in for the hidden framework class. It gets packaged, but class loading checks
 * the framework first, so the phone's own TaskStackListener is the one that runs.
 */
public class TaskStackListener {
    public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) throws android.os.RemoteException {
    }
}
