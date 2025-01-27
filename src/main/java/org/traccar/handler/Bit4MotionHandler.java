package org.traccar.handler;

import jakarta.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.NetworkMessage;
import org.traccar.helper.DataConverter;
import org.traccar.managers.DeviceManager;
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
 *  - Detects false->true transition, if device model contains "gosafe", sends "<SPGS*CHP>" after 15s.
 *  - Persists oldMotion in device attributes so it survives restarts.
 */
public class Bit4MotionHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bit4MotionHandler.class);

    // Single-thread scheduler to handle delayed commands
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    // Attribute name for storing oldMotion in device attributes (DB)
    private static final String ATTR_OLD_MOTION = "huabaoOldMotion";

    private final CacheManager cacheManager;
    private final DeviceManager deviceManager;
    private final ConnectionManager connectionManager;

    @Inject
    public Bit4MotionHandler(
            CacheManager cacheManager,
            DeviceManager deviceManager,
            ConnectionManager connectionManager) {
        this.cacheManager = cacheManager;
        this.deviceManager = deviceManager;
        this.connectionManager = connectionManager;
    }

    @Override
    protected Position handlePosition(Position position) {

        long deviceId = position.getDeviceId();
        if (deviceId == 0) {
            // No associated device
            return position;
        }

        // Must already have KEY_MOTION set by HuabaoProtocolDecoder (bit4 check)
        if (!position.hasAttribute(Position.KEY_MOTION)) {
            // No motion info from bit4
            return position;
        }

        boolean currentMotion = position.getBoolean(Position.KEY_MOTION);

        // Retrieve device from cache
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            // Device not found in cache
            return position;
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
        deviceManager.updateDevice(device);

        // Return position so it continues the pipeline
        return position;
    }

    private void sendHuabaoCommand(long deviceId, String messageText) {
        Device device = deviceManager.getById(deviceId);
        if (device == null) {
            LOGGER.warn("Device {} not found; cannot send command", deviceId);
            return;
        }
        String imei = device.getUniqueId();
        String serialNumberHex = "0001"; // or dynamic increment

        ByteBuf command = buildHuabaoCommand(messageText, imei, serialNumberHex);

        // Acquire session to write
        DeviceSession session = connectionManager.getDeviceSession(deviceId);
        if (session == null || session.getChannel() == null) {
            LOGGER.info("Device {} offline or no channel; command not sent", deviceId);
            return;
        }

        // Write raw
        session.getChannel().writeAndFlush(new NetworkMessage(command, session.getRemoteAddress()));
        LOGGER.info("Sent <SPGS*CHP> command to device {}", deviceId);
    }

    private ByteBuf buildHuabaoCommand(String messageText, String imei, String serialNumberHex) {

        // 1) ASCII -> hex
        String hexText = asciiToHex(messageText);

        // 2) txtLength (2 bytes)
        int txtLength = hexText.length() / 2;
        String txtLengthHex = String.format("%04X", txtLength);

        // 3) Hard-coded fields
        String header    = "7E";
        String messageId = "8304";
        String markByte  = "4E";
        // packetLength = 1 + 2 + txtLength
        int packetLength = 1 + 2 + txtLength;
        String packetLengthHex = String.format("%04X", packetLength);

        // 4) content
        String content = messageId + packetLengthHex + imei + serialNumberHex
                + markByte + txtLengthHex + hexText;

        // 5) XOR-based CRC
        String crcHex = calculateXor(content);

        // 6) final
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
