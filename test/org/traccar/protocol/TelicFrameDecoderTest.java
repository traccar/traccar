package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class TelicFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TelicFrameDecoder decoder = new TelicFrameDecoder();

        Assert.assertNull(
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "00303032363937393238317c3233327c30337c30303230303430313000")));

        Assert.assertEquals(
                binary(ByteOrder.LITTLE_ENDIAN, "303032363937393238317c3233327c30337c303032303034303130"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "303032363937393238317c3233327c30337c30303230303430313000")));

        Assert.assertEquals(
                binary(ByteOrder.LITTLE_ENDIAN, "3030323039373932383139392c3231303231363038313930302c302c3231303231363038313835392c3031333839333338352c34363635383639352c332c302c302c382c2c2c3534312c36313239382c2c303030302c30302c302c3139362c302c30343037"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "650000003030323039373932383139392c3231303231363038313930302c302c3231303231363038313835392c3031333839333338352c34363635383639352c332c302c302c382c2c2c3534312c36313239382c2c303030302c30302c302c3139362c302c3034303700")));

    }

}
