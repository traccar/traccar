package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatorProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeId() {

        assertEquals("3512345006", GatorProtocolDecoder.decodeId(12, 162, 50, 134));

    }

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new GatorProtocolDecoder(null));

        verify(decoder, binary(
                "24248000278632210440910210250107210008006368860032001400000057c047000000185c050da7f9960d"),
                position());

        verify(decoder, binary(
                "24248000278632210440910210250107210008006368860032001400000057c047000000185c050da7f90000000001960d"),
                position().attribute(Position.KEY_ALARM, Position.ALARM_ACCELERATION));

        verify(decoder, binary(
                "242480002600341cad190917022021812497260280594200000000c047010000135400009bb600ff00b90d"),
                position(Checks.ATTRIBUTES));

        verify(decoder, binary(
                "2424800026364101b31608041108380273453415301532000000008000010000122800000124000000c40d"),
                position(Checks.ATTRIBUTES));

        verify(decoder, binary(
                "242421000658e3d851150d"));

        verify(decoder, binary(
                "242480002658e3d851a60101c662bc00000000000000000000000000470007a30b0c00b10fc900ff00460d"),
                position(Checks.ATTRIBUTES));

        verify(decoder, binary(
                "242421000643e30282070d"));

        verify(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"),
                position().location("2014-11-21T07:27:02.000Z", true, 59.37697, 10.72792));

        verify(decoder, binary(
                "24248100230CA23285100306145907022346901135294700000000C04001012C0E1100000021CB0D"),
                position());

        verify(decoder, binary(
                "2424800023c2631e00111220104909833268648703804100000000c0470000000b4e00000000550d"),
                position().location("2011-12-20T10:49:09.000Z", true, -33.44773, -70.63402));

        verify(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"),
                position());

    }

}
