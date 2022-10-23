package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class AppelloProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AppelloProtocolDecoder(null));

        verifyAttributes(decoder, text(
                "FOLLOWIT,867273024233699,UTCTIME,0.000000,0.000000,0,0,0,0,L,262:3:c703:4bf8:64:255|262:3:c703:9a18:45|262:3:c703:e838:33|262:3:c703:7190:20|262:3:c704:d896:17|,02,44,,31,,4.20,0,0,86/44/24,,,,26,02264DFF6E16:69|4C09D408554E:79|4C09D408554F:79|E0885DE705E5:81|E2885DE705E7:81|246511122CCC:83|,34925"));

        verifyAttributes(decoder, text(
                "FOLLOWIT,867273024233699,UTCTIME,0.000000,0.000000,0,0,0,0,L,262:3:c703:4bf8:64:1|262:3:c703:9a18:44|262:3:c703:e838:33|262:3:c703:7190:21|262:3:c704:d896:18|262:3:c703:71a6:13|262:3:c703:253d:13|,02,44,,22,,4.20,0,0,86/44/24,,,,30,02264DFF6E16:67|E0885DE705E5:87|B4A5EF284B94:88|E2885DE705E7:85|4C09D408554E:78|3481C4D71B13:43|,24033"));

        verifyAttributes(decoder, text(
                "FOLLOWIT,860719028336968,UTCTIME,-12.112660,-77.045189,0,0,3,-0,L,716,10,049C,2A47,23,,4.22,,53,999/00/00,,,,,,59826,"));

        verifyPosition(decoder, text(
                "FOLLOWIT,860719028336968,160211221959,-12.112660,-77.045258,1,0,6,116,F,716,17,4E85,050C,29,,4.22,,39,999/00/00,,,,,,46206,"));

        verifyPosition(decoder, text(
                "FOLLOWIT,359586019278139,130809160321,22.340218,114.030737,60,120,05,152,F,460,01,2533,720B,31,out,3.90,1,192,20/00/00,12.5,100%,80,45,1CFA68BB754E:60|2CFA68BB754E:100|3CFA68BB754E:100|4CFA68BB754E:100|5CFA68BB754E:100|,46672"));

    }

}
