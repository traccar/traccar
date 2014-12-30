package org.traccar;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.TestDataManager;
import org.traccar.model.Position;
import org.traccar.protocol.AtrackProtocolDecoder;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class FilterHandlerTest {

    private FilterHandler filtingHandler;
    private FilterHandler passingHandler;

    @Before
    public void setUp() {
        filtingHandler = new FilterHandler(true, true, true, 10, 10);
        passingHandler = new FilterHandler(false, false, false, 0, 0);
    }

    @After
    public void tearDown() {
        filtingHandler = null;
        passingHandler = null;
    }

    @Test
    public void testFilterInvalid() throws Exception {

        Position position = new Position(0, new Date(), true, 10, 10, 10, 10, 10);

        assertNotNull(filtingHandler.decode(null, null, position));
        assertNotNull(passingHandler.decode(null, null, position));

        position = new Position(0, new Date(), false, 10, 10, 10, 10, 10);

        assertNull(filtingHandler.decode(null, null, position));
        assertNotNull(passingHandler.decode(null, null, position));
    }

}
