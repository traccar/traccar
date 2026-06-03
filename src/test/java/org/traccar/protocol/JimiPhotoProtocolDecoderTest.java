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
                Content-Disposition: form-data; name="file"; filename="img.jpg"
                Content-Type: image/jpeg

                ÿØÿàfakejpeg
                --boundary
                Content-Disposition: form-data; name="filename"

                img.jpg
                --boundary
                Content-Disposition: form-data; name="timestamp"

                1737550459000
                --boundary
                Content-Disposition: form-data; name="sign"

                unverified
                --boundary
                Content-Disposition: form-data; name="callbackBody"

                {"businessType":"eventAttachment","imei":"864547000000123","camera":1,"alarmTime":1737550459,"eventType":"0C01","lat":"22.576635","lng":"113.943064","mimeType":"image/jpeg","localFileName":"img.jpg"}
                --boundary--
                """.replace("\n", "\r\n");

        verifyPosition(decoder, request(
                HttpMethod.POST, "/upload",
                new ReadOnlyHttpHeaders(true, "Content-Type", "multipart/form-data; boundary=boundary"),
                buffer(body)));
    }

}
