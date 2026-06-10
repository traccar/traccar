package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class JimiPhotoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new JimiPhotoProtocolDecoder(null));

        String body = """
                --boundary
                Content-Disposition: form-data; name="file"; filename="ALDW_20260610_203521_CH1_001_000000.jpg"
                Content-Type: image/jpeg

                ÿØÿàfakejpeg
                --boundary
                Content-Disposition: form-data; name="filename"

                ALDW_20260610_203521_CH1_001_000000.jpg
                --boundary
                Content-Disposition: form-data; name="timestamp"

                1781089614000
                --boundary
                Content-Disposition: form-data; name="sign"

                unverified
                --boundary
                Content-Disposition: form-data; name="callbackBody"

                {"businessType":"eventAttachment","imei":"865478070305877","camera":1,"alarmTime":1781089614,"lat":"-27.263320","lng":"153.037503","localFileName":"865478070305877_30333035383737260610203521030500_1_00.jpg","mimeType":"image/jpeg","eventType":null,"videoBeginTime":null,"videoEndTime":null,"timezone":"GMT+10:00","instructionId":"30333035383737260610203521030500"}
                --boundary--
                """.replace("\n", "\r\n");

        verifyPosition(decoder, request(
                HttpMethod.POST, "/upload",
                new ReadOnlyHttpHeaders(true, "Content-Type", "multipart/form-data; boundary=boundary"),
                buffer(body)));
    }

}
