package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

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

        // Packet from log: 7e7e7e7e00 72012413f1 11e3051c335705a508f4691c3cd4d492ff050012296d0000000008801502c0287000be 7c11 7e7e
        // After frame decoder: 72012413f1 11e3051c335705a508f4691c3cd4d492ff050012296d0000000008801502c0287000be 7c11
        // Type=0x72 (114=TINI_BATCH_ONLINE), Seq=0x01(1), Size=0x24(36 bytes), BoxID=0x13F1(5105)
        // Device ID = 1,300,000 + 5,105 = 1,305,105
        // Data: 32 bytes GPS32 record + 4 bytes extra + CRC16(7C11)
        verifyPositions(decoder, binary(
                "72012413F111E3051C335705A508F4691C3CD4D492FF050012296D0000000008801502C0287000BE7C11"));

        // New packet: 7E7E7E7E0072722405F911E207000C690D510467110F3B9E23B7FE010009273778300067300063BD000000B60590DC7E7E
        // After frame decoder: 72722405F911E207000C690D510467110F3B9E23B7FE010009273778300067300063BD000000B60590DC
        // Type=0x72 (114), Seq=0x72(114), Size=0x24(36), BoxID=0x05F9(1529)
        // Device ID = 1,300,000 + 1,529 = 1,301,529
        // Expected values:
        //   Type: 11 (0x11)
        //   Engine: 0
        //   flagDegree: 0xE2 (11100010b: East=1, North=1, Valid=1, Course=2 → 22.5°)
        //   HDOP: 7
        //   Speed: 0.0 km/h
        //   DateTime: 0x0C690D51 → 2025-10-23 08:42:34
        //   Latitude: 0x0467110F → 7.6439065°
        //   Longitude: 0x3B9E23B7 → 100.03659583333334°
        //   Digital: 0xFE01
        //   Satellites: 9
        //   Altitude: 0x2737 → 11.8872m
        //   Analog values: ana0=1923, ana1=0, ana2=1651, ana3=0, ana4=1595, ana5=3328
        //   RecordCRC: 0x05 (validated)
        verifyPositions(decoder, binary(
                "72722405F911E207000C690D510467110F3B9E23B7FE010009273778300067300063BD000000B60590DC"));

        // Packet with 0000 suffix (instead of 7E7E): 7e7e7e7e00 73092410ab...746b 0000
        // After frame decoder: 73092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b
        // Type=0x73 (115=OFFLINE), Seq=0x09(9), Size=0x24(36 bytes), BoxID=0x10AB(4267)
        // Device ID = 1,300,000 + 4,267 = 1,304,267
        // Note: Some devices send 0000 instead of 7E7E as suffix
        // Frame decoder now handles both 7E7E and 0000 as valid suffixes
        verifyPositions(decoder, binary(
                "73092410AB11E005003357110303C278C63BC19E82FE050012282D00000000079800000000700028746B"));

        // Ping reply packet: 7e7e7e7e00 6d008413f1 11e3051b...5555 05 837e 7e7e
        // After frame decoder: 6d008413f1 11e3051b...5555 05 837e
        // Type=0x6D (109=PING_REPLY_ENHIO), Seq=0x00, Size=0x84(132 bytes), BoxID=0x13F1(5105)
        // Device ID = 1,300,000 + 5,105 = 1,305,105
        // Note: Size field says 132 bytes but packet has only 129 bytes data
        //       Decoder handles this gracefully (may be old format 128+1 or incomplete extended)
        //       CRC validation will fail but position data should decode
        verifyNotNull(decoder, binary(
                "6D008413F111E3051B3357058608F400583CD44AB8FF010012296C0000000008851502B0A900000000001402080003000E01C1305F00000000000000002C000E2C000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000000000000000000000000555505837E"));

        // New ping reply: 7e7e7e7e00 6d458405f9 11e207000c6904f6...5555 9eee 7e7e
        // After frame decoder: 6d458405f9 11e207000c6904f6...5555 9eee
        // Type=0x6D (109=PING_REPLY_ENHIO), Seq=0x45(69), Size=0x84(132 bytes), BoxID=0x05F9(1529)
        // Device ID = 1,300,000 + 1,529 = 1,301,529
        // GPS32 data:
        //   DateTime: 0x0C6904F6 → 2025-10-23 07:39:44 (with GPS rollover)
        //   Latitude: 0x04671098 → 7.643886666666667°
        //   Longitude: 0x3B9E23E8 → 100.036604°
        //   Satellites: 11, Altitude: 11.8872m
        //   Analog: ana0=1922, ana1=0, ana2=1653, ana3=0, ana4=1596, ana5=0
        // Cell tower: MCC=520 (0x0208), MNC=3, Cell ID=0x0150992C
        verifyNotNull(decoder, binary(
                "6D458405F911E207000C6904F6046710983B9E23E8FE01000B273778200067500063C000720000000000000208000300000150992C00000000223347222C00002C07380000000000000000000000000000000000000000000000B40000000000000000000000000000000000000000000000000000000000000000000000000000000055559EEE"));

        // Ping reply: 7e7e7e7e00 6dd184127711e3050033568a69...5555 47af 7e7e
        // After frame decoder: 6dd184127711e3050033568a69...5555 47af
        // Type=0x6D (109=PING_REPLY_ENHIO), Seq=0xD1(209), Size=0x84(132 bytes), BoxID=0x1277(4727)
        // Device ID = 1,300,000 + 4,727 = 1,304,727
        // GPS32 data:
        //   flagDegree: 0xE3 (11100011b: East=1, North=1, Valid=1, Course=3 → 45°)
        //   HDOP: 5
        //   Speed: 0 km/h
        //   DateTime: 0x33568A69
        //   Latitude: 0x0A4053A8 → 17.74088°
        //   Longitude: 0x3D6E9492 → 103.86025333333333°
        //   Digital: 0xFE01
        //   Satellites: 20 (0x14)
        //   Altitude: 0x2997 → 77.724m
        //   Analog: ana0=0, ana1=0, ana2=1920, ana3=0, ana4=51, ana5=0
        // Cell tower: MCC=520 (0x0208), MNC=5, Cell ID=0x010A8D29
        verifyNotNull(decoder, binary(
                "6DD184127711E3050033568A690A4053A83D6E9492FE0100142997000000000780000000330000000000500208000500010A8D29D600000000000000002C00012C0000000000000000000000000000000000000000000000000B0000000000000000000000000000000000000000000000000000000000000000000000000000000000555547AF"));

        // Packet with CRC at size field position (not at end): 7e7e7e7e00 720924199411e0060033571f79...0dd0...b52f 7e7e
        // After frame decoder: 720924199411e0060033571f79...0dd0...b52f
        // Type=0x72 (114=TINI_BATCH_ONLINE), Seq=0x09(9), Size=0x24(36 bytes), BoxID=0x1994(6548)
        // Device ID = 1,300,000 + 6,548 = 1,306,548
        // This packet has CRC at position (size-2) = 34, not at the end (position 41)
        // Decoder tries both positions and uses whichever validates correctly
        // GPS32 data at position 5-36, then trailing data 37-42
        verifyPositions(decoder, binary(
                "720924199411E0060033571F79086117F23BD381B0FF05001127210DD000000824000FD9007000A8B52F"));

        // More packets with CRC at last 2 bytes (all have Size=36, Actual=42, trailing 6 bytes)
        // Packet: BoxID 0x0FB0 (4016), Device 1304016
        verifyPositions(decoder, binary(
                "7208240FB011F1060B335721AE03C1D2183BC148AFFF050012280F000000000878000150007000DF548E"));

        // Packet: BoxID 0x0E5A (3674), Device 1303674
        verifyPositions(decoder, binary(
                "7201240E5A11E00A000C6921AE07F168B63B5688EFFE050008271F0000000008006B00000070B688EF28"));

        // Packet: BoxID 0x1563 (5475), Device 1305475
        verifyPositions(decoder, binary(
                "720324156311F50604335721AF0891008E3C3B5772FF050010283D0000000008700004101070008BDA44"));

        // Packet: BoxID 0x1ABF (6847), Device 1306847
        verifyPositions(decoder, binary(
                "7207241ABF11E00700335721AF0A439B623D16AD90FF05000E29B307C00000086C000E00007000B27CF6"));

        // Ping reply with 2-byte suffix: 7e7e7e7e00 6d538418981...5555edbc 7e7e
        // After frame decoder: 6d538418981...5555edbc (removes 5-byte preamble and 2-byte suffix 7E7E)
        // Type=0x6D (109=PING_REPLY_ENHIO), Seq=0x53(83), Size=0x84(132 bytes), BoxID=0x1898(6296)
        // Device ID = 1,300,000 + 6,296 = 1,306,296
        // Note: This packet has 2-byte suffix (7E7E) not 4 bytes like position packets
        // Frame decoder detects 7E7E at the end and removes it dynamically
        verifyNotNull(decoder, binary(
                "6D5384189811E1061D335723B1099171EE3D0E9AD7FF010010291103B0000007E3000000880000000000140208000500010A8EB0C600000000000000002C00012C0000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000005555EDBC"));

        // Position report with 2-byte suffix: 7e7e7e7e00 727424189811e007223357250c...6069 7e7e
        // After frame decoder: 727424189811e007223357250c...6069 (removes 5-byte preamble and 2-byte suffix)
        // Type=0x72 (114=TINI_BATCH_ONLINE), Seq=0x74(116), Size=0x24(36 bytes), BoxID=0x1898(6296)
        // Device ID = 1,300,000 + 6,296 = 1,306,296
        // CRC at last 2 bytes [40-41] = 0x6069, validates correctly
        verifyPositions(decoder, binary(
                "727424189811E007223357250C09996CB43D1125F0FF05000F29410430000008B0000000007000C06069"));

    }

    @Test
    public void testDecodeDriverLicense() throws Exception {

        var decoder = inject(new XsenseGtr9ProtocolDecoder(null));

        // Real GTR-9 driver license packet from user
        // Packet 3: BoxID 4938, SINPUKSA THONGSUK - CRC validates correctly (0x2F62)
        verifyAttribute(decoder, binary(
                "646866134A11E006003356BF1B03DBAD463BC584E8FF015C5B255E53494E50554B53412454484F4E4753554B244D522E5E5E3F3B36303037363433343530373030343734383535443D3236303331393732313130393D3F5F3233313030303337363339303230325D2F62"),
                Position.KEY_DRIVER_UNIQUE_ID, "%^SINPUKSA$THONGSUK$MR.^^?;6007643450700474855D=260319721109=?_231000376390202");
    }

    @Test
    public void testDecodeGtr9PacketWith0000Suffix() throws Exception {

        var decoder = inject(new XsenseGtr9ProtocolDecoder(null));

        // GTR-9 packet ending with 0000 suffix (not 7E7E)
        // Full packet: 7E7E7E7E00 736824189811E1051E33571A570930B6383D01538CFF050012298203D0000008740000000070008168E2 0000
        // After frame decoder removes preamble (7E7E7E7E00) and suffix (0000):
        // 736824189811E1051E33571A570930B6383D01538CFF050012298203D0000008740000000070008168E2
        // Type=0x73 (115=M_TINI_BATCH_OFFLINE), Seq=0x68(104), Size=0x24(36 bytes), BoxID=0x1898(6296)
        // Device ID = 1,300,000 + 6,296 = 1,306,296
        // Data: 32 bytes GPS32 record + 4 bytes extra + CRC16(68E2)
        //
        // GPS32 breakdown (32 bytes):
        // 11 = recordType
        // E1 = flagDegree (11100001b: East=1, North=1, Valid=1, Course=01→22.5°)
        // 05 = HDOP
        // 1E = Speed (30 km/h)
        // 33571A57 = DateTime
        // 0930B638 = Latitude
        // 3D01538C = Longitude
        // FF05 = Digital
        // 0012 = Opt (satellites)
        // 298203 = Analog 0-1 (3 bytes)
        // D00000 = Analog 2-3 (3 bytes)
        // 087400 = Analog 4-5 (3 bytes)
        // 00 = recordCrc (placeholder)
        // Extra: 00007000 (4 bytes)
        // CRC: 8168E2 (last 2 bytes at position 40-41)
        verifyPositions(decoder, binary(
                "736824189811E1051E33571A570930B6383D01538CFF050012298203D0000008740000000070008168E2"));
    }

}
