package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class RfTrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RfTrackProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/deviceDataUpload.do",
                buffer("gsm={\"n\":0,\"b\":[{\"l\":6166,\"b\":19,\"c\":21423},{\"l\":6166,\"b\":18,\"c\":21416},{\"l\":6166,\"b\":17,\"c\":21383},{\"l\":6166,\"b\":13,\"c\":21422},{\"l\":6166,\"b\":13,\"c\":21435},{\"l\":6169,\"b\":11,\"c\":21311}],\"c\":460}&wifi=[{\"l\":-49,\"t\":\"lianqin20\",\"m\":\"30-B4-9E-DD-F8-2D\"}]&mt=1073709094&i=358477047125172&gps={\"a\":30.0,\"y\":31.251563,\"s\":4,\"t\":1589764496654,\"z\":0.0,\"x\":121.360346}&dbm=-53&td=1589720501123&rc=0&bar=1001.85065&u_ids=[8441644.1,53036.1]&t=1589764500713&bat=12380&v=T2.142.2_2.0_R03&i_ids=[4247328.1,53036.1,10522408.1]&idt=140736414711817&id=39163")));

    }

}
