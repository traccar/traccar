/*
 * Copyright 2015 Iuri Pereira (iuricmp@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;
import javax.xml.bind.DatatypeConverter;

public class GeoStudioProtocolDecoderTest extends ProtocolTest {
    
    @Test
    public void testDecode() throws Exception {
        
        GeoStudioProtocol protocol = new GeoStudioProtocol();
        GeoStudioProtocolDecoder decoder = new GeoStudioProtocolDecoder(protocol);
        
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "");
        request.setContent(ChannelBuffers
                .wrappedBuffer(ByteOrder.LITTLE_ENDIAN, DatatypeConverter.parseHexBinary("10032a1ca400f118d13a00004e6606fa86115cee4157e6550000cd0301000000")));
        verifyPosition(decoder, request);
        
    }
}
