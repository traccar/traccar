package org.traccar.protocol;

import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WialonProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        WialonProtocolDecoder decoder = new WialonProtocolDecoder(new WialonProtocol());

        assertNull(decoder.decode(null, null, "#L#123456789012345;test"));
        
        assertNull(decoder.decode(null, null, "#L#2002;NA"));
        
        assertNull(decoder.decode(null, null, "#P#"));

        verify(decoder.decode(null, null,
                "#SD#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4"));
        
        verify(decoder.decode(null, null,
                "#SD#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5"));

        verify(decoder.decode(null, null,
                "#D#270413;205601;5544.6025;N;03739.6834;E;1;2;3;4;0.0;0;0;14.77,0.02,3.6;NA;count1:1:564,fuel:2:45.8,hw:3:V4.5"));
        
        verify(decoder.decode(null, null,
                "#D#190114;051312;4459.6956;N;04105.9930;E;35;306;204.000000;12;NA;452986639;NA;106.000000;NA;sats_gps:1:9,sats_glonass:1:3,balance:2:12123.000000,stay_balance:1:0"));
        
        verify(decoder.decode(null, null,
                "#D#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5;1.74;NA;NA;NA;NA;NA"));

        verify(decoder.decode(null, null,
                "#D#021214;065947;2237.7552;N;11404.8851;E;0.000;;170.9;5;1.74;NA;NA;;NA;NA"));

        verify(decoder.decode(null, null,
                "#B#080914;073235;5027.50625;N;03026.19321;E;0.700;0.000;NA;4;NA;NA;NA;;NA;Батарея:3:100 %|080914;073420;5027.50845;N;03026.18854;E;1.996;292.540;NA;4;NA;NA;NA;;NA;Батарея:3:100 %"));
        
        verify(decoder.decode(null, null,
                "#B#110914;102132;5027.50728;N;03026.20369;E;1.979;288.170;NA;NA;NA;NA;NA;;NA;Батарея:3:100 %"));

        verify(decoder.decode(null, null,
                "#B#110315;045857;5364.0167;N;06127.8262;E;0;155;965;7;2.40;4;0;;NA;Uacc:2:3.4,Iacc:2:0.000,Uext:2:13.2,Tcpu:2:14.4,Balance:2:167.65,GPS:3:Off"));

        verify(decoder.decode(null, null,
                "#B#110315;045857;5364.0167;N;06127.8262;E;0;155;965;7;2.40;4;0;14.77,0.02,3.6;AB45DF01145;"));

    }

}
