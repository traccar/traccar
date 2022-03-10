package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TeraTrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new TeraTrackProtocolDecoder(null);

        verifyAttributes(decoder, text(
                "{\"MDeviceID\":\"022043756090\",\"DiviceType\":\"1\",\"DataType\":\"1\",\"DataLength\":\"69\",\"DateTime\":\"2022-03-09 10:56:01\",\"Latitude\":\"-6.846451\",\"Longitude\":\"39.316324\",\"LongitudeState\":\"1\",\"LatitudeState\":\"0\",\"Speed\":\"90\",\"Mileage\":\"0\",\"FenceAlarm\":\"0\",\"AreaAlarmID\":\"0\",\"LockCutOff\":\"0\",\"SealTampered\":\"0\",\"MessageAck\":\"1\",\"LockRope\":\"1\",\"LockStatus\":\"1\",\"LockOpen\":\"0\",\"PasswordError\":\"0\",\"CardNo\":\"60000644\",\"IllegalCard\":\"0\",\"LowPower\":\"0\",\"UnCoverBack\":\"0\",\"CoverStatus\":\"1\",\"LockStuck\":\"0\",\"Power\":\"79\",\"GSM\":\"16\",\"IMEI\":\"860922043756090\",\"Index\":\"20\",\"Slave\":[]}"));

        verifyAttributes(decoder, text(
                "{\"MDeviceID\":\"074054558620\",\"DeviceType\":\"1\",\"DataType\":\"2\",\"DataLength\":\"0913\",\"DateTime\":\"2022-02-22 23:35:35\",\"Latitude\":\"-6.826699\",\"Longitude\":\"39.279008\",\"LatitudeState\":\"0\",\"LongitudeState\":\"1\",\"Speed\":\"0\",\"Mileage\":\"0\",\"FenceAlarm\":\"0\",\"AreaAlarmID\":\"0\",\"LockCutOff\":\"0\",\"SealTempered\":\"1\",\"MessageAck\":\"1\",\"LockRope\":\"0\",\"LockStatus\":\"0\",\"LockOpen\":\"1\",\"PasswordError\":\"0\",\"CardNo\":\"60060198\",\"IllegalCard\":\"0\",\"LowPower\":\"0\",\"UnCoverBack\":\"1\",\"CoverStatus\":\"0\",\"LockStuck\":\"1\",\"Power\":\"90\",\"GSM\":\"14\",\"IMEI\":\"861774054558620\",\"Index\":\"39\",\"Slave\":[{\"SDeviceId\":\"685304\",\"SPower\":\"00\",\"SLockCutOff\":\"0\",\"SLockOpen\":\"1\",\"SUnCoverBack\":\"0\",\"SCoverStatus\":\"1\",\"STimeOut\":\"1\",\"SLockRope\":\"0\",\"SSealTempered\":\"0\",\"SLockStuck\":\"0\"},{\"SDeviceId\":\"224779\",\"SPower\":\"00\",\"SLockCutOff\":\"0\",\"SLockOpen\":\"1\",\"SUnCoverBack\":\"0\",\"SCoverStatus\":\"1\",\"STimeOut\":\"1\",\"SLockRope\":\"0\",\"SSealTempered\":\"0\",\"SLockStuck\":\"0\"}]}"));

    }

}
