package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TtnHttpProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeJson() throws Exception {

        var decoder = inject(new TtnHttpProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/", new ReadOnlyHttpHeaders(true, "Content-Type", "application/json"), buffer(
                "{\"end_device_ids\":{\"device_id\":\"test-device\",\"application_ids\":{\"application_id\":\"test-app\"},\"dev_eui\":\"0000000000000000\",\"dev_addr\":\"00000000\"},\"received_at\":\"2025-11-12T08:29:01.954236764Z\",\"uplink_message\":{\"f_port\":1,\"f_cnt\":15,\"frm_payload\":\"AIVpAyHNAIgFtcvtXxgAEWIAAAsAAgBaAIQAYwCCAAB7tQBnAS0AcyeRAGh5\",\"decoded_payload\":{\"altitude\":44.5,\"barometer_0\":1012.9,\"hdop\":0.9,\"heading\":99,\"humidity_0\":60.5,\"latitude\":37.4219,\"longitude\":-122.084,\"sats\":11,\"speed\":31.669,\"temperature_0\":30.1,\"time\":1762936141},\"rx_metadata\":[{\"gateway_ids\":{\"gateway_id\":\"test-gateway\",\"eui\":\"0000000000000000\"},\"time\":\"2025-11-12T08:29:01.553412Z\",\"rssi\":-58,\"channel_rssi\":-58,\"snr\":12,\"location\":{\"latitude\":37.4219,\"longitude\":-122.084,\"altitude\":47,\"source\":\"SOURCE_REGISTRY\"},\"received_at\":\"2025-11-12T08:23:02.467711084Z\"}],\"settings\":{\"data_rate\":{\"lora\":{\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\"}},\"frequency\":\"916800000\"},\"received_at\":\"2025-11-12T08:29:01.749842730Z\",\"consumed_airtime\":\"0.112896s\",\"packet_error_rate\":0.7777778,\"locations\":{\"frm-payload\":{\"latitude\":37.4219,\"longitude\":-122.084,\"altitude\":44,\"source\":\"SOURCE_GPS\"}},\"network_ids\":{\"net_id\":\"000013\",\"ns_id\":\"EC656E0000000183\",\"tenant_id\":\"ttn\",\"cluster_id\":\"au1\",\"cluster_address\":\"au1.cloud.thethings.network\"}}}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/", new ReadOnlyHttpHeaders(true, "Content-Type", "application/json"), buffer(
                "{\"end_device_ids\":{\"device_id\":\"test-device\",\"application_ids\":{\"application_id\":\"test-app\"},\"dev_eui\":\"0000000000000000\",\"dev_addr\":\"00000000\"},\"received_at\":\"2025-11-12T08:29:01.954236764Z\",\"uplink_message\":{\"f_port\":1,\"f_cnt\":15,\"frm_payload\":\"AIgFxALtUoAAEWIAZwEQAWcA/w==\",\"decoded_payload\":{\"gps_0\":{\"altitude\": 44.5,\"latitude\": 37.7858,\"longitude\": -122.4064},\"temperature_0\": 27.2,\"temperature_1\":25.5},\"rx_metadata\":[{\"gateway_ids\":{\"gateway_id\":\"test-gateway\",\"eui\":\"0000000000000000\"},\"time\":\"2025-11-12T08:29:01.553412Z\",\"rssi\":-58,\"channel_rssi\":-58,\"snr\":12,\"received_at\":\"2025-11-12T08:23:02.467711084Z\"}],\"settings\":{\"data_rate\":{\"lora\":{\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\"}},\"frequency\":\"916800000\"},\"received_at\":\"2025-11-12T08:29:01.749842730Z\",\"consumed_airtime\":\"0.112896s\",\"packet_error_rate\":0.7777778,\"locations\":{\"frm-payload\":{\"latitude\":37.7858,\"longitude\":-122.4064,\"altitude\":44,\"source\":\"SOURCE_GPS\"}},\"network_ids\":{\"net_id\":\"000013\",\"ns_id\":\"EC656E0000000183\",\"tenant_id\":\"ttn\",\"cluster_id\":\"au1\",\"cluster_address\":\"au1.cloud.thethings.network\"}}}")));

    }

}
