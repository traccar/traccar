package org.traccar.helper;

import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.traccar.model.Data;
import org.traccar.model.Position;

public class DecoderVerifier {
    
    private static void verifyPosition(Position position) {
        
        assertNotNull(position.getDeviceId());
        
        assertNotNull(position.getFixTime());
        assertTrue(position.getFixTime().after(new Date(946684800000L))); // 2000 year
        assertTrue(position.getFixTime().getTime() < System.currentTimeMillis() + 3600000); // 1 hour from now

        assertNotNull(position.getValid());
        
        assertNotNull(position.getLatitude());
        assertTrue(position.getLatitude() >= -90);
        assertTrue(position.getLatitude() <= 90);
        
        assertNotNull(position.getLongitude());
        assertTrue(position.getLongitude() >= -180);
        assertTrue(position.getLongitude() <= 180);
        
        assertNotNull(position.getAltitude());
        assertTrue(position.getAltitude() >= -12262);
        assertTrue(position.getAltitude() <= 18000);
        
        assertNotNull(position.getSpeed());
        assertTrue(position.getSpeed() >= 0);
        assertTrue(position.getSpeed() <= 869);
        
        assertNotNull(position.getCourse());
        assertTrue(position.getCourse() >= 0);
        assertTrue(position.getCourse() <= 360);
        
        assertNotNull(position.getAttributes());

    }
    
    private static void verifyData(Data data) {

        assertNotNull(data.getDeviceId());

        assertNotNull(data.getAttributes());

    }

    public static void verify(Object object) {
        
        assertNotNull(object);
        
        if (object instanceof Position) {
            verifyPosition((Position) object);
        } else if (object instanceof Data) {
            verifyData((Data) object);
        } else if (object instanceof List) {
            List positions = (List) object;
            
            assertFalse(positions.isEmpty());
            
            for (Object item : positions) {
                if (item instanceof Position) {
                    verifyPosition((Position) item);
                } else if (item instanceof Data) {
                    verifyData((Data) item);
                }
            }
        }
        
    }
    
}
