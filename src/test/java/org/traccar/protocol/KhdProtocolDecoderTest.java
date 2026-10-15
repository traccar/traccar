package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class KhdProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new KhdProtocolDecoder(null));

        verifyDecode(decoder, binary(
                "29298000a6c8dca3182610021401168061704410658602000000007f000000fff75cffffffffffffffff00001300243436303b30313b333331323b3237333035000500042952150004000801e5001600a35757413543382053433238585556312e38203b00000600a50000000c00060089ffffffff002400a901fe15060cf06aa9350cefb3e5300cf06aaa170cf06aa8110cef27a7110cef49800e000600c5ffffffff000400fa14117a0d"),
                position()
                        .attribute("model", "0000000C")
                        .attribute("debug", "WWA5C8 SC28XUV1.8 ;")
                        .network(network()
                                .cell(cell().mcc(460).mnc(1).lac(3312).cid(27305))
                                .cell(cell().mcc(510).mnc(21).lac(3312).cid(27305).signal(-53))
                                .cell(cell().mcc(510).mnc(21).lac(3311).cid(46053).signal(-48))
                                .cell(cell().mcc(510).mnc(21).lac(3312).cid(27306).signal(-23))
                                .cell(cell().mcc(510).mnc(21).lac(3312).cid(27304).signal(-17))
                                .cell(cell().mcc(510).mnc(21).lac(3311).cid(10151).signal(-17))
                                .cell(cell().mcc(510).mnc(21).lac(3311).cid(18816).signal(-14))));

        verifyDecode(decoder, binary(
                "2929A300403099934C2004030943310000000000000000000000007B0000007FFF0E0000E70014000000000018050B01303030314330334437312102007B2203140DDA610D"),
                position().attribute(Position.KEY_DRIVER_UNIQUE_ID, "0001C03D71"));

        verifyDecode(decoder, binary(
                "2929a3003e1680ba0a2304180759500000000000000000000000007b00000080001914000000000000000000162001641b0b0000249002bc58030001cc46020000e70d"),
                position().attribute(Position.KEY_BATTERY_LEVEL, 100));

        verifyDecode(decoder, binary(
                "2929800028258b8c10210731035840031534240542120200000337fb000000ffff5a00000a0000000005005d0d"),
                position());

        verifyDecode(decoder, binary(
                "292980002825863156210105095059035109370460010100000211ffff000002fc0000001e780b12000034e70d"),
                position());

        verifyDecode(decoder, binary(
                "2929a3003420b2ab46201115115601800115110350825100000133fb00df4bfdff0d000000000000000900000c180887d9ffffffffffff960d"),
                position());

        verifyDecode(decoder, binary(
                "2929b1000605162935b80d"));

        verifyDecode(decoder, binary(
                "29298e006d1f29402d181117083846801193910365274500000000f80000227ffc3f00001e00500000000000060088000000220019ffc100000000000000000000000000000000007080002000000016ff893839323534303231303734313134323334333639000800233030302e30306e0d"),
                position());

        verifyDecode(decoder, binary(
                "2929a3002e1780c663170216203353003060811013839500000114f8000000ffff5000000a00000000000000060102003db70d"),
                position());

        verifyDecode(decoder, binary(
                "292980002805162935140108074727801129670365336900000103ffff000082fc0000001e78091b000000360d"),
                position());

        verifyDecode(decoder, binary(
                "29298100280A9F9538081228160131022394301140372500000330FF0000007FFC0F00001E000000000034290D"),
                position());

        verifyDecode(decoder, binary(
                "29298000280A81850A120310095750005281370061190800000232F848FFBBFFFF0000001E000000000000ED0D"),
                position());

        verifyDecode(decoder, binary(
                "29298E00280F80815A121218203116022318461140227000720262FB00077C7FBF5600001E3C3200000000850D"),
                position());

        verifyDecode(decoder, binary(
                "29298200230AA2CC391205030505220285947903109550008002078400000002000000000000750D"),
                position());

        verifyDecode(decoder, binary(
                "29298500081DD08C22120312174026026545710312541700000000F819C839FFFF1D00001E00500000003AF90D"),
                position());

        verifyDecode(decoder, binary(
                "292980002822836665140825142037045343770193879200000050ffff000082fc000004b0780b170000002a0d"),
                position());

        verifyDecode(decoder, binary(
                "292980002802425349120811032137022373011140211100000334FFFF000082FC0000001E780913000034DF0D"),
                position());

    }

}
