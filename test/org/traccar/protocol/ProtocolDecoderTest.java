package org.traccar.protocol;

import org.traccar.Context;
import org.traccar.helper.TestIdentityManager;

public class ProtocolDecoderTest {

    static {
        try {
            Context.init(new TestIdentityManager());
        } catch(Exception error) {
        }
    }

}
