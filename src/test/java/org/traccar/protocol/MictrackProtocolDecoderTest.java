package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class MictrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeStandard() throws Exception {

        var decoder = inject(new MictrackProtocolDecoder(null));

        verifyAttributes(decoder, text(
                "MT;5;867035041396795;Y1;220111085741+test,8c:53:c3:db:e7:26,-58,jiuide-842,80:26:89:f0:5e:4f,-74,jiu2ide 403,94:e4:4b:0a:31:08,-75,jiu3ide,7a:91:e9:50:26:0b,-85,CNet-9rNe,78:91:e9:40:26:0b,-87+0+4092+1"));

        verifyAttribute(decoder, text(
                "867035041390699 netlock=Success!"),
                Position.KEY_RESULT, "netlock=Success");

        verifyAttribute(decoder, text(
                "mode=Success!"),
                Position.KEY_RESULT, "mode=Success");

        verifyPosition(decoder, text(
                "MT;6;866425031361423;R0;10+190109091803+22.63827+114.02922+2.14+69+2+3744+113"),
                position("2019-01-09 09:18:03.000", true, 22.63827, 114.02922));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R1;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;1;866425031379169;R2;181129081017+0,21681,20616,460+4+3976+0"));

        verifyAttributes(decoder, text(
                "MT;1;866425031379169;R3;181129081017+0,167910723,14924,460,176+4+3976+0"));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R12;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+0,21681,20616,460+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R13;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+0,167910723,14924,460,176+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;5;866425031379169;RH;5+190116112648+0+0+0+0+11+3954+1"));
    }

    @Test
    public void testDecodeLowAltitude() throws Exception {

        var decoder = inject(new MictrackProtocolDecoder(null));

        verifyPositions(decoder, text(
                "861836051888035$162835.00,A,4139.6460,N,07009.7239,W,,41.53,-25.8,220621"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117$062332.00,A,2238.2836,N,11401.7386,E,0.06,209.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"),
                position("2017-11-13 06:22:32.000", true, 22.63806, 114.028976));
    }

    @Test
    public void testDecodeHQ() throws Exception {

        var decoder = inject(new MictrackProtocolDecoder(null));

        verifyPosition(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283"),
                position("2015-07-10 04:36:02.000", true, 22.58212, 113.90664));

        verifyPosition(decoder, text(
                "*HQ,8168000008,V5,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283,1000,125"));

        verifyPosition(decoder, text(
                "*HQ,8168000008,V6,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283,898602A2091508006821"));

        verifyAttribute(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFD,460,00,10342,4283"),
                Position.KEY_ALARM, Position.ALARM_SOS);

        verifyAttribute(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFB,460,00,10342,4283"),
                Position.KEY_ALARM, Position.ALARM_OVERSPEED);

        verifyNull(decoder, text(
                "*HQ,8168000008,V4,V1,20150710043602"));

        verifyPosition(decoder, text(
                "*HQ,8168000008,V1,083000,A,3600.0000,N,09600.0000,W,000.00,142,010124,FFF7FBFF,310,410,12345,67890"),
                position("2024-01-01 08:30:00.000", true, 36.0, -96.0));

    }

    @Test
    public void testDecodeMT700() throws Exception {

        var decoder = inject(new MictrackProtocolDecoder(null));

        // GPS available, LBS OFF
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815$GPRMC,123318.00,A,2238.8946,N,11402.0635,E,,,100124,,,A*5C\n"));

        // GPS available, LBS ON
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#$GPRMC,123548.00,A,2238.8936,N,11402.0640,E,,,100124,,,A*5A\n"));

        // GPS unavailable, LBS ON
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#460,00,1D29,156153D$GPRMC,121831.00,V,,,,,,,100124,,,A*7C\n"));

        // WiFi, LBS OFF
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815$WIFI,124517.00,A,-39,6877248FA31A,-39,7E77248FA31A,-73,DC333DF82C74,-75,0260736CF982,-77,90769F421140,100124*0E\n"));

        // WiFi, LBS ON
        verifyAttributes(decoder, text(
                "#862255061947757#MT700#0000#AUTO#1\n#3815#460,00,262C,11F1$WIFI,022300.00,A,-31,6877248FA31A,-32,7E77248FA31A,-73,0260736CF982,-74,DC333DF82C74,-74,90769F421140,100124*74\n"));

        // SHAKE alarm
        verifyPosition(decoder, text(
                "#862255061947757#MT700#0000#SHAKE#1\n#3815$GPRMC,090000.00,A,2238.8946,N,11402.0635,E,0.0,0.0,100124,,,A*00\n"));

        // MT700W variant header
        verifyAttributes(decoder, text(
                "#862255061947757#MT700W#0000#AUTO#1\n#3815#460,00,262C,11F1$WIFI,095147.00,V,,,,,,,,,,,241223*06\n"));

        // TOWED alarm, GPS unavailable, low voltage raw value (37 = 3.7V)
        verifyAttribute(decoder, text(
                "#862255061947757#MT700#0000#TOWED#1\n#37$GPRMC,090000.00,V,,,,,,,100124,,,A*7C\n"),
                Position.KEY_ALARM, Position.ALARM_TOW);

        // MT700 DEF = device remove / light sensor alarm (not power cut)
        verifyAttribute(decoder, text(
                "#862255061947757#MT700#0000#DEF#1\n#3815$GPRMC,090000.00,A,2238.8946,N,11402.0635,E,0.0,0.0,100124,,,A*00\n"),
                Position.KEY_ALARM, Position.ALARM_REMOVING);

        // MT600 DEF = cut power alarm
        verifyAttribute(decoder, text(
                "#862255061947757#MT600#0000#DEF#1\n#3815$GPRMC,090000.00,A,2238.8946,N,11402.0635,E,0.0,0.0,100124,,,A*00\n"),
                Position.KEY_ALARM, Position.ALARM_POWER_CUT);

    }

}
