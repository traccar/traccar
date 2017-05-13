/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *                Ivan F. Martinez
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
package org.traccar.helper;

import org.junit.Test;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class PositionUtilTest {
    
    @Test
    public void testConversion() {
        Position pos = new Position();
        pos.setLatitude(1);
        pos.setLongitude(2);
        
        assertEquals("1.0,2.0", PositionUtil.processTemplate("{latitude},{longitude}", pos, null, false));

        //TODO more tests...
    }

}
