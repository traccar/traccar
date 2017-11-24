package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class StarcomProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        StarcomProtocolDecoder decoder = new StarcomProtocolDecoder(new StarcomProtocol());

        verifyPosition(decoder, text(
                "|unit=836673,unittype=5,address=186.227.158.251,kind=1,pending=1,mileage=23.808,odometer=1300,logic_state=1,reason=1,eventid=52,response=1,longitude=-46.50328,latitude=-23.54878,altitude=786,gps_valid=1,gps_connected=1,satellites=7,velocity=1,heading=0,emergency=0,driver=0,ignition=1,door=1,arm=0,disarm=0,extra1=0,extra2=0,extra3=0,siren=0,lock=0,immobilizer=1,unlock=0,fuel=0,rpm=0,modemsignal=0,main_voltage=12.06,backup_voltage=-1.00,analog1=0.00,analog2=0.00,analog3=0.00,datetime_utc=2017/11/16 03:18:59,datetime_actual=2017/11/16 03:18:59,network=TCPIP.1|\r\n"));

    }

}
