/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class StrongTowerProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodePositionPeriodic() throws Exception {

        StrongTowerProtocolEncoder encoder = new StrongTowerProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 300);

        assertEquals("{\"id\":1,\"cmd\":\"" + Command.TYPE_POSITION_PERIODIC + "\",\"" + Command.KEY_FREQUENCY + "\":300}\r\n", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        StrongTowerProtocolEncoder encoder = new StrongTowerProtocolEncoder();

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "hello");

        assertEquals("{\"data\":\"hello\",\"id\":1,\"cmd\":\"" + Command.TYPE_CUSTOM + "\"}\r\n", encoder.encodeCommand(command));

    }

}
