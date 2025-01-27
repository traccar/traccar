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
 *  - Relies on Huabao decoder to set Position.KEY_MOTION from bit4 (true/false).
 *  - Detects false->true transition; if device model contains "gosafe", sends "<SPGS*CHP>" after 15s.
 *  - Persists oldMotion in device attributes so it survives restarts.
 */
public class Bit4MotionHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bit4MotionHandler.class);

    // Single-thread scheduler to handle delayed commands
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    // Attribute name for storing oldMotion in device attributes (DB)
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
     * onPosition is the method from BasePositionHandler that you need to implement.
     * If you want this handler to continue in the pipeline, call callback.processed(false).
     * If you want to filter (stop) this position, call callback.processed(true).
     */
    @Override
    public void onPosition(Position position, Callback callback) {

        long deviceId = position.getDeviceId();
        if (deviceId == 0) {
            // No associated device
            callback.processed(false);
            return;
        }

        // Must already have KEY_MOTION set by HuabaoProtocolDecoder (bit4 check)
        if (!position.hasAttribute(Position.KEY_MOTION)) {
            // No motion info from bit4
            callback.processed(false);
            return;
        }

        boolean currentMotion = position.getBoolean(Position.KEY_MOTION);

        // Retrieve device from cache
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            // Device not found in cache
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

        // false -> true transition
        if (!oldMotion && currentMotion) {
            // Check if "gosafe" is in model
            String model = device.getModel() != null ? device.getModel() : "";
            if (model.toLowerCase().contains("gosafe")) {
                // Schedule sending "<SPGS*CHP>" command after 15 seconds
                SCHEDULER.schedule(() -> {
                    try {
                        sendHuabaoCommand(deviceId, "<SPGS*CHP>");
                    } catch (Exception e) {
                        LOGGER.error("Failed to send Huabao command", e);
                    }
                }, 15, TimeUnit.SECONDS);
            }
        }

        // Store updated motion
        attributes.put(ATTR_OLD_MOTION, currentMotion);
        device.setAttributes(attributes);

        // If you have a direct way to persist the device, do so here:
        // cacheManager.updateObject(Device.class, device);
        // or any logic your version of Traccar uses to update the DB.

        callback.processed(false);
    }

    private void sendHuabaoCommand(long deviceId, String messageText) {
        // Retrieve session from ConnectionManager
        DeviceSession session = connectionManager.getDeviceSession(deviceId);
        if (session == null || session.getChannel() == null) {
            LOGGER.info("Device {} offline or no active channel; command not sent", deviceId);
            return;
        }

        ByteBuf command = buildHuabaoCommand(messageText, session.getDeviceUniqueId());
        // Send out the raw buffer to the device
        session.getChannel().writeAndFlush(
                new NetworkMessage(command, session.getChannel().remoteAddress()));

        LOGGER.info("Sent '{}' command to device {}", messageText, deviceId);
    }

    private ByteBuf buildHuabaoCommand(String messageText, String imei) {
        // For demonstration, we build a raw Huabao-like message:
        // Adjust the structure as needed for your device protocol.

        // Convert ascii to hex
        String hexText = asciiToHex(messageText);
        // txtLength (2 bytes)
        int txtLength = hexText.length() / 2;
        String txtLengthHex = String.format("%04X", txtLength);

        // Hard-coded fields as example
        String header        = "7E";
        String messageId     = "8304";
        String markByte      = "4E";
        String serialNumber  = "0001"; // can be replaced with an incremental or unique ID
        int packetLength     = 1 + 2 + txtLength;  // 1 + 2 bytes + actual text
        String packetLenHex  = String.format("%04X", packetLength);

        // Compose content
        // messageId + length + IMEI(hex) + serial + markByte + txtLengthHex + hexText
        String content = messageId + packetLenHex + imei + serialNumber
                + markByte + txtLengthHex + hexText;

        // CRC (XOR)
        String crcHex = calculateXor(content);

        // Final
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
