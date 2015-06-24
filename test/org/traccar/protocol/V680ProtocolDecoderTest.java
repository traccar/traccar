package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class V680ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        V680ProtocolDecoder decoder = new V680ProtocolDecoder(new V680Protocol());
        
        assertNull(decoder.decode(null, null,
                "#353588102019155"));
        
        verify(decoder.decode(null, null,
                "#356823031193431##0#0000#SF#1#72403#V#04702.3025,W,2252.18380,S,008.18,0#090413#134938"));

        verify(decoder.decode(null, null,
                "#356823033219838#1000#0#1478#AUT#1#66830FFB#03855.6628,E,4716.6821,N,001.41,259#130812#143905"));

        verify(decoder.decode(null, null,
                "#353588102019155##1#0000#AUT#01#7240060be7873f#4849.079800,W,2614.458200,S,0.00,0.00#130413#182110.000"));
        
        verify(decoder.decode(null, null,
                "#353588302045917##1#0000#AUT#01#7243141c2b14c3#4738.442300,W,2334.874000,S,0.00,0.30#170413#004831.000"));
        
        verify(decoder.decode(null, null,
                "#352897045085282##0#0000#AUT#1#72400510730208,00d36307,10734fc4#4647.8922,W,2339.1956,S,2.60,63.74#200413#094310.000"));
        
        verify(decoder.decode(null, null,
                "#356823033537791##0#0000#AUT#1#V#03610.2179,E,5004.5796,N,000.01,349#180513#073758"));
        
        verify(decoder.decode(null, null,
                "#356823031236214##0#0000#AUT#1#V#01904.5491,E,6941.0085,N,000.09,248#170513#160140"));
        
        assertNull(decoder.decode(null, null,
                "#353588550032869##1#0000#AUT#01#72400401cd01a5#00000.0000,E,0000.0000,N,0.00,#000000#000000.000"));

        verify(decoder.decode(null, null,
                "#352897045085282##0#0000#AUT#1#72400510730208,00d36307,10734fc4#4647.8922,W,2339.1956,S,2.60,63.74#200413#094310.000##"));
        
        verify(decoder.decode(null, null,
                "#352165050199210##13#0000#AUT#1#72400605471305,054712fd,054712ff#05144.0008,W,3005.5011,S,0.11,201.46#260713#172647.000##"));
        
        verify(decoder.decode(null, null,
                "#356823031166908#13001190527#0#0000#AUT#4#V#07136.4070,W,1040.0575,N,000.35,257#280813#142836#V#07136.4088,W,1040.0580,N,000.49,288#280813#142846#V#07136.4098,W,1040.0590,N,000.59,264#280813#142856#V#07136.4093,W,1040.0605,N,000.30,264#280813#142906##"));
        
        verify(decoder.decode(null, null,
                "#135790246811222#13486119277#1#0000#SOS#1#27bc10af#11407.4182,E,2232.7632,N,0.00,79.50#070709#134147.000##"));
        
        verify(decoder.decode(null, null,
                "#355488020132015##1#0000#AUT#01#510089246a34c0#10641.338800,E,619.427100,S,0.00,0.00#011113#161942.000##"));
        
        verify(decoder.decode(null, null,
                "#359094025419110#bigfriend#0#1234#AUTO#1##04632.8846,W,2327.2264,S,0.00,0.00#220913#234808##"));
        
        verify(decoder.decode(null, null,
                "#353588102031599##1#0000#AUT#01#41300304843fc1#7955.124400,E,642.095500,N,5.28,95.21#041213#074431.000##"));
        
        verify(decoder.decode(null, null,
                "1#0000#AUT#01#23403007fa650e#16.747700,W,5136.356500,N,0.00,0.00#040415#002051.000"));

    }

}
