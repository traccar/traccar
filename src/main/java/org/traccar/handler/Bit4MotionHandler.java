package org.traccar.handler;

import jakarta.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.NetworkMessage;
import org.traccar.helper.DataConverter;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;
import org.traccar.session.cache.CacheManager;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bit4MotionHandler:
 *  - Relies on HuabaoProtocolDecoder to set Position.KEY_MOTION from bit4 (true/false).
 *  - Detects false -> true transition; if device model contains "gosafe", sends "<SPGS*CHP>" after 15s.
 *  - Fixes deficiency in the gosafe huabao implementation.
 */
public class Bit4MotionHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bit4MotionHandler.class);

    // Single-thread scheduler to handle delayed commands
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    // Attribute key to store "oldMotion" in device attributes
    private static final String ATTR_OLD_MOTION = "huabaoOldMotion";

    private final CacheManager cacheManager;
    private final ConnectionManager connectionManager;

    @Inject
    public Bit4MotionHandler(
            CacheManager cacheManager,
            ConnectionManager connectionManager) {
        this.cacheManager = cacheManager;
        this.connectionManager = connectionManager;
    }

    /**
     * Method from BasePositionHandler. Invoked for each new Position.
     */
    @Override
    public void onPosition(Position position, Callback callback) {

        long deviceId = position.getDeviceId();
        if (deviceId == 0) {
            callback.processed(false);
            return;
        }

        // Must already have KEY_MOTION set by HuabaoProtocolDecoder (bit4 check)
        if (!position.hasAttribute(Position.KEY_MOTION)) {
            callback.processed(false);
            return;
        }

        boolean currentMotion = position.getBoolean(Position.KEY_MOTION);

        // Retrieve Device from cache
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            LOGGER.warn("Device {} not found in cache", deviceId);
            callback.processed(false);
            return;
        }

        // Get oldMotion from device attributes
        Map<String, Object> attributes = device.getAttributes();
        boolean oldMotion = false;
        if (attributes.containsKey(ATTR_OLD_MOTION)) {
            Object raw = attributes.get(ATTR_OLD_MOTION);
            if (raw instanceof Boolean) {
                oldMotion = (Boolean) raw;
            } else if (raw instanceof String) {
                oldMotion = Boolean.parseBoolean((String) raw);
            }
        }

        // Check for false -> true transition
        if (!oldMotion && currentMotion) {
            // If device model includes "gosafe", schedule sending <SPGS*CHP> after 15s
            String model = device.getModel() != null ? device.getModel() : "";
            if (model.toLowerCase().contains("gosafe")) {

                LOGGER.info("Device {} has started motion; scheduling <SPGS*CHP>", deviceId);

                for (int i = 1; i <= 3; i++) {
                    int delay = i * 15;
                    SCHEDULER.schedule(() -> {
                        try {
                            sendHuabaoCommand(deviceId, "<SPGS*CHP>");
                        } catch (Exception e) {
                            LOGGER.error("Failed to send Huabao command", e);
                        }
                    }, delay, TimeUnit.SECONDS);    
                }
            }
        }

        // Persist the updated motion into memory device attributes
        attributes.put(ATTR_OLD_MOTION, currentMotion);
        device.setAttributes(attributes);

        callback.processed(false);
    }

    /**
     * Schedules command to the device.
     */
    private void sendHuabaoCommand(long deviceId, String messageText) {

        // Retrieve active session from ConnectionManager
        DeviceSession session = connectionManager.getDeviceSession(deviceId);
        if (session == null || session.getChannel() == null) {
            LOGGER.info("Device {} offline or no active channel; command not sent", deviceId);
            return;
        }

        // We can retrieve the device again (or pass it in):
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            LOGGER.warn("Cannot send command, device {} not found", deviceId);
            return;
        }

        // Get device UniqueId
        String uniqueid = device.getUniqueId();

        // Build raw Huabao command
        ByteBuf command = buildHuabaoCommand(messageText, uniqueid);

        // Write the command to the device's channel
        session.getChannel().writeAndFlush(new NetworkMessage(command, session.getChannel().remoteAddress()));

        LOGGER.info("Sent '{}' command to device {}", messageText, deviceId);
    }

    /**
     * Construct Huabao message.
     */
    private ByteBuf buildHuabaoCommand(String messageText, String imei) {

        // Convert ASCII to hex
        String hexText = asciiToHex(messageText);

        // text length (2 bytes)
        int txtLength = hexText.length() / 2;
        String txtLengthHex = String.format("%04X", txtLength);

        String header       = "7E";
        String messageId    = "8304";
        String serialNumber = "0001"; // Could be dynamic or incremented
        String markByte     = "4E";
        int packetLength    = 1 + 2 + txtLength;  // 1 + 2 + text length
        String packetLenHex = String.format("%04X", packetLength);

        // Build content
        String content = messageId + packetLenHex + imei + serialNumber
                + markByte + txtLengthHex + hexText;

        // Compute XOR-based CRC
        String crcHex = calculateXor(content);

        // Combine everything
        String finalCommand = header + content + crcHex + header;

        return Unpooled.wrappedBuffer(DataConverter.parseHex(finalCommand));
    }

    private String asciiToHex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(String.format("%02X", (int) c));
        }
        return sb.toString();
    }

    private String calculateXor(String hexData) {
        int crc = 0;
        for (int i = 0; i < hexData.length(); i += 2) {
            int b = Integer.parseInt(hexData.substring(i, i + 2), 16);
            crc ^= b;
        }
        return String.format("%02X", crc);
    }
}
