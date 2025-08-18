package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FoxProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FoxProtocolDecoder(null));

        verifyPosition(decoder, text(
                "<fox><gps id=\"10\" data=\"51,A,010416,085317,4444.4158,N,02025.4466,E,1,182,,1110111111110111 141 0 0 0 0 0 10010000 10142,018C81851800009B\"/></fox>"));

        verifyPosition(decoder, text(
                "<fox><gps id=\"90\" data=\"1092,V,010101,000004,0000.0000,N,00000.0000,E,0,0,,1111111111111111 123 0 0 0 0 0 00000000 47664,47664\"/></fox>"));

        verifyPosition(decoder, text(
                "<fox><gps id=\"90\" data=\"31,V,110316,125952,0000.0000,N,00000.0000,E,0,0,,1111111111111111 123 0 0 0 0 0 00000000 47664,65 60 60 60 60 60 60 60 65 65 55 55 60 60 60 60 60 60 60 60 55 55 60 55 65 60 60 60 60 60 60 55\"/></fox>"));

        verifyPosition(decoder, text(
                "<fox><gps id=\"90\" data=\"0,V,110316,130154,0000.0000,N,00000.0000,E,0,0,,1111111111111111 123 0 0 0 0 0 00000000 47664,47664 0\"/></fox>"));

        verifyPosition(decoder, text(
                "<fox><gps id=\"90\" data=\"0,A,110316,131834,4448.8355,N,02028.2021,E,0,217,,1111111111111111 123 0 0 0 0 0 00000000 50020,50020 0\"/></fox>"));

    }

}
