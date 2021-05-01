package org.traccar.protocol;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import io.netty.channel.Channel;

public class Mavlink2ProtocolDecoder extends BaseProtocolDecoder {

    public Mavlink2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object pkt) throws Exception {
        byte[] packet = (byte[]) pkt;
        /*
         * 0 uint8_t magic Packet start marker 0xFD Protocol-specific start-of-text
         * (STX) marker used to indicate the beginning of a new packet. Any system that
         * does not understand protocol version will skip the packet.
         * *
         * 1 uint8_t len Payload length 0 - 255 Indicates length of the following
         * payload section. This may be affected by payload truncation.
         * *
         * 2 uint8_t incompat_flags Incompatibility Flags Flags that must be understood
         * for MAVLink compatibility (implementation discards packet if it does not
         * understand flag).
         * *
         * 3 uint8_t compat_flags Compatibility Flags Flags that can be ignored if not
         * understood (implementation can still handle packet even if it does not
         * understand flag).
         * *
         * 4 uint8_t seq Packet sequence number 0 - 255 Used to detect packet loss.
         * Components increment value for each message sent.
         * *
         * 5 uint8_t sysid System ID (sender) 1 - 255 ID of system (vehicle) sending the
         * message. Used to differentiate systems on network. Note that the broadcast
         * address 0 may not be used in this field as it is an invalid source address.
         * *
         * 6 uint8_t compid Component ID (sender) 1 - 255 ID of component sending the
         * message. Used to differentiate components in a system (e.g. autopilot and a
         * camera). Use appropriate values in MAV_COMPONENT. Note that the broadcast
         * address MAV_COMP_ID_ALL may not be used in this field as it is an invalid
         * source address.
         * *
         * 7 to 9 uint32_t msgid:24 Message ID (low, middle, high bytes) 0 - 16777215 ID
         * of message type in payload. Used to decode data back into message object.
         * *
         * 10 to (n+10) uint8_t payload[max 255] Payload Message data. Depends on
         * message type (i.e. Message ID) and contents.
         * *
         * (n+10) to (n+11) uint16_t checksum Checksum (low byte, high byte)
         * CRC-16/MCRF4XX for message (excluding magic byte). Includes CRC_EXTRA byte.
         * *
         * (n+12) to (n+25) uint8_t signature[13] Signature (Optional) Signature to
         * ensure the link is tamper-proof.
         */

        if (packet[0] != (byte) 0xfd) {
            return null;
        }

        int msgid = (packet[7] & 0xff) | ((packet[8] & 0xff) << 8) | ((packet[9] & 0x0f) << 16);
        int len = packet[1] & 0xff;
        int sysid = packet[5] & 0xff;
        // GLOBAL_POSITION_INT ( #33 )
        if (msgid == 33) {
            byte[] message = Arrays.copyOfRange(packet, 10, 10 + len);
            /*
             * time_boot_ms uint32_t ms Timestamp (time since system boot).
             * *
             * lat int32_t degE7 Latitude, expressed
             * *
             * lon int32_t degE7 Longitude, expressed
             * *
             * alt int32_t mm Altitude (MSL). Note that virtually all GPS modules provide
             * both WGS84 and MSL.
             * *
             * relative_alt int32_t mm Altitude above ground
             * *
             * vx int16_t cm/s Ground X Speed (Latitude, positive north)
             * *
             * vy int16_t cm/s Ground Y Speed (Longitude, positive east)
             * *
             * vz int16_t cm/s Ground Z Speed (Altitude, positive down)
             * *
             * hdg uint16_t cdeg Vehicle heading (yaw angle), 0.0..359.99 degrees. If
             * unknown, set to: UINT16_MAX
             */

            // int timeBootms = ByteBuffer.wrap(message, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int lat = ByteBuffer.wrap(message, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int lon = ByteBuffer.wrap(message, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int alt = ByteBuffer.wrap(message, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int vx = ByteBuffer.wrap(message, 20, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            int vy = ByteBuffer.wrap(message, 22, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            int hdg = ByteBuffer.wrap(message, 26, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, Integer.toString(sysid));
            if (deviceSession == null) {
                return null;
            }
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            position.setValid(true);
            // position.setTime(Date.from(Instant.ofEpochMilli(timeBootms)));
            position.setTime(Date.from(Instant.now()));
            position.setLatitude(lat / 1e7);
            position.setLongitude(lon / 1e7);
            position.setAltitude(alt / 1e3);
            double speed = Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2));
            // cm/s to kn
            position.setSpeed(speed * 0.01943844);
            position.setCourse(hdg / 1e2);
            return position;
        }
        return null;
    }

}
