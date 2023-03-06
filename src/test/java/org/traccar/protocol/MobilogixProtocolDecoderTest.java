package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class MobilogixProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MobilogixProtocolDecoder(null));

        verifyAttributes(decoder, text(
                "[2021-08-20 19:27:14,T14,1,V1.3.5,201909000982,53,12.18"));

        verifyAttributes(decoder, text(
                "\r\n[2021-08-20 19:27:14,T14,1,V1.3.5,201909000982,53,12.18"));

        verifyNull(decoder, text(
                "[2020-12-01 14:00:22,T1,1,V1.1.1,201951132031,,,12345678,724108005415815,359366080211420"));

        verifyNull(decoder, text(
                "[2020-10-25 20:44:08,T8,1,V1.2.3,201951132044,3596"));

        verifyPosition(decoder, text(
                "[2020-10-25 20:45:09,T9,1,V1.2.3,201951132044,59,10.50,701,-25.236860,-45.708530,0,314"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:46:10,T10,1,V1.2.3,201951132044,59,0.50,082,-25.909590,-47.045387,0,145"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:47:11,T11,1,V1.2.3,201951132044,3F,9.23,991,-25.909262,-47.045387,1,341"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:54:11,T12,1,V1.2.3,201951132044,3F,9.23,991,-25.909262,-47.045387,1,341"));

        verifyAttributes(decoder, text(
                "[2021-10-25 20:48:14,T14,1,V1.2.3,201951132044,51,0.50"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:49:15,T15,1,V1.2.3,201951132044,59,0.50,591,-25.908621,-47.045971,2,127"));

        verifyNull(decoder, text(
                "[2021-10-25 20:50:16,T16,1,V1.2.3,201951132044,1"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:51:21,T21,1,V1.2.3,201951132044,37,12.18,961,-25.932310,-47.022415,0,82"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:52:22,T22,1,V1.2.3,201951132044,1B,12.05,082,-25.909590,-47.045387,0,145"));

        verifyPosition(decoder, text(
                "[2021-10-25 20:53:31,T31,1,V1.2.3,201951132044,D3,26.17,961,-23.458092,-46.392132,0,8"));

        verifyAttribute(decoder, text(
                "[2021-10-25 20:55:11,T13,1,V1.2.3,201951132044,3F,9.23,991,-25.909262,-47.045387,1,341"),
                Position.KEY_TYPE, "T13");

        verifyPosition(decoder, text(
                "[2020-12-01 12:01:09,T3,1,V1.1.1,201951132031,3B,12.99,022,-23.563410,-46.588055,0,0"));

        verifyPosition(decoder, text(
                "[2021-09-30 20:06:35,T21,1,V1.3.5,201950130047,37,14.97,092,-23.494715,-46.851341,0,240,4.08,0,19516,4431,0.78,724,10,09111,00771,31,4680"));

    }

}
