package org.traccar.handler.events;

import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.state.OverspeedProcessor;
import org.traccar.session.state.OverspeedState;
import org.traccar.session.state.TollRouteProcessor;
import org.traccar.session.state.TollRouteState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class TollRouteEventHandlerTest extends BaseTest {

    private Position position(String time, double speed) throws ParseException {
        Position position = new Position();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(time));
        position.setSpeed(speed);
        return position;
    }

    private void verifyState(TollRouteState tollRouteState, boolean state) {
        assertEquals(state, tollRouteState.isChanged());
    }

    private void testTollWithPosition() throws ParseException {
//        TollRouteState state = new TollRouteState();
//
//        TollRouteProcessor.updateState(state, position("2017-01-01 00:00:00", 50), true, "toll road", "toll road name full",15000);
//        assertNull(state.getEvent());
//        verifyState(state, true);
//
//        TollRouteProcessor.updateState(state, position("2017-01-01 00:00:10", 55), true, "toll road", "toll road name full",15000);
//        assertNull(state.getEvent());
//
//        TollRouteProcessor.updateState(state, position("2017-01-01 00:00:20", 55), true, "toll road", "toll road name full",15000);
//        assertNotNull(state.getEvent());
//        assertEquals(Event.TYPE_DEVICE_TOLLROUTE_ENTER, state.getEvent().getType());
////        assertTrue(state.getEvent().getBoolean("isToll"));
//        //TODO: add exit test case
//        assertEquals("toll road", state.getEvent().getString("tollRef"));
//        assertEquals("toll road name full", state.getEvent().getString("tollName"));
//        verifyState(state, true);
//
//        TollRouteProcessor.updateState(state, position("2017-01-01 00:00:30", 55), true, "toll road", "toll road name full",15000);
//        assertNull(state.getEvent());
//        verifyState(state, true);
//
//        TollRouteProcessor.updateState(state, position("2017-01-01 00:00:40", 30), false, null, null,15000);
//        assertNull(state.getEvent());
//        verifyState(state, false);
    }

    @Test
    public void testTollEventHandler() throws Exception {
        testTollWithPosition();
    }

}
