package org.traccar.protocol;

import org.traccar.ProtocolTest;

import org.junit.Test;

public class Jt600ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Jt600ProtocolDecoder decoder = new Jt600ProtocolDecoder(new Jt600Protocol());

        verifyPosition(decoder, binary(
                "24311021600111001B16021105591022329862114046227B0598095080012327951435161F"),
                position("2011-02-16 05:59:10.000", true, 22.54977, -114.07705));

        verifyPosition(decoder, binary(
                "24312082002911001B171012052831243810120255336425001907190003FD2B91044D1FA0"));

        verifyPosition(decoder, binary(
                "24312082002911001B1710120533052438099702553358450004061E0003EE000000000C00"));

        verifyPosition(decoder, binary(
                "24608111888821001B09060908045322564025113242329F0598000001003F0000002D00AB"));

        verifyPosition(decoder, buffer(
                "(3110312099,W01,11404.6204,E,2232.9961,N,A,040511,063736,4,7,100,4,17,1,1,company)"),
                position("2011-05-04 06:37:36.000", true, 22.54994, 114.07701));

        verifyPosition(decoder, buffer(
                "(3120820029,W01,02553.3555,E,2438.0997,S,A,171012,053339,0,8,20,6,31,5,20,20)"));

        /*verifyPosition(decoder, text( ChannelBuffers.copiedBuffer(
                "(3330104377,U01,010100,010228,F,00.000000,N,000.000000,E,0,0,0,0%,00001000000000,741,14,22,0,206)", Charset.defaultCharset())));

        verifyPosition(decoder, text( ChannelBuffers.copiedBuffer(
                "(6221107674,2,U09,129,2,A,280513113036,E,02711.0500,S,1721.0876,A,030613171243,E,02756.7618,S,2300.0325,3491,538200,14400,1)", Charset.defaultCharset())));*/

    }

}
