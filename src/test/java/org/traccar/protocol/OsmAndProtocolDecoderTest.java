package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class OsmAndProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeJson() throws Exception {

        var decoder = inject(new OsmAndProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/", new ReadOnlyHttpHeaders(true, "Content-Type", "application/json"), buffer(
                "{\"location\":{\"timestamp\":\"2025-06-15T13:45:12.862Z\",\"coords\":{\"latitude\":37.4219983,\"longitude\":-122.084,\"accuracy\":5,\"speed\":0,\"heading\":-1,\"altitude\":5},\"is_moving\":false,\"odometer\":0,\"event\":\"motionchange\",\"battery\":{\"level\":1,\"is_charging\":false},\"activity\":{\"type\":\"still\"},\"extras\":{},\"_\":\"&id=48241179&lat=37.4219983&lon=-122.084&timestamp=2025-06-15T13:45:12.862Z&\"},\"device_id\":\"48241179\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/", new ReadOnlyHttpHeaders(true, "Content-Type", "application/json"), buffer(
                "{\"location\":{\"extras\":{},\"mock\":true,\"coords\":{\"speed_accuracy\":-1,\"speed\":-1,\"longitude\":-122.406417,\"ellipsoidal_altitude\":0,\"floor\":null,\"heading_accuracy\":-1,\"latitude\":37.785834000000001,\"accuracy\":5,\"altitude_accuracy\":-1,\"altitude\":0,\"heading\":-1},\"is_moving\":false,\"age\":188,\"odometer\":0,\"uuid\":\"2FB04C65-99CF-42AB-8DD3-EBCB4B108BF8\",\"event\":\"motionchange\",\"battery\":{\"level\":-1,\"is_charging\":false},\"activity\":{\"type\":\"unknown\",\"confidence\":100},\"timestamp\":\"2025-05-09T04:11:30.579Z\"},\"device_id\":\"658765\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/", new ReadOnlyHttpHeaders(true, "Content-Type", "application/json"), buffer(
                "{\"location\":{\"event\":\"motionchange\",\"is_moving\":false,\"uuid\":\"0e9a2473-a9a7-4c00-997b-fb97d2154e75\",\"timestamp\":\"2021-07-21T08:06:34.444Z\",\"odometer\":0,\"coords\":{\"latitude\":-6.1148096,\"longitude\":106.6837015,\"accuracy\":3.8,\"speed\":18.67,\"speed_accuracy\":0.26,\"heading\":63,\"heading_accuracy\":0.28,\"altitude\":35.7,\"altitude_accuracy\":3.8},\"activity\":{\"type\":\"still\",\"confidence\":100},\"battery\":{\"is_charging\":false,\"level\":0.79},\"extras\":{}},\"device_id\":\"8737767034\"}")));

    }

    @Test
    public void testDecodeQuery() throws Exception {

        var decoder = inject(new OsmAndProtocolDecoder(null));

        verifyNotNull(decoder, request(
                "/?id=123456&timestamp=1377177267&cell=257,02,16,2224&cell=257,02,16,2223,-90&wifi=00-14-22-01-23-45,-80&wifi=00-1C-B3-09-85-15,-70"));

        verifyNull(decoder, request(
                "/?timestamp=1377177267&lat=60.0&lon=30.0"));

        verifyPosition(decoder, request(
                "/?id=902064&lat=42.06288&lon=-88.23412&timestamp=2016-01-27T18%3A55%3A47Z&hdop=6.0&altitude=224.0&speed=0.0"));

        verifyPosition(decoder, request(
                "/?id=902064&lat=42.06288&lon=-88.23412&timestamp=1442068686579&hdop=6.0&altitude=224.0&speed=0.0"));

        verifyPosition(decoder, request(
                "/?lat=49.60688&lon=6.15788&timestamp=2014-06-04+09%3A10%3A11&altitude=384.7&speed=0.0&id=353861053849681"));
        
        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0&speed=0.0&bearing=0.0&altitude=0&hdop=0.0"));
        
        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&lat=60.0&lon=30.0"));
        
        verifyPosition(decoder, request(
                "/?lat=60.0&lon=30.0&speed=0.0&heading=0.0&vacc=0&hacc=0&altitude=0&deviceid=123456"));

        verifyPosition(decoder, request(
                "/?id=861001000719969&lat=41.666667&lon=-0.883333&altitude=350.059479&speed=0.000000&batt=87"));

        verifyPosition(decoder, request(
                "/?id=123456&timestamp=1377177267&location=60.0,30.0"));

        verifyPosition(decoder, request(
                "/?id=123456789012345&timestamp=1504763810&lat=40.7232948571&lon=-74.0061408571&bearing=7.19889788244&speed=40&ignition=true&rpm=933&fuel=24"));

    }

}
