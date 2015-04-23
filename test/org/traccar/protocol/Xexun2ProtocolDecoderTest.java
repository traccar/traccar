package org.traccar.protocol;

import static org.junit.Assert.assertNull;
import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import org.junit.Test;

public class Xexun2ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Xexun2ProtocolDecoder decoder = new Xexun2ProtocolDecoder(null);
        
        assertNull(decoder.decode(null, null,
                ",+48606717068,,L,, imei:012207005047292,,,F:4.28V,1,52,11565,247,01,000E,1FC5"));

        verify(decoder.decode(null, null,
                "130302125349,+79604870506,GPRMC,085349.000,A,4503.2392,N,03858.5660,E,6.95,154.65,020313,,,A*6C,F,, imei:012207007744243,03,-1.5,F:4.15V,1,139,28048,250,01,278A,5072"));

        verify(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120009.590,A,4639.6774,N,01418.5737,E,0.00,0.00,111111,,,A*68,F,, imei:359853000144328,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576"));

        verify(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,help me!, imei:123456789012345,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576"));

        verify(decoder.decode(null, null,
                "111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,help me!, imei:123456789012345,04,481.2,L:3.5V,0,139,2689,232,03,2725,0576"));

        verify(decoder.decode(null, null,
                "111111120009,436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,help me!, imei:123456789012345,04,481.2,L:3.5V,0,139,2689,232,03,2725,0576"));

        verify(decoder.decode(null, null,
                "111111120009,+1234,GPRMC,204530.4,A,6000.0000,N,13000.0000,E,0.0,,010112,0.0,E,A*68,F,imei:123456789012345,04,123.5,F:3.55V,0,139,,232,03,272CE1,0576"));

        verify(decoder.decode(null, null,
                "111111120009,+1234,GPRMC,204530.4,A,6000.000,N,01000.6288,E,0.0,0.00,230713,0.0,E,A*3C,F,imei:123456789012345,00,,F:3.88V,0,125,,262,01,224CE1,379B"));

        verify(decoder.decode(null, null,
                "111111120009,+1234,GPRMC,215840.7,A,6000.000,N,01000.6253,E,0.0,0.00,230713,0.0,E,A*34,F,imei:123456789012345,00,,F:3.9V,0,124,,262,01,224CE1,379B"));

        verify(decoder.decode(null, null,
                "130725134142,,GPRMC,134142.591,A,3845.6283,N,00909.8876,W,2.08,287.33,250713,,,A*71,F,, imei:013227000526784,03,-50.7,L:3.69V,0,128,65337,268,03,177A,119F"));
        
        verify(decoder.decode(null, null,
                "140602152533,TESCO_INFO,GPRMC,152533.000,A,5145.4275,N,00000.3448,E,0.00,0.00,020614,,,A*66,F,, imei:013227002781643,06,35.1,F:4.15V,1,135,38950,234,10,10B4,5235"));

        verify(decoder.decode(null, null,
                "150216154418,5277,GNRMC,134418.000,A,5533.8973,N,03745.4398,E,0.00,308.85,160215,,,A*7A,F,, imei:864244028033115,10,169.8,F:4.28V,1,132,48269,250,99,6D0D,8572"));

        verify(decoder.decode(null, null,
                "150224173341,+66961544651,GPRMC,093341.000,A,1344.5716,N,10033.6648,E,0.00,0.00,240215,,,A*68,F,,imei:865328028306149,05,106.4,F:4.01V/ADC1=0.20V/ADC2=0.00V,0,159,955,520,01,5DE8,0399,6.21km"));

        verify(decoder.decode(null, null,
                "150316182840,07872167745,GPRMC,182840.000,A,5126.1310,N,00055.5573,W,0.00,0.00,160315,,,A*7C,F,,imei:865328023469306,06,54.3,F:4.10V/ADC1=0.76V/ADC2=0.00V,0,157,38486,234,10,34DC,48A6,3.70km"));

    }

}
