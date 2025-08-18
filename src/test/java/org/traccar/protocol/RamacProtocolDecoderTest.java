package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class RamacProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new RamacProtocolDecoder(null));

        verifyAttributes(decoder, request(HttpMethod.POST, "/",
                buffer("{\"PacketType\": 0,\"SeqNumber\": 4,\"UpdateDate\": \"2022-05-06 12:25:35\",\"Alert\": 42,\"AlertMessage\": \"Low Battery\",\"Mode\": 1,\"ModeText\": \"Help Me\",\"SigfoxTXInterval\": 2,\"GpsFixInterval\": 3,\"SigfoxTXIntervalText\": \"2 Seconds\",\"GpsFixIntervalText\": \"3 Seconds\",\"BatteryPercentage\": 4,\"Battery\": 0.1,\"Temperature\": -22,\"HwVersion\": 7,\"FirmwareVersion\": 8,\"DeviceId\": \"A10001\",\"DeviceType\": \"12\",\"DeviceTypeText\": \"RAMAC P1\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"PacketType\": 1,\"SeqNumber\": 4,\"UpdateDate\": \"2022-05-06 12:25:35\",\"Alert\": 0,\"AlertMessage\": \"\",\"Latitude\": -25.87586735939189,\"Longitude\": 28.179579268668846,\"Speed\": 1,\"COG\": 3,\"EstimatedAccuracy\": 3,\"LastLocation\": 0,\"LastLocationText\": \"NEW LOCATION\",\"IsMoving\": 0,\"IsMovingText\": \"STATIONARY\",\"GpsEvent\": 5,\"GpsEventText\": \"Heartbeat\",\"DeviceId\": \"A10001\",\"DeviceType\": \"12\",\"DeviceTypeText\": \"RAMAC P1\"}")));

        verifyPosition(decoder, request(HttpMethod.POST, "/",
                buffer("{\"PacketType\": 2,\"SeqNumber\": 4,\"UpdateDate\": \"2022-05-06 12:25:35\",\"Alert\": 19,\"AlertMessage\": \"P1 Panic\",\"Event\": 16,\"DeviceId\": \"A10001\",\"DeviceType\": \"12\",\"DeviceTypeText\": \"RAMAC P1\",\"Latitude\": -25.875867359392,\"Longitude\": 28.179579268669,\"LocationDateTime\": \"2022-05-05 08:48:11\"}")));

    }

}
