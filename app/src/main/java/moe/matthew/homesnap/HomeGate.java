package moe.matthew.homesnap;

/** Decides what to do from the order tasks come to the front. */
final class HomeGate {
    enum Action { NONE, CANCEL_ANIMATION, RESTORE_HOME }

    static final int ACTIVITY_TYPE_HOME = 2;
    static final int ACTIVITY_TYPE_RECENTS = 3;

    // ColorOS moves recents up about 10 ms after the cancel. Any later and it is the user's next gesture.
    private static final long RESTORE_WINDOW_MS = 100;

    private boolean lastWasRecents;
    private boolean canceled;
    private long canceledAtMs;

    /** Cancels only when home directly follows recents, so back gestures are left alone. */
    synchronized Action onMovedToFront(int activityType, long nowMs) {
        boolean isRecents = activityType == ACTIVITY_TYPE_RECENTS;
        boolean endOfHomeGesture = lastWasRecents && activityType == ACTIVITY_TYPE_HOME;
        boolean pendingRestore = canceled && nowMs - canceledAtMs <= RESTORE_WINDOW_MS;
        canceled = false;

        if (isRecents && pendingRestore) {
            // Not treated as a gesture start, or restoring home would trigger another cancel.
            lastWasRecents = false;
            return Action.RESTORE_HOME;
        }
        lastWasRecents = isRecents;
        return endOfHomeGesture ? Action.CANCEL_ANIMATION : Action.NONE;
    }

    synchronized void onCanceled(long nowMs) {
        canceled = true;
        canceledAtMs = nowMs;
    }
}
