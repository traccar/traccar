package org.traccar.protocol;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FlespiProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FlespiProtocolDecoder(null));

        verifyPositions(decoder, request(HttpMethod.POST, "/",
            buffer("[{\"position.speed\":0,\"position.latitude\":53.90573,\"time.valid.status\":true,\"timestamp\":1506956075,\"position.satellites\":10,\"message.buffered.status\":false,\"business.mode.status\":true,\"gps.status\":true,\"position.longitude\":27.455848,\"position.direction\":0,\"ident\":\"605630\"},{\"siren.status\":false,\"business.mode.status\":true,\"position.satellites\":8,\"timestamp\":1506695785,\"led.status\":false,\"position.latitude\":53.905569,\"position.longitude\":27.455986,\"position.speed\":0,\"gradual.stop.status\":false,\"position.direction\":262.643854,\"hardware.version.enum\":223,\"vehicle.mileage\":160,\"message.buffered.status\":false,\"blinkers.status\":false,\"ident\":\"605630\",\"position.altitude\":233.48,\"immobilizer.status\":false}]")));

        verifyPositions(decoder, request(HttpMethod.POST, "/",
            buffer("[{\"geofence.inside.status\":false,\"position.valid\":false,\"ain#4\":0,\"rs232.sensor.value#1\":0,\"position.direction\":0,\"rs232.sensor.value#0\":0,\"position.speed\":0,\"position.latitude\":10.11223,\"refrigerator.sensor.temperature#1\":62.5,\"gnss.antenna.cut.status\":true,\"din\":3,\"ain#3\":0,\"refrigerator.sensor.temperature#3\":71.4,\"position.altitude\":0,\"shock.event.trigger\":false,\"alarm.mode.status\":false,\"ibutton.event.connect\":false,\"refrigerator.sensor.temperature#4\":66.7,\"internal.battery.voltage.limit.lower.status\":false,\"ain#2\":0,\"gsm.signal.level\":0,\"refrigerator.connection.status\":0,\"position.satellites\":0,\"external.powersource.voltage.range.outside.status\":false,\"refrigerator.sensor.temperature#2\":68.2,\"incline.event.trigger\":false,\"alarm.event.trigger\":false,\"movement.status\":true,\"refrigerator.sensor.temperature#6\":68.9,\"ident\":\"605630\",\"timestamp\":946684840,\"engine.ignition.status\":true,\"gsm.sim.status\":true,\"record.seqnum\":8165,\"external.powersource.voltage\":15.298,\"gnss.enum\":\"glonass\",\"position.longitude\":20.88774,\"battery.voltage\":4.088,\"refrigerator.sensor.temperature#5\":71.3,\"ain#1\":0,\"internal.bus.supply.voltage.range.outside.status\":false},{\"geofence.inside.status\":false,\"position.valid\":true,\"ain#4\":0,\"rs232.sensor.value#1\":0,\"position.direction\":0,\"rs232.sensor.value#0\":0,\"position.speed\":0,\"position.latitude\":57.986744,\"refrigerator.sensor.temperature#1\":74.1,\"gnss.antenna.cut.status\":false,\"ain#3\":0,\"position.hdop\":21.1,\"refrigerator.sensor.temperature#3\":71.4,\"position.altitude\":219,\"shock.event.trigger\":false,\"alarm.mode.status\":false,\"ibutton.event.connect\":false,\"refrigerator.sensor.temperature#4\":70.5,\"internal.battery.voltage.limit.lower.status\":false,\"ain#2\":0,\"gsm.signal.level\":0,\"refrigerator.connection.status\":0,\"position.satellites\":5,\"external.powersource.voltage.range.outside.status\":false,\"refrigerator.sensor.temperature#2\":71.3,\"incline.event.trigger\":false,\"alarm.event.trigger\":false,\"movement.status\":true,\"refrigerator.sensor.temperature#6\":69.3,\"ident\":\"605630\",\"timestamp\":1392272112,\"engine.ignition.status\":true,\"gsm.sim.status\":true,\"record.seqnum\":8174,\"external.powersource.voltage\":15.303,\"gnss.enum\":\"glonass\",\"position.longitude\":56.207576,\"battery.voltage\":3.934,\"refrigerator.sensor.temperature#5\":68.1,\"ain#1\":0,\"internal.bus.supply.voltage.range.outside.status\":false}]")));

        verifyPositions(decoder, request(HttpMethod.POST, "/",
            buffer("[{\"ain#1\":1,\"ain#2\":0,\"ain#3\":0,\"ain#4\":0,\"alarm.event.trigger\":true,\"custom.SOS\":1,\"custom.dparam\":3.141593,\"custom.ign\":1,\"custom.iparam\":-55,\"custom.tparam\":\"lorem\",\"din\":722,\"dout\":1048576,\"ident\":\"namo:namo\",\"position.altitude\":300,\"position.direction\":0,\"position.hdop\":1.1,\"position.latitude\":53.90821,\"position.longitude\":27.524165,\"position.satellites\":7,\"position.speed\":0,\"timestamp\":1508508510.013227}]")));
    }

}