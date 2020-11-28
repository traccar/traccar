package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import org.junit.Test;
import org.traccar.Context;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

import static org.junit.Assert.assertEquals;

public class WatchProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        WatchProtocolDecoder decoder = new WatchProtocolDecoder(null);

        verifyPosition(decoder, buffer(
                "[3G*358839237678820*0122*ALCUSTOMER1,251120,081821,V,0.0,N,0.0,E,2.58,317.462,35.147,14,100,2,11089,0,00100008,1,1,460,01,42308,101992452,100,5,shizhou1,44:56:e2:03:ea:2a,-69,FART3,30:0d:9e:bb:fa:4d,-70,ZKY-A209,88:c3:97:c1:f4:7f,-73,ChinaNet-HNeD,e8:84:c6:21:7c:dc,-77,,30:45:96:10:14:5d,-79,1.2035439]"));

        verifyPosition(decoder, buffer(
                "[3G*0304187088*0100*UD_WCDMA,100720,094202,V,0.0,N,0.0,E,22.0,0,-1,21,75,92,0,0,00000000,1,1,425,01,10192,1282125,75,5,Inet,04:f0:21:46:1f:57,-54,iNetSecurity,00:1e:42:25:2f:3e,-71,Gilad,58:d5:6e:9d:1b:af,-80,weekend,14:ae:db:cb:99:25,-82,advancemed1,04:f0:21:4c:c8:3e,-89,0.0]"));

        verifyPosition(decoder, buffer(
                "[3G*8809008845*00C0*AL,271219,094744,V,00.000000,N, 0.0000000,E,0.00,0.0,0.0,0,100,81,0,0,00010000,7,0,460,0,9336,3981,141,9336,3912,141,9336,3982,140,9765,4233,134,9765,4071,134,9765,4321,134,9336,4353,132,0,0.0]"));

        verifyPosition(decoder, buffer(
                "[3G*2104134718*00A1*UD_WCDMA,161019,134938,A,43.373367,N,71.157615,W,22.0,350.206,279.717,17,28,79,0,0,00000000,1,1,310,410,23999,132013696,28,1,Home2,60:45:cb:cb:34:68,-93,8.263865]"));

        verifyPosition(decoder, buffer(
                "[ZJ*014111001332708*0075*0064*AL,040418,052156,A,22.536207,N,113.938673,E,0,0,0,5,100,82,1000,50,00100000,1,255,460,0,9340,3663,35]"));

        verifyPosition(decoder, buffer(
                "[SG*352661090143150*006C*UD,150817,132115,V,28.435142,N,81.354333,W,2.2038,000,99,00,70,100,0,50,00000000,1,1,310,260,1091,30082,139,,00]"));

        verifyAttributes(decoder, buffer(
                "[3G*4700609403*0013*bphrt,120,79,73,,,,]"));

        verifyAttributes(decoder, buffer(
                "[ZJ*357653059860416*0007*000c*BLOOD,109,68]"));

        verifyPosition(decoder, buffer(
                "[3G*8308373902*0080*AL,230817,095346,A,47.083950,N,15.4821850,E,7.60,273.8,0.0,4,15,44,0,0,00200010,2,255,232,1,7605,42530,118,7605,58036,119,0,65.8]"));

        verifyPosition(decoder, buffer(
                "[SG*9051007430*006C*UD,150817,132115,V,28.435142,N,81.354333,W,2.2038,000,99,00,70,100,0,50,00000000,1,1,310,260,1091,30082,139,,00]"));

        verifyPosition(decoder, buffer(
                "[3G*6005412902*011F*WT,170517,133811,V,18.512200,N,73.7750283,E,0.00,0.0,0.0,0,92,82,4262,0,00000010,2,1,404,22,10125,8301,141,10125,13921,122,5,Skynet,28:c6:8e:be:87:c0,-60,Intel Wi-Fi,4c:60:de:32:3d:38,-70,Nirvanic-2,40:e3:d6:4a:d9:c2,-73,A4-Guest,40:e3:d6:4a:d9:c4,-73,A4Idatix,40:e3:d6:4a:d9:c3,-73,13.8]"));

        verifyPosition(decoder, buffer(
                "[3G*8308406279*00CC*UD3,170417,190930,V,54.739618,N,25.273213,E,0.0,323.53,175.1,6,51,83,0,0,00000000,1,1,246,01,200,13242758,51,3,TEO-189835,00:8c:54:58:1d:64,-84,Cgates_7137,78:54:2e:e3:71:37,-85,ASUS,9c:5c:8e:b8:d4:78,-93]"));

        verifyPosition(decoder, buffer(
                "[SG*9051004074*0058*AL,120117,145602,V,40.058413,N,76.336618,W,11.519,188,99,00,01,80,0,50,00000000,0,1,0,0,,10]"));

        verifyPosition(decoder, buffer(
                "[SG*9051000884*009B*UD,030117,161129,V,52.745450,N,0.369512,,0.1481,000,99,00,70,5,0,50,00000000,5,1,234,15,893,3611,135,893,3612,132,893,3993,131,893,30986,129,893,40088,126,,00]"));

        verifyPosition(decoder, buffer(
                "[3G*6430073509*00E7*UD2,241016,081622,V,09.951861,N,-84.1422119,W,0.00,0.0,0.0,0,39,94,0,0,00000000,1,0,712,3,2007,18961,123,4,Luz,00:23:6a:34:ee:76,-70,familia,b0:c5:54:b9:90:ef,-78,fam salas delgado,fc:b4:e6:5d:50:ea,-81,QWERTY,c8:3a:35:43:0f:e8,-93]"));

        verifyPosition(decoder, buffer(
                "[3G*6105117105*008D*UD2,210716,231601,V,-33.480366,N,-70.7630692,E,0.00,0.0,0.0,0,100,34,0,0,00000000,3,255,730,2,29731,54315,167,29731,54316,162,29731,54317,145]"),
                position("2016-07-21 23:16:01.000", false, -33.48037, -70.76307));

        verifyPosition(decoder, buffer(
                "[3G*4700222306*0077*UD,120316,140610,V,48.779045,N, 9.1574736,E,0.00,0.0,0.0,0,25,83,0,0,00000000,2,255,262,1,21041,9067,121,21041,5981,116]"));

        verifyPosition(decoder, buffer(
                "[3G*4700222306*011F*UD2,120316,140444,A,48.779045,N, 9.1574736,E,0.57,12.8,0.0,7,28,77,0,0,00000000,2,2,262,1,21041,9067,121,21041,5981,116,5,WG-Superlativ,34:31:c4:c8:a9:22,-67,EasyBox-28E858,18:83:bf:28:e8:f4,-70,MoMaXXg,be:05:43:b7:19:15,-72,MoMaXX2,bc:05:43:b7:19:15,-72,Gastzugang,18:83:bf:28:e8:f5,-72]"));

        verifyNull(decoder, buffer(
                "[SG*9081000548*0009*LK,0,100]"));

        verifyPosition(decoder, buffer(
                "[SG*9081000548*00A9*UD,110116,113639,V,16.479064,S,68.119072,,0.7593,000,99,00,80,80,0,50,00000000,5,1,736,2,10103,10732,153,10103,11061,152,10103,11012,152,10103,10151,150,10103,10731,143,,00]"));

        verifyPosition(decoder, buffer(
                "[3G*2256002206*0079*UD2,100116,153723,A,38.000000,N,-9.000000,W,0.44,299.3,0.0,7,100,86,0,0,00000008,2,0,268,3,3010,51042,146,3010,51043,132]"));

        verifyNull(decoder, buffer(
                "[3G*8800000015*0003*TKQ]"));

        verifyPosition(decoder, buffer(
                "[3G*4700186508*00B1*UD,301015,084840,V,45.853100,N,14.6224899,E,0.00,0.0,0.0,0,84,61,0,11,00000008,7,255,293,70,60,6453,139,60,6432,139,60,6431,132,60,6457,127,60,16353,126,60,6451,121,60,16352,118]"));

        verifyNull(decoder, buffer(
                "[SG*8800000015*0002*LK]"));

        verifyAttributes(decoder, buffer(
                "[3G*4700186508*000B*LK,0,10,100]"));

        verifyPosition(decoder, buffer(
                "[SG*8800000015*0087*UD,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141]"),
                position("2014-04-22 13:46:52.000", true, 22.57171, 113.86140));

        verifyPosition(decoder, buffer(
                "[SG*8800000015*0087*UD,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141]"));

        verifyPosition(decoder, buffer(
                "[SG*8800000015*0088*UD2,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141]"));

        verifyPosition(decoder, buffer(
                "[SG*8800000015*0087*AL,220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0001,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141]"));

        verifyAttributes(decoder, buffer(
                "[CS*8800000015*0008*PULSE,72]"));

        verifyAttributes(decoder, buffer(
                "[3G*6005412902*0007*heart,0]"));

        verifyAttributes(decoder, buffer(
                "[3G*6005412902*0008*heart,71]"));

        verifyPosition(decoder, buffer(
                "[ZJ*014111001350304*0033*0064*UD,070318,020827,V,00.000000,N,000.000000,E,0,0,0,0,100,19,1000,50,00000000,1,255,460,0,9346,5223,42]"));

        verifyPosition(decoder, buffer(
                "[ZJ*014111001350304*0035*0097*UD,070318,020857,V,00.000000,N,000.000000,E,0,0,0,0,100,19,1000,50,00000000,5,255,460,0,9346,5223,42,9346,5214,21,9784,4083,13,9346,5222,11,9346,5221,8]"));

        verifyPosition(decoder, buffer(
                "[ZJ*014111001350304*0038*008a*UD,070318,021027,V,00.000000,N,000.000000,E,0,0,0,0,100,18,1000,50,00000000,4,255,460,0,9346,5223,42,9346,5214,20,9784,4083,11,9346,5221,5]"));

    }

    @Test
    public void testDecodeVoiceMessage() throws Exception {

        WatchProtocolDecoder decoder = new WatchProtocolDecoder(null);

        verifyNull(decoder.decode(null, null, buffer("[CS*1234567890*0004*TK,1]")));

        ByteBuf data = binary("7d5b5d2c2aff");

        Object decodedObject = decoder.decode(null, null, concatenateBuffers(buffer("[CS*1234567890*000e*TK,#!AMR"), data.resetReaderIndex(), buffer("]")));
        assertEquals("1234567890/mock.amr", ((Position) decodedObject).getAttributes().get("audio"));

        verifyFrame(concatenateBuffers(buffer("#!AMR"), data.resetReaderIndex()), ((MockMediaManager) Context.getMediaManager()).readFile("1234567890/mock.amr"));

    }

}
