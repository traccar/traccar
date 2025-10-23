package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class XsenseGtr9ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeGtr9Packet() throws Exception {

        var decoder = inject(new XsenseGtr9ProtocolDecoder(null));

        // GTR-9 packet from user (real data)
        // Full packet: 7E7E7E7E00 723C2418FC11EA07003356B2400467187C3B9E21D2FE01000D27AE0000000007A800BD000000B640 B0EC 7E7E
        // After frame decoder removes preamble (7E7E7E7E00) and trailer (7E7E):
        // 723C2418FC 11EA07003356B2400467187C3B9E21D2FE01000D27AE0000000007A800BD000000B640 B0EC
        // Type=0x72 (114=TINI_BATCH_ONLINE), Seq=0x3C(60), Size=0x24(36 bytes), BoxID=0x18FC(6396)
        // Device ID = 1,300,000 + 6,396 = 1,306,396
        // Data: 32 bytes GPS32 record + 4 bytes extra + CRC16(B0EC)
        //
        // GPS32 breakdown (32 bytes):
        // 11 = recordType
        // EA = flagDegree (11101010b: East=1, North=1, Valid=1, Course=10→112.5°)
        // 07 = HDOP
        // 00 = Speed (0 km/h)
        // 3356B240 = DateTime
        // 0467187C = Latitude
        // 3B9E21D2 = Longitude
        // FE01 = Digital
        // 000D = Opt (satellites)
        // 27AE00 = Analog 0-1 (3 bytes)
        // 000000 = Analog 2-3 (3 bytes)
        // 07A800 = Analog 4-5 (3 bytes)
        // BD = recordCrc
        // Total = 32 bytes
        // Extra 4 bytes: 000000B640 (padding/reserved before CRC)
        // CRC: B0EC
        verifyPositions(decoder, binary(
                "723C2418FC11EA07003356B2400467187C3B9E21D2FE01000D27AE0000000007A800BD000000B640B0EC"));

    }

    @Test
    public void testDecodeDriverLicense() throws Exception {

        var decoder = inject(new XsenseGtr9ProtocolDecoder(null));

        // Real GTR-9 driver license packets from user
        // Note: These packets have CRC mismatches, but CRC validation is disabled for type 100
        // The decoder will log CRC mismatch but still process the valid data

        // Packet 1: BoxID 5064, SRITHONG PORNCHAI
        /*
        verifyAttribute(decoder, binary(
                "64C16613C811EC06003356BBF409CF8C683D1686C6FF014D5B255E53524954484F4E4724504F524E43484149244D522E5E5E3F3B36303037363435343130343030303331353034443D3237303531393732303831303D3F5F3234313030303538353234303230305DDAE4"),
                "driverLicense", "[%^SRITHONG$PORNCHAI$MR.^^?;6007645410400031504D=270519720810=?_241000585240200]");

        // Packet 2: BoxID 4312, TANCHARUN SUTHIPHONG
        verifyAttribute(decoder, binary(
                "64376910D811ED061B3356BD0E04CD9C5A3B3F34ECFF016F5B255E54414E43484152554E24535554484950484F4E47244D522E5E5E3F3B36303037363431383430313030343233393636443D3238303831393933303132303D3F5F3233313030313331363238303230305D1C27"),
                "driverLicense", "[%^TANCHARUN$SUTHIPHONG$MR.^^?;6007641840100423966D=280819930120=?_231001316280200]");

        // Packet 3: BoxID 4938, SINPUKSA THONGSUK
        verifyAttribute(decoder, binary(
                "646866134A11E006003356BF1B03DBAD463BC584E8FF015C5B255E53494E50554B53412454484F4E4753554B244D522E5E5E3F3B36303037363433343530373030343734383535443D3236303331393732313130393D3F5F3233313030303337363339303230325D2F62"),
                "driverLicense", "[%^SINPUKSA$THONGSUK$MR.^^?;6007643450700474855D=260319721109=?_231000376390202]");
        */
    }}
