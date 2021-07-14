package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class DmtHttpProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new DmtHttpProtocolDecoder(null);

        verifyPositions(decoder, request(HttpMethod.POST, "/",
                buffer("{\"SerNo\":131693,\"IMEI\":\"356692063643328\",\"ICCID\":\"8944538523010771676\",\"ProdId\":33,\"FW\":\"33.4.1.27\",\"Records\":[{\"SeqNo\":125,\"Reason\":11,\"DateUTC\":\"2017-05-11 05:58:44\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":2,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14641,\"3\":2484,\"4\":26,\"5\":10868},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":0,\"13\":309,\"14\":9921,\"15\":3},\"FType\":7}]},{\"SeqNo\":128,\"Reason\":11,\"DateUTC\":\"2017-05-11 17:59:45\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":2,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14607,\"3\":2752,\"4\":26,\"5\":11062},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":1,\"13\":325,\"14\":10881,\"15\":3},\"FType\":7}]},{\"SeqNo\":130,\"Reason\":9,\"DateUTC\":\"2017-05-11 19:30:03\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":3,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14599,\"3\":2731,\"4\":27,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":2,\"13\":329,\"14\":11121,\"15\":3},\"FType\":7}]},{\"SeqNo\":131,\"Reason\":11,\"DateUTC\":\"2017-05-11 19:32:03\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14403,\"3\":2783,\"4\":27,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":2,\"13\":330,\"14\":11181,\"15\":3},\"FType\":7}]},{\"SeqNo\":133,\"Reason\":11,\"DateUTC\":\"2017-05-11 19:36:15\",\"Fields\":[{\"GpsUTC\":\"2017-05-08 18:04:57\",\"Lat\":43.7370138,\"Long\":-79.3462607,\"Alt\":197,\"Spd\":0,\"SpdAcc\":13,\"Head\":66,\"PDOP\":18,\"PosAcc\":37,\"GpsStat\":7,\"FType\":0},{\"DIn\":6,\"DOut\":0,\"DevStat\":2,\"FType\":2},{\"AnalogueData\":{\"1\":14319,\"3\":2898,\"4\":23,\"5\":10965},\"FType\":6},{\"AnalogueData\":{\"11\":34,\"12\":3,\"13\":331,\"14\":11241,\"15\":3},\"FType\":7}]}]}")));

    }

}
