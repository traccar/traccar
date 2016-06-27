package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AppelloProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        AppelloProtocolDecoder decoder = new AppelloProtocolDecoder(new AppelloProtocol());

        verifyAttributes(decoder, text(
                "FOLLOWIT,860719028336968,UTCTIME,-12.112660,-77.045189,0,0,3,-0,L,716,10,049C,2A47,23,,4.22,,53,999/00/00,,,,,,59826,"));
        
        verifyPosition(decoder, text(
                "FOLLOWIT,860719028336968,160211221959,-12.112660,-77.045258,1,0,6,116,F,716,17,4E85,050C,29,,4.22,,39,999/00/00,,,,,,46206,"));

        verifyPosition(decoder, text(
                "FOLLOWIT,359586019278139,130809160321,22.340218,114.030737,60,120,05,152,F,460,01,2533,720B,31,out,3.90,1,192,20/00/00,12.5,100%,80,45,1CFA68BB754E:60|2CFA68BB754E:100|3CFA68BB754E:100|4CFA68BB754E:100|5CFA68BB754E:100|,46672"));

    }

}
