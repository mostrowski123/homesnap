package moe.matthew.homesnap;

import static moe.matthew.homesnap.HomeGate.ACTIVITY_TYPE_HOME;
import static moe.matthew.homesnap.HomeGate.ACTIVITY_TYPE_RECENTS;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HomeGateTest {
    private static final int ACTIVITY_TYPE_STANDARD = 1;
    private static final int ACTIVITY_TYPE_UNDEFINED = 0;

    private final HomeGate gate = new HomeGate();

    private HomeGate.Action home(long now) {
        return gate.onMovedToFront(ACTIVITY_TYPE_HOME, now);
    }

    private HomeGate.Action recents(long now) {
        return gate.onMovedToFront(ACTIVITY_TYPE_RECENTS, now);
    }

    private HomeGate.Action app(long now) {
        return gate.onMovedToFront(ACTIVITY_TYPE_STANDARD, now);
    }

    @Test
    public void homeWithNoGestureBeforeItDoesNothing() {
        assertEquals(HomeGate.Action.NONE, home(0));
    }

    @Test
    public void homeDirectlyAfterRecentsCancelsOnce() {
        assertEquals(HomeGate.Action.NONE, recents(100));
        assertEquals(HomeGate.Action.CANCEL_ANIMATION, home(200));
        assertEquals(HomeGate.Action.NONE, home(210));
    }

    @Test
    public void backGestureToHomeDoesNothing() {
        assertEquals(HomeGate.Action.NONE, app(300));
        assertEquals(HomeGate.Action.NONE, home(400));
    }

    @Test
    public void homeAfterQuickSwitchDoesNothing() {
        recents(500);
        assertEquals(HomeGate.Action.NONE, app(600));
        assertEquals(HomeGate.Action.NONE, home(700));
    }

    @Test
    public void undefinedTypeBreaksTheSequence() {
        recents(800);
        assertEquals(HomeGate.Action.NONE, gate.onMovedToFront(ACTIVITY_TYPE_UNDEFINED, 900));
        assertEquals(HomeGate.Action.NONE, home(950));
    }

    @Test
    public void recentsJumpingUpAfterCancelRestoresHomeWithoutLooping() {
        recents(1000);
        assertEquals(HomeGate.Action.CANCEL_ANIMATION, home(1100));
        gate.onCanceled(1200);
        assertEquals(HomeGate.Action.RESTORE_HOME, recents(1210));
        assertEquals(HomeGate.Action.NONE, home(1215));
    }

    @Test
    public void recentsAfterTheWindowIsANewGesture() {
        recents(2000);
        home(2100);
        gate.onCanceled(2200);
        assertEquals(HomeGate.Action.NONE, recents(2301));
        assertEquals(HomeGate.Action.CANCEL_ANIMATION, home(2400));
    }

    @Test
    public void cancelWithNoRecentsReplyDoesNotLinger() {
        gate.onCanceled(3000);
        assertEquals(HomeGate.Action.NONE, app(3010));
        assertEquals(HomeGate.Action.NONE, recents(3020));
    }
}
