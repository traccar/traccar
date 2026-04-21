package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class DraginoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new DraginoProtocolDecoder(null));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"end_device_ids\":{\"device_id\":\"eui-a840412b81874509\",\"application_ids\":{\"application_id\":\"teast12344321\"},\"dev_eui\":\"A840412B81874509\",\"join_eui\":\"A840410000000102\",\"dev_addr\":\"260BB59A\"},\"correlation_ids\":[\"gs:uplink:01HDTGK4QWSRX0AMP8E6R1R809\"],\"received_at\":\"2023-10-28T06:47:23.853335601Z\",\"uplink_message\":{\"session_key_id\":\"AYt1CUoFjSPmZ+/Hf8bbng==\",\"f_port\":2,\"f_cnt\":1,\"frm_payload\":\"AAAAAAAAAAAPoiACpwEQ\",\"decoded_payload\":{\"ALARM_status\":\"FALSE\",\"BatV\":4.002,\"Hum\":67.9,\"LON\":\"ON\",\"Latitude\":0,\"Location\":0,\"Longitude\":0,\"MD\":0,\"Tem\":27.2},\"rx_metadata\":[{\"gateway_ids\":{\"gateway_id\":\"eui-a840411b7c4c4150\",\"eui\":\"A840411B7C4C4150\"},\"time\":\"2023-10-28T06:47:23.522309Z\",\"timestamp\":2618357987,\"rssi\":-101,\"channel_rssi\":-101,\"snr\":8.5,\"uplink_token\":\"CiIKIAoUZXVpLWE4NDA0MTFiN2M0YzQxNTASCKhAQRt8TEFQEOPxw+AJGgwI+9zyqQYQ+LGJswIguO2wkpqnNg==\",\"received_at\":\"2023-10-28T06:47:23.643979512Z\"},{\"gateway_ids\":{\"gateway_id\":\"test123123\",\"eui\":\"A840411D178C4150\"},\"time\":\"2023-10-28T06:47:23.522842Z\",\"timestamp\":3064971227,\"rssi\":-78,\"channel_rssi\":-78,\"snr\":8.8,\"uplink_token\":\"ChgKFgoKdGVzdDEyMzEyMxIIqEBBHReMQVAQ2/++tQsaDAj73PKpBhDyj+e1AiD43pX0ma44\",\"received_at\":\"2023-10-28T06:47:23.649709554Z\"},{\"gateway_ids\":{\"gateway_id\":\"eui-b8ea26fdfe2d5d28\",\"eui\":\"B8EA26FDFE2D5D28\"},\"time\":\"2023-10-28T06:47:23.536703Z\",\"timestamp\":2967400725,\"rssi\":-38,\"channel_rssi\":-38,\"snr\":14.2,\"frequency_offset\":\"1061\",\"uplink_token\":\"CiIKIAoUZXVpLWI4ZWEyNmZkZmUyZDVkMjgSCLjqJv3+LV0oEJXi+4YLGgwI+9zyqQYQxqentwIgiPT2tq60NQ==\",\"received_at\":\"2023-10-28T06:47:23.532394512Z\"},{\"gateway_ids\":{\"gateway_id\":\"eu868-1\",\"eui\":\"A840411B7E5E1868\"},\"time\":\"2023-10-28T06:47:23.526195Z\",\"timestamp\":3486064627,\"rssi\":-35,\"channel_rssi\":-35,\"snr\":9,\"uplink_token\":\"ChUKEwoHZXU4NjgtMRIIqEBBG35eGGgQ87+k/gwaDAj73PKpBhDZzuW4AiC4mpPNusM1\",\"received_at\":\"2023-10-28T06:47:23.537018603Z\"}],\"settings\":{\"data_rate\":{\"lora\":{\"bandwidth\":125000,\"spreading_factor\":7,\"coding_rate\":\"4/5\"}},\"frequency\":\"868100000\",\"timestamp\":2618357987,\"time\":\"2023-10-28T06:47:23.522309Z\"},\"received_at\":\"2023-10-28T06:47:23.644805734Z\",\"consumed_airtime\":\"0.066816s\",\"network_ids\":{\"net_id\":\"000013\",\"ns_id\":\"EC656E0000000181\",\"tenant_id\":\"ttn\",\"cluster_id\":\"eu1\",\"cluster_address\":\"eu1.cloud.thethings.network\"}}}")));

    }

}
