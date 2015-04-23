package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.traccar.helper.ChannelBufferTools;

public class H02ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        H02ProtocolDecoder decoder = new H02ProtocolDecoder(null);

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,1451316409,V1,030149,A,-23-29.0095,S,-46-51.5852,W,2.4,065,070315,FFFFFFFF#", Charset.defaultCharset())));

        assertNull(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,353588020068342,V1,000000,V,0.0000,0,0.0000,0,0.00,0.00,000000,ffffffff,000106,000002,000203,004c87,16#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,3800008786,V1,062507,V,3048.2437,N,03058.5617,E,000.00,000,250413,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,4300256455,V1,111817,A,1935.5128,N,04656.3243,E,0.00,100,170913,FFE7FBFF#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,123456789012345,V1,155850,A,5214.5346,N,2117.4683,E,0.00,270.90,131012,ffffffff,000000,000000,000000,000000#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,353588010001689,V1,221116,A,1548.8220,S,4753.1679,W,0.00,0.00,300413,ffffffff,0002d4,000004,0001cd,000047#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,354188045498669,V1,195200,A,701.8915,S,3450.3399,W,0.00,205.70,050213,ffffffff,000243,000000,000000#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,2705171109,V1,213324,A,5002.5849,N,01433.7822,E,0.00,000,140613,FFFFFFFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V1,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S17,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S14,100,10,1,3,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S20,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S20,DONE,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,F7FFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,R8,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S23,165.165.33.250:8800,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S24,thit.gd,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*TH,2020916012,V4,S1,OK,pass_word,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFD#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,353588020068342,V1,062840,A,5241.1249,N,954.9490,E,0.00,0.00,231013,ffffffff,000106,000002,000203,004c87,24#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,353505220903211,V1,075228,A,5227.5039,N,01032.8443,E,0.00,0,231013,FFFBFFFF,106,14, 201,2173#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,353505220903211,V1,140817,A,5239.3538,N,01003.5292,E,21.03,312,221013,FFFBFFFF,106,14, 203,1cd#", Charset.defaultCharset())));
        
        verify(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,356823035368767,V1,083618,A,0955.6392,N,07809.0796,E,0.00,0,070414,FFFBFFFF,194,3b5,  71,c9a9#", Charset.defaultCharset())));
        
        assertNull(decoder.decode(null, null, ChannelBuffers.copiedBuffer(
                "*HQ,8401016597,BASE,152609,0,0,0,0,211014,FFFFFFFF#", Charset.defaultCharset())));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2427051711092133391406135002584900014337822e000000ffffffffff0000"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2427051711092134091406135002584900014337822e000000ffffffffff0000"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2410307310010503162209022212874500113466574C014028fffffbffff0000"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "2441090013450831250401145047888000008554650e000000fffff9ffff001006000000000106020299109c01"))));

        verify(decoder.decode(null, null, ChannelBuffers.wrappedBuffer(ChannelBufferTools.convertHexString(
                "24270517030820321418041423307879000463213792000056fffff9ffff0000"))));

    }

}
