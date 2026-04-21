package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class StarcomProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new StarcomProtocolDecoder(null));

        verifyPosition(decoder, text(
                "|unit=579978,unittype=5,address=196.190.61.110,kind=1,pending=0,mileage=127268.864,odometer=339863,logic_state=1,reason=20,eventid=1,response=0,longitude=40.86503,latitude=9.06824,altitude=1809,gps_valid=1,gps_connected=1,satellites=7,velocity=23,heading=130,emergency=0,driver=0,ignition=1,door=1,arm=0,disarm=0,extra1=0,extra2=0,extra3=0,siren=0,lock=0,immobilizer=0,unlock=0,fuel=0,rpm=0,modemsignal=0,main_voltage=14.11,backup_voltage=100.00,analog1=3.38,analog2=0.00,analog3=0.00,datetime_utc=2023/08/24 14:56:29,datetime_actual=2023/08/24 14:56:23,network=TCPIP 6600|\r\n"));

        verifyPosition(decoder, text(
                "|unit=416307,unittype=5,address=186.167.243.28,kind=14,software_version=14.02.18,hardware_type=17,gps_type=6,longitude=-67.85891,latitude=10.21988,datetime_actual=2019/05/07 21:59:38,network=TCPIP.1|\r\n"));

        verifyAttributes(decoder, text(
                "|unit=934706,unittype=5,address=186.167.251.198,kind=1,pending=0,mileage=202428.416,odometer=0,logic_state=1,reason=2,eventid=0,response=0,longitude=-66.99394,latitude=10.54119,altitude=544,gps_valid=1,gps_connected=1,satellites=7,velocity=29,heading=123,emergency=0,driver=0,ignition=1,door=0,arm=0,disarm=0,extra1=0,extra2=0,extra3=0,siren=0,lock=0,immobilizer=0,unlock=0,fuel=0,rpm=0,modemsignal=0,main_voltage=28.31,backup_voltage=96.00,analog1=0.00,analog2=0.00,analog3=0.37,datetime_utc=1963/08/16 15:43:56,datetime_actual=1899/12/30 00:00:00,network=TCPIP.1|\r\n"));

        verifyPosition(decoder, text(
                "|unit=111111,unittype=5,address=111.111.111.111,kind=1,pending=1,mileage=23.808,odometer=1300,logic_state=1,reason=1,eventid=52,response=1,longitude=-11.11111,latitude=-11.11111,altitude=786,gps_valid=1,gps_connected=1,satellites=7,velocity=1,heading=0,emergency=0,driver=0,ignition=1,door=1,arm=0,disarm=0,extra1=0,extra2=0,extra3=0,siren=0,lock=0,immobilizer=1,unlock=0,fuel=0,rpm=0,modemsignal=0,main_voltage=12.06,backup_voltage=-1.00,analog1=0.00,analog2=0.00,analog3=0.00,datetime_utc=2017/11/16 03:18:59,datetime_actual=2017/11/16 03:18:59,network=TCPIP.1|\r\n"));

    }

}
