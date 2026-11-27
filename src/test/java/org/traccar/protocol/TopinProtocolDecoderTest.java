package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class TopinProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TopinProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "787801080D0A"));

        verifyDecode(decoder, binary(
                "78780d0103593390754169634d0d0a"));

        verifyDecode(decoder, binary(
                "7878041a2611071034381c7f2c32dad0491c599ba9778c52b43ae2b7116b52b43ae2b711685203025a0200007a27000080bb4600007a270000f04e5000007a270000bc3b50000d0a"),
                position(Checks.NONE).network(network()
                        .cell(cell().mcc(602).mnc(2).lac(31271).cid(32955))
                        .cell(cell().mcc(602).mnc(2).lac(31271).cid(61518))
                        .cell(cell().mcc(602).mnc(2).lac(31271).cid(48187))));

        verifyDecode(decoder, binary(
                "787803181604130318491475905bd30e25001e10bbf7635d14759006e626560401cc00000028660090df425f000028660090df576c00002866009487566700002866009ca15667000d0a"),
                position(Checks.NONE));

        verifyDecode(decoder, binary(
                "7878006921120412565802010601071e4a9764071e4a9864010d0a"),
                position(Checks.ATTRIBUTES).attribute(Position.KEY_ALARM, Position.ALARM_VIBRATION));

        verifyDecode(decoder, binary(
                "787801940D0A"),
                position(Checks.ATTRIBUTES).attribute(Position.KEY_ALARM, Position.ALARM_VIBRATION));

        verifyDecode(decoder, binary(
                "787801990D0A"),
                position(Checks.ATTRIBUTES).attribute(Position.KEY_ALARM, Position.ALARM_SOS));

        verifyDecode(decoder, binary(
                "78780A13424008196400041F000D0A"),
                position(Checks.ATTRIBUTES));

        verifyDecode(decoder, binary(
                "78781510120B05030D2498038077200BE2078F0034000102030D0A"),
                position());

        verifyDecode(decoder, binary(
                "7878200813081A0733211608C8D1710DED1D1608DFFB710E06D51039050100286489000D0A"),
                position());

        verifyDecode(decoder, binary(
                "78782008140709121f36300d769f02058cfd300d771202058c6f0000000300005c99000d0a"),
                position());

        verifyDecode(decoder, binary(
                "787812100A03170F32179C026B3F3E0C22AD651F34600D0A"),
                position());

        verifyDecode(decoder, binary(
                "78780a132827010063000000000d0a"),
                position(Checks.ATTRIBUTES));

        verifyDecode(decoder, binary(
                "7878001719111120141807019456465111aa3c465111ab464651c1a550465106b150465342f750465342f65a465111a95a000d0a"),
                position(Checks.NONE));

        verifyDecode(decoder, binary(
                "787812100a03170f32179c026b3f3e0c22ad651f34600d0a"),
                position());

        verifyDecode(decoder, binary(
                "78780713514d0819640d0a"),
                position(Checks.ATTRIBUTES));

        verifyDecode(decoder, binary(
                "787801300d0a"));

    }

}
