package org.traccar.protocol;

import org.traccar.Context;
import org.traccar.helper.TestDataManager;

public class ProtocolDecoderTest {

    static {
        try {
            Context.init(new TestDataManager());
        } catch(Exception error) {
        }
    }

}
