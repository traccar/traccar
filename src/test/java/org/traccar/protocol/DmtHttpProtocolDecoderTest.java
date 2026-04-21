package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class DmtHttpProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new DmtHttpProtocolDecoder(null));

        verifyAttributes(decoder, request(HttpMethod.POST, "/",
                buffer("{\"date\":\"2021-12-12T16:04:52Z\",\"device\":{\"sn\":\"416581\",\"prod\":85,\"rev\":1,\"fw\":\"1.12\",\"iccid\":\"89011702278612797427\",\"imei\":\"351358810439486\"},\"sqn\":1549,\"reason\":42,\"counters\":[{\"id\":0,\"val\":5304},{\"id\":3,\"val\":3200},{\"id\":4,\"val\":5066},{\"id\":128,\"val\":1},{\"id\":129,\"val\":8},{\"id\":130,\"val\":0},{\"id\":131,\"val\":0},{\"id\":132,\"val\":0},{\"id\":134,\"val\":1},{\"id\":138,\"val\":0},{\"id\":139,\"val\":36},{\"id\":142,\"val\":1629023},{\"id\":145,\"val\":0},{\"id\":146,\"val\":1}]}")));

        verifyAttributes(decoder, request(HttpMethod.POST, "/",
                buffer("{\"date\":\"2021-10-04T18:15:47Z\",\"device\":{\"sn\":\"403809\",\"prod\":85,\"rev\":1,\"fw\":\"1.12\",\"iccid\":\"89011702278483601922\",\"imei\":\"352656106127312\"},\"sqn\":40927,\"reason\":11,\"analogues\":[{\"id\":1,\"val\":4265},{\"id\":3,\"val\":3800},{\"id\":4,\"val\":12},{\"id\":5,\"val\":4251}],\"inputs\":1,\"outputs\":0,\"status\":137}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"date\":\"2021-10-04T18:14:55Z\",\"device\":{\"sn\":\"403809\",\"prod\":85,\"rev\":1,\"fw\":\"1.12\",\"iccid\":\"89011702278483601922\",\"imei\":\"352656106127312\"},\"sqn\":40925,\"reason\":1,\"lat\":26.87366,\"lng\":-80.10618,\"posAcc\":47.7,\"posInfo\":{\"GDOP\":4.68,\"BSat\":2,\"GSat\":4,\"Src\":1},\"analogues\":[{\"id\":1,\"val\":4265},{\"id\":3,\"val\":3800},{\"id\":4,\"val\":16},{\"id\":5,\"val\":4255}],\"inputs\":1,\"outputs\":0,\"status\":137}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{ \"date\": \"2021-04-20T11:10:03.702659861Z\", \"device\":{ \"sn\": \"0016C001F000ABEC\", \"prod\": 0.2, \"rev\": 0.3, \"fw\": \"1.1\", \"module\": \"LR 34.3.3\", \"iccid\": \"89610180000000000000\", \"imei\": \"354043000000000\" }, \"sqn\": 347263802, \"reason\":3, \"lat\": 1.1, \"lng\": 2.2, \"posAcc\": 30.1, \"posInfo\":{ \"HDOP\": 0.1, \"PDOP\": 0.2, \"GDOP\": 0.3, \"BSat\":1, \"GSat\":2, \"Src\":2 }, \"analogues\":[{ \"id\":1, \"val\": 300 },{ \"id\":2, \"val\": 500} ], \"inputs\": 5001, \"outputs\":0, \"status\": 17, \"counters\":[{ \"id\": 11, \"val\": 43 },{ \"id\": 23, \"val\": 8800} ], \"lora\":{ \"dev_id\": \"yabby-abec\", \"app_id\": \"digital-matter\", \"dev_addr\": \"260B567A\", \"gw\": [ { \"id\": \"dm-sentrius\", \"snr\": 10, \"rssi\": -36 } ] }}")));

        verifyPositions(decoder, request(HttpMethod.POST, "/",
                buffer("{\"SerNo\":131693,\"IMEI\":\"356692063643328\",\"ICCID\":\"8944538523010771676\",\"ProdId\":33,\"FW\":\"33.4.1.27\",\"Records\":[{\"SeqNo\":125,\"Reason\":11,\"DateUTC\":\"2017-05-11 05:58:44\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":2,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14641,\"3\":2484,\"4\":26,\"5\":10868},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":0,\"13\":309,\"14\":9921,\"15\":3},\"FType\":7}]},{\"SeqNo\":128,\"Reason\":11,\"DateUTC\":\"2017-05-11 17:59:45\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":2,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14607,\"3\":2752,\"4\":26,\"5\":11062},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":1,\"13\":325,\"14\":10881,\"15\":3},\"FType\":7}]},{\"SeqNo\":130,\"Reason\":9,\"DateUTC\":\"2017-05-11 19:30:03\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":3,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14599,\"3\":2731,\"4\":27,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":2,\"13\":329,\"14\":11121,\"15\":3},\"FType\":7}]},{\"SeqNo\":131,\"Reason\":11,\"DateUTC\":\"2017-05-11 19:32:03\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14403,\"3\":2783,\"4\":27,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":2,\"13\":330,\"14\":11181,\"15\":3},\"FType\":7}]},{\"SeqNo\":133,\"Reason\":11,\"DateUTC\":\"2017-05-11 19:36:15\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14319,\"3\":2898,\"4\":23,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":3,\"13\":331,\"14\":11241,\"15\":3},\"FType\":7}]}]}")));

    }

}
