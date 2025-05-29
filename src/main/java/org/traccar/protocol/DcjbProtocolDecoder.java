/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2025 Gerrit Maus (funk@maus.xyz)
 * Copyright 2025 Christina Maus (minnie@maus.xyz)
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.traccar.BaseMqttProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decoder for DCJB MQTT packets.
 */
public class DcjbProtocolDecoder extends BaseMqttProtocolDecoder {

    public DcjbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Position parseInstanceGnss(Position position, String name, JsonNode instanceValues) throws Exception {

        JsonNode instanceCoords = instanceValues.get("coords");
        if (instanceCoords == null) {
            throw new Exception("Obligatory coords field does not exist.");
        }

        double latdeg = instanceCoords.get("latdeg").asDouble();
        double londeg = instanceCoords.get("londeg").asDouble();

        position.setValid(true);
        position.setLatitude(latdeg);
        position.setLongitude(londeg);

        //continue decoding other GNSS fields when protocol definition is extended
        return position;
    }

    private Position parseDataBlock(DeviceSession deviceSession, int[] version, Date datetime, JsonNode dataBlock)
            throws Exception {

        Position position = new Position(getProtocolName());

        /*
         * The UID is ignored here, since the device management is done by Traccar
         * on a higher level. We have to adhere to that so far!
         */
        position.setDeviceId(deviceSession.getDeviceId());

        /* Datetime */
        JsonNode blockDatetime = dataBlock.get("datetime");
        if (blockDatetime == null || blockDatetime.isNull()) {
            // Inherit the datetime from the global datetime.
            position.setTime(datetime);
        } else {
            position.setTime(Date.from(Instant.parse(blockDatetime.asText())));
        }

        /* Instances */
        JsonNode blockInstances = dataBlock.get("instances");
        if (blockInstances == null || !blockInstances.isArray()) {
            throw new Exception("Invalid instances!");
        }

        for (JsonNode blockInstance : blockInstances) {

            // The datetime is ignored, since only a single time can be set for each
            // position in the Traccar system.

            /* Name */
            JsonNode instanceName = blockInstance.get("name");
            String name = instanceName == null || instanceName.isNull() ? "" : instanceName.asText();

            /* Type */
            JsonNode instanceType = blockInstance.get("type");
            if (instanceType == null) {
                throw new Exception("Obligatory type field does not exist.");
            }
            String type = instanceType.asText();

            /* Values */
            JsonNode instanceValues = blockInstance.get("values");
            if (instanceValues == null) {
                throw new Exception("Obligatory values field does not exist.");
            }

            switch (type) {
                case "gnss":
                    position = parseInstanceGnss(position, name, instanceValues);
                    break;
                default:
                    throw new Exception("Unknown type.");
            }
        }

        return position;
    }

    @Override
    protected Object decode(DeviceSession deviceSession, MqttPublishMessage message) throws Exception {

        List<Position> positions = new LinkedList<>();

        /*
         * The full message, either plain JSON or gzipped JSON (to be implemented).
         */
        ByteBuf buf = message.payload();
        String msgStr = buf.toString(StandardCharsets.UTF_8);

        /* Parse JSON. */
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);

        JsonNode packetBody;
        try {
            packetBody = objectMapper.readTree(msgStr);
        } catch (Exception e) {
            throw new Exception("Invalid JSON.", e);
        }

        /* Check version. */
        JsonNode packetVersion = packetBody.get("version");
        if (packetVersion == null) {
            throw new Exception("Obligatory version field does not exist.");
        }
        int[] version = {-1, -1, -1};
        int i = 0;
        for (JsonNode ver : packetVersion) {
            version[i] = ver.asInt();
            i += 1;
        }

        /* Extract global datetime. */
        JsonNode packetDatetime = packetBody.get("datetime");
        if (packetDatetime == null) {
            throw new Exception("Obligatory datetime field does not exist.");
        }
        Date datetime;
        try {
            datetime = Date.from(Instant.parse(packetDatetime.asText()));
        } catch (Exception e) {
            throw new Exception("Invalid datetime.", e);
        }

        if (version[0] < 0) {
            throw new Exception("Given version cannot be handled.");
        }

        /* Parse all data blocks (if any). */
        JsonNode packetData = packetBody.get("data");
        if (packetData != null) {
            if (!packetData.isArray()) {
                throw new Exception("Invalid data array.");
            }
            for (JsonNode dataBlock : packetData) {
                positions.add(parseDataBlock(deviceSession, version, datetime, dataBlock));
            }
        }

        return positions.isEmpty() ? null : positions;
    }
}
