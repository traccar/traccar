/*
 * Copyright 2015 alexis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;
 
import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;

/**
 *
 * @author Irving Gonzalez [ialexis93@gmail.com]
 */
public class TotemProtocolEncoder extends StringProtocolEncoder{
    
    @Override
    protected Object encodeCommand(Command command) {
        
        switch (command.getType()) {
            case Command.TYPE_IMEI:
                return "*000000,801#";
        }
        
        return null;
    }
}
