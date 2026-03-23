package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class ES4x0ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeRegular() throws Exception {

        var decoder = inject(new ES4x0ProtocolDecoder(null));

        verifyNotNull(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "01" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0018" // mask: latitude and longitude
                + "12A292CC" // latitude: 31.26443 degrees
                + "47F5994C" // longitude: 120.72779 degrees
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "02" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0001" // mask: only satellites
                + "08" // satellites
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "03" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0002" // mask: only HDOP
                + "05" // HDOP
                + "0000" // checksum
        ));

        verifyNotNull(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "04" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0004" // mask: only GPS time
                + "65F1E240" // GPS time: 1712345696 (2024-04-05)
                + "0000" // checksum
        ));

        verifyNotNull(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "05" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0018" // mask: latitude and longitude
                + "12A292CC" // latitude: 31.26443 degrees
                + "47F5994C" // longitude: 120.72779 degrees
                + "0000" // checksum
        ));

        verifyNotNull(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "06" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0020" // mask: speed
                + "00002710" // speed (100 km/h)
                + "0000" // checksum
        ));

        verifyNotNull(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "07" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0040" // mask: course
                + "005A" // course (90 degrees)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "08" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0080" // mask: input status
                + "01" // input status
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "09" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0100" // mask: output status
                + "02" // output status
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0A" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "15" // event ID (overspeed)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0B" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0400" // mask: odometer
                + "00002710" // odometer
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0C" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0800" // mask: ADC
                + "03E8" // ADC
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0D" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "1000" // mask: power voltage
                + "0BB8" // power voltage (30V)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0E" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "2000" // mask: battery voltage
                + "0BB8" // battery voltage (3V)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "0F" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "4000" // mask: RSSI
                + "1E" // RSSI (30)
                + "0000" // checksum
        ));

    }

    @Test
    public void testDecodeMaintenance() throws Exception {

        var decoder = inject(new ES4x0ProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "01" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "00FF" // mask: all fields present
                + "04" // event ID
                + "1E" // RSSI (30)
                + "01" // network registration
                + "01" // PDP state
                + "00" // message queue
                + "3132333435363738393031323334353637383930" // ICCID (20 bytes)
                + "31323334353637383930313233343536373839303132333435313233343531323334353637383930313233343536373839303132333435" // firmware version (35 bytes)
                + "3132333435363738393031323334" // hardware version (14 bytes)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "02" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0001" // mask: only event ID
                + "04" // event ID
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "03" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0002" // mask: only RSSI
                + "1E" // RSSI (30)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "04" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0020" // mask: only ICCID
                + "3132333435363738393031323334353637383930" // ICCID (20 bytes)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "05" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0040" // mask: only firmware version
                + "31323334353637383930313233343536373839303132333435313233343531323334353637383930313233343536373839303132333435" // firmware version (35 bytes)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4D" // type: Maintenance Report
                + "06" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0080" // mask: only hardware version
                + "3132333435363738393031323334" // hardware version (14 bytes)
                + "0000" // checksum
        ));

    }

    @Test
    public void testDecodeObd() throws Exception {

        var decoder = inject(new ES4x0ProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "01" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "00FF" // mask: all fields present
                + "04" // event ID
                + "3132333435363738393031323334353637" // VIN
                + "07D0" // RPM (2000)
                + "3C" // OBD speed (60 km/h)
                + "50" // fuel level (80%)
                + "00" // MIL state
                + "01" // ignition
                + "02" // DTC count
                + "503030300000" // DTC 1
                + "503030310000" // DTC 2
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "02" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0001" // mask: only event ID
                + "04" // event ID
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "03" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0002" // mask: only VIN
                + "3132333435363738393031323334353637" // VIN
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "04" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0004" // mask: only RPM
                + "07D0" // RPM (2000)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "05" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0008" // mask: only OBD speed
                + "3C" // OBD speed (60 km/h)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "06" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0010" // mask: only fuel level
                + "50" // fuel level (80%)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "07" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0020" // mask: only MIL state
                + "00" // MIL state
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "08" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0040" // mask: only ignition
                + "01" // ignition
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "4F" // type: OBD Report
                + "09" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0080" // mask: only DTCs
                + "02" // DTC count
                + "503030300000" // DTC 1
                + "503030310000" // DTC 2
                + "0000" // checksum
        ));

    }

    @Test
    public void testDecodeAlarm() throws Exception {

        var decoder = inject(new ES4x0ProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "01" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "04" // event ID (general alarm)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "02" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "15" // event ID (overspeed)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "03" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "0B" // event ID (low battery)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "04" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "02" // event ID (power cut)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "05" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "20" // event ID (braking)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "06" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "21" // event ID (acceleration)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "07" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "22" // event ID (accident)
                + "0000" // checksum
        ));

        verifyAttributes(decoder, binary(
                "4554343100" // header
                + "86011102" // IMEI (BCD compressed: 860111020114389)
                + "0114389F" // IMEI continued
                + "52" // type: Regular Report
                + "08" // sequence
                + "65F1E240" // message time: 1712345696 (2024-04-05)
                + "0200" // mask: event ID
                + "19" // event ID (tow)
                + "0000" // checksum
        ));

    }

}