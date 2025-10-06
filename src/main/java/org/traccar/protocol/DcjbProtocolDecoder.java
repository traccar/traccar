/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2025 Gerrit Maus (funk@maus.xyz)
 * Copyright 2025 Christina Maus (frau@maus.xyz)
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.traccar.BaseMqttProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder for DCJB MQTT packets.
 */
public class DcjbProtocolDecoder extends BaseMqttProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcjbProtocolDecoder.class);

    private void debugLog(String fmt) {
        LOGGER.debug("DCJB: " + fmt);
    }
    private void debugLog(String fmt, Object arg1) {
        LOGGER.debug("DCJB: " + fmt, arg1);
    }
    private void debugLog(String fmt, Object arg1, Object arg2) {
        LOGGER.debug("DCJB: " + fmt, arg1, arg2);
    }
    private void debugLog(String fmt, Object arg1, Object arg2, Object arg3) {
        LOGGER.debug("DCJB: " + fmt, arg1, arg2, arg3);
    }

    public DcjbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    /**
     * Parses a single coordinate object (`east` or `north`).
     */
    private double parseCoordinate(JsonNode coordNode) throws Exception {
        if (coordNode == null || coordNode.isNull()) {
            throw new Exception("No coordinate(s) specified.");
        }
        if (coordNode.isObject()) {
            /* Parse the degrees. */
            JsonNode degNode = getNodeForKey(coordNode, "deg");
            if (degNode == null || degNode.isNull()) {
                throw new Exception("No degree value specified.");
            }
            double coord = degNode.asDouble();
            /* Minutes and seconds are added to the **absolute value** of the
             * degree field!
             */
            float sign;
            if (coord >= 0) {
                sign = 1;
            } else {
                sign = -1;
            }
            /* Parse the minutes. */
            JsonNode minNode = getNodeForKey(coordNode, "min");
            if (minNode != null && !minNode.isNull()) {
                coord += sign * minNode.asDouble() / 60.0;
            }
            /* Parse the seconds. */
            JsonNode secNode = getNodeForKey(coordNode, "sec");
            if (secNode != null && !secNode.isNull()) {
                coord += sign * secNode.asDouble() / 60.0 / 60.0;
            }
            return coord;
        } else {
            /* Only the decimal degree value is given. */
            return coordNode.asDouble();
        }
    }

    /**
     * Parses the values of a GNSS instance type.
     */
    private Position parseValuesGNSS(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Determine the validity of the coordinate. */
        boolean valid;
        final JsonNode qualityNode = getNodeForKey(valuesNode, "quality");
        if (qualityNode == null || qualityNode.asInt() > 0) {
            valid = true;
            position.setLatitude(parseCoordinate(getNodeForKey(valuesNode, "north")));
            position.setLongitude(parseCoordinate(getNodeForKey(valuesNode, "east")));
            debugLog("   -> Valid coordinate {} N, {} E", position.getLatitude(), position.getLongitude());
        } else {
            valid = false;
        }
        position.setValid(valid);
        /* Altitude. */
        final JsonNode heightNode = getNodeForKey(valuesNode, "height");
        if (heightNode != null) {
            position.setAltitude(heightNode.asDouble());
        }
        /* Speed. */
        final JsonNode speedNode = getNodeForKey(valuesNode, "speed");
        if (speedNode != null) {
            position.setSpeed(speedNode.asDouble());
        }
        /* Course. */
        final JsonNode courseNode = getNodeForKey(valuesNode, "course");
        if (courseNode != null) {
            position.setCourse(courseNode.asDouble());
        }
        /* Satellites in use. */
        final JsonNode inuseNode = getNodeForKey(valuesNode, "inuse");
        if (inuseNode != null) {
            position.set(position.KEY_SATELLITES, inuseNode.asInt());
        }
        /* Satellites visible. */
        final JsonNode visibleNode = getNodeForKey(valuesNode, "visible");
        if (visibleNode != null) {
            position.set(position.KEY_SATELLITES_VISIBLE, visibleNode.asInt());
        }
        return position;
    }

    /**
     * Calculates the Euclidean vector norm from the given list of values.
     */
    private <T extends Number> double calcNorm(List<T> vals) {
        double norm = 0;
        for (T val : vals) {
            double dVal = val.doubleValue();
            norm += dVal * dVal;
        }
        return Math.sqrt(norm);
    }

    /**
     * Calculates the average from the given list of values.
     */
    private <T extends Number> double calcAvg(List<T> vals) {
        double avg = 0;
        int num = 0;
        for (T val : vals) {
            double dVal = val.doubleValue();
            avg += dVal;
            num += 1;
        }
        return avg / num;
    }

    /**
     * Parses the numeric values from `level`, `measurement`, `state`, and
     * `bool` instances.
     */
    private <T extends Number> Position setNumericValues(
        List<T> vals, Position position, String name, String flavour, int[] version
    ) throws Exception {

        final String[] supportedNames = {
            "fall", "roam",
            "status",
            "bat",
            "rss", "temp", "press", "humid", "acc", "gyro"
        };
        final String fullName = lookupIdentifier(supportedNames, name);
        String doublePrefix = "";
        String intPrefix = "";
        String boolPrefix = "";
        String doubleKey = "";
        String intKey = "";
        String boolKey = "";
        double doubleValue = 0;
        int intValue = 0;
        boolean boolValue = false;
        switch (fullName) {
            case "fall":
                if (vals.get(0).intValue() != 0) {
                    position.addAlarm(position.ALARM_FALL_DOWN);
                    debugLog("   -> ALARM: fall");
                }
                break;
            case "roam":
                boolKey = position.KEY_ROAMING;
                boolValue = (vals.get(0).intValue() != 0);
                break;
            case "status":
                intKey = position.KEY_STATUS;
                intValue = vals.get(0).intValue();
                break;
            case "bat":
                doublePrefix = position.PREFIX_BATTERY_LEVEL;
                doubleKey = position.KEY_BATTERY_LEVEL;
                doubleValue = calcAvg(vals);
                break;
            case "rss":
                doubleKey = position.KEY_RSSI;
                doubleValue = vals.get(0).doubleValue();
                break;
            case "temp":
                doubleKey = position.KEY_DEVICE_TEMP;
                doubleValue = vals.get(0).doubleValue();
                break;
            case "press":
                doubleKey = position.KEY_DEVICE_PRESSURE;
                doubleValue = vals.get(0).doubleValue();
                break;
            case "humid":
                doubleKey = position.KEY_HUMIDITY;
                doubleValue = vals.get(0).doubleValue();
                break;
            case "acc":
                doublePrefix = position.PREFIX_ACCELERATION;
                doubleKey = position.KEY_ACCELERATION;
                doubleValue = calcNorm(vals);
                break;
            case "gyro":
                doublePrefix = position.PREFIX_GYROSCOPE;
                break;
            default:
                throw new Exception("Unsupported measurement name '" + fullName + "'.");
        }
        /* Handle double values. */
        if (!doublePrefix.equals("")) {
            int i = 1;
            for (T val : vals) {
                position.set(doublePrefix + i, val.doubleValue());
                i += 1;
            }
        }
        if (!doubleKey.equals("")) {
            position.set(doubleKey, doubleValue);
            debugLog("   -> {}: {}", doubleKey, doubleValue);
        }
        /* Handle integer values. */
        if (!intPrefix.equals("")) {
            int i = 1;
            for (T val : vals) {
                position.set(intPrefix + i, val.intValue());
                i += 1;
            }
        }
        if (!intKey.equals("")) {
            position.set(intKey, intValue);
            debugLog("   -> {}: {}", intKey, intValue);
        }
        /* Handle boolean values. */
        if (!boolPrefix.equals("")) {
            int i = 1;
            for (T val : vals) {
                position.set(boolPrefix + i, (val.intValue() != 0));
                i += 1;
            }
        }
        if (!boolKey.equals("")) {
            position.set(boolKey, boolValue);
            debugLog("   -> {}: {}", boolKey, boolValue);
        }

        return position;
    }

    /**
     * Parses the values of a measurement instance type.
     */
    private Position parseValuesMeasurement(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Test for an allowed collapse. */
        JsonNode avgNode;
        if (valuesNode.isObject()) {
            /* The `valuesNode` has not been collapsed. */
            avgNode = getNodeForKey(valuesNode, "avg");
        } else {
            /* The `valuesNode` has been collapsed at least into the `avg` node. */
            avgNode = valuesNode;
        }

        /* Get all avg values. */
        List<Double> avgs = new LinkedList<>();
        if (avgNode.isArray()) {
            for (JsonNode avg : avgNode) {
                avgs.add(avg.asDouble());
            }
        } else {
            /* The avg array has been collapsed into a single value. */
            avgs.add(avgNode.asDouble());
        }

        position = setNumericValues(avgs, position, name, flavour, version);

        /* Upper/Lower/Std are currently unsupported and unmapped. */

        return position;
    }

    /**
     * Parses the values of a level instance type.
     */
    private Position parseValuesLevel(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Default values for the upper and lower thresholds. */
        double lower = 0.0;
        double upper = 100.0;
        /* Test for an allowed collapse. */
        JsonNode pointsNode;
        if (valuesNode.isObject()) {
            /* The `valuesNode` has not been collapsed. */
            pointsNode = getNodeForKey(valuesNode, "points");
            /* Look for the upper and lower thresholds. */
            final JsonNode upperNode = getNodeForKey(valuesNode, "upper");
            if (upperNode != null && !upperNode.isNull()) {
                upper = upperNode.asDouble();
            }
            final JsonNode lowerNode = getNodeForKey(valuesNode, "lower");
            if (lowerNode != null && !lowerNode.isNull()) {
                lower = lowerNode.asDouble();
            }
        } else {
            /* The `valuesNode` has been collapsed at least into the `points` node. */
            pointsNode = valuesNode;
        }

        /* Get all percentage points values. */
        List<Double> points = new LinkedList<>();
        if (pointsNode.isArray()) {
            for (JsonNode pointNode : pointsNode) {
                double point = pointNode.asDouble();
                if (point < lower) {
                    point = lower;
                } else if (point > upper) {
                    point = upper;
                }
                points.add(
                    (point - lower) / (upper - lower) * 100
                );
            }
        } else {
            /* The points array has been collapsed into a single value. */
            double point = pointsNode.asDouble();
            if (point < lower) {
                point = lower;
            } else if (point > upper) {
                point = upper;
            }
            points.add(
                (point - lower) / (upper - lower) * 100
            );
        }

        position = setNumericValues(points, position, name, flavour, version);

        return position;
    }

    /**
     * Parses the values of a bool instance type.
     */
    private Position parseValuesBool(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Boolean values are handled as integer values. The differentiation
         * takes place within `setNumericValues()`.
         */
        return parseValuesState(valuesNode, position, name, flavour, version);
    }

    /**
     * Parses the values of a state instance type.
     */
    private Position parseValuesState(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Test for an allowed collapse. */
        JsonNode statesNode;
        if (valuesNode.isObject()) {
            /* The `valuesNode` has not been collapsed. */
            statesNode = getNodeForKey(valuesNode, "states");
        } else {
            /* The `valuesNode` has been collapsed at least into the `states` node. */
            statesNode = valuesNode;
        }

        /* Get all states. */
        List<Integer> states = new LinkedList<>();
        if (statesNode.isArray()) {
            for (JsonNode state : statesNode) {
                states.add(state.asInt());
            }
        } else {
            /* The states array has been collapsed into a single value. */
            states.add(statesNode.asInt());
        }

        position = setNumericValues(states, position, name, flavour, version);

        return position;
    }

    /**
     * Parses the values of a revision instance type.
     */
    private Position parseValuesRevision(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Test for an allowed collapse of the values node. */
        JsonNode revisionNode;
        if (valuesNode.isObject()) {
            /* The `valuesNode` has not been collapsed. */
            revisionNode = getNodeForKey(valuesNode, "revision");
        } else {
            /* The `valuesNode` has been collapsed at least into the `revision` node. */
            revisionNode = valuesNode;
        }

        /* Get all parts of the revision. */
        int i = 0;
        String revision = "";
        if (revisionNode.isArray()) {
            for (JsonNode revNode : revisionNode) {
                if (i == 1 || i == 2) {
                    revision += ".";
                } else if (i >= 3) {
                    revision += "-";
                }
                revision += revNode.asText();
                i += 1;
            }
        } else {
            /* The revision array has been collapsed into a single value. */
            revision = revisionNode.asText();
        }

        /* Get the full name. */
        final String[] supportedRevisions = {"fw", "hw"};
        final String fullName = lookupIdentifier(supportedRevisions, name);
        String key = "";
        switch (fullName) {
            case "fw":
                key = Position.KEY_VERSION_FW;
            break;
            case "hw":
                key = Position.KEY_VERSION_HW;
            break;
        }
        debugLog("   -> {}: {}", key, revision);
        position.set(key, revision);

        return position;
    }

    /**
     * Parses the values of an identifier instance type.
     */
    private Position parseValuesIdentifier(
        JsonNode valuesNode, Position position, String name, String flavour, int[] version
    ) throws Exception {

        /* Test for an allowed collapse of the values node. */
        JsonNode identifierNode;
        if (valuesNode.isObject()) {
            /* The `valuesNode` has not been collapsed. */
            identifierNode = getNodeForKey(valuesNode, "identifier");
        } else {
            /* The `valuesNode` has been collapsed into the `identifier` node. */
            identifierNode = valuesNode;
        }

        if (identifierNode == null) {
            throw new Exception("No identifier node given.");
        }
        final String identifier = identifierNode.asText();
        final String[] supportedIdentifier = {"imsi", "imei", "cid", "lac", "type"};
        final String fullName = lookupIdentifier(supportedIdentifier, name);
        String key = "";
        switch (fullName) {
            case "imsi":
                key = Position.KEY_IMSI;
            break;
            case "imei":
                key = Position.KEY_IMEI;
            break;
            case "cid":
                key = Position.KEY_CID;
            break;
            case "lac":
                key = Position.KEY_LAC;
            break;
            case "type":
                key = Position.KEY_TYPE;
            break;
        }
        debugLog("    -> {}: {}", key, identifier);
        position.set(key, identifier);

        return position;
    }

    /**
     * Parses a single instance.
     */
    private Position parseInstance(
        JsonNode instanceNode, Position position, String flavour, int[] version
    ) throws Exception {

        /* Get the type. */
        final JsonNode typeNode = getNodeForKey(instanceNode, "type");
        String type;
        if (typeNode == null) {
            throw new Exception("No type field available.");
        } else {
            final String[] supportedTypes = {
                "gnss", "measurement", "level", "bool", "state", "revision", "identifier"
            };
            type = lookupIdentifier(
                supportedTypes,
                typeNode.asText()
            );
        }
        debugLog("  -> Processing instance with type '{}'.", type);

        /* Get the name (if any). */
        final JsonNode nameNode = getNodeForKey(instanceNode, "name");
        String name;
        if (nameNode == null) {
            name = "";
        } else {
            name = nameNode.asText();
        }

        /* The datetime on the instance level is ignored, as there is no
         * Traccar-compatible way to save that.
         */
        final JsonNode datetimeNode = getNodeForKey(instanceNode, "datetime");
        if (datetimeNode != null && !datetimeNode.isNull()) {
            LOGGER.warn("Given instance-level datetime is ignored..");
        }

        /* Get the actual values. */
        final JsonNode valuesNode = getNodeForKey(instanceNode, "values");
        if (valuesNode == null) {
            throw new Exception("No values field available.");
        }
        position = switch (type) {
            case "gnss" -> parseValuesGNSS(valuesNode, position, name, flavour, version);
            case "measurement" -> parseValuesMeasurement(valuesNode, position, name, flavour, version);
            case "level" -> parseValuesLevel(valuesNode, position, name, flavour, version);
            case "bool" -> parseValuesBool(valuesNode, position, name, flavour, version);
            case "state" -> parseValuesState(valuesNode, position, name, flavour, version);
            case "revision" -> parseValuesRevision(valuesNode, position, name, flavour, version);
            case "identifier" -> parseValuesIdentifier(valuesNode, position, name, flavour, version);
            default -> throw new Exception("Unsupported type '" + type + "'.");
        };

        return position;
    }

    /**
     * Parses a single data block.
     */
    private Position parseBlock(
        JsonNode blockNode, DeviceSession deviceSession, ZonedDateTime datetime, String flavour, int[] version
    ) throws Exception {

        Position position = new Position(getProtocolName());
        /* The UID is ignored here, since the device management is done by Traccar
         * on a higher level. We have to adhere to that so far!
         */
        position.setDeviceId(deviceSession.getDeviceId());
        /* We actually allow here the datetime not to be set. */
        ZonedDateTime blocktime = parseDatetime(
            getNodeForKey(blockNode, "datetime"), datetime
        );
        /* Get all instance nodes. */
        List<JsonNode> instances = new LinkedList<>();
        JsonNode instancesNode = getNodeForKey(blockNode, "instances");
        if (instancesNode == null) {
            throw new Exception("No instances field available.");
        } else if (instancesNode.isArray()) {
            /* The instances node is a regular array of instances. */
            for (JsonNode instance : instancesNode) {
                instances.add(instance);
            }
        } else {
            /* The instances node has been collapsed into a single
             * instance.
             */
            instances.add(instancesNode);
        }
        /* Assemble the position object from all given instances. */
        position.setDeviceTime(Date.from(datetime.toInstant()));
        position.setFixTime(Date.from(blocktime.toInstant()));
        debugLog(" -> Parse data block with time {}.", blocktime.toString());
        for (JsonNode instance : instances) {
            position = parseInstance(instance, position, flavour, version);
        }

        return position;
    }

    /**
     * Parses the data blocks (if any).
     */
    private List<Position> parseBlocks(
        JsonNode blocksNode, DeviceSession deviceSession, ZonedDateTime datetime, String flavour, int[] version
    ) throws Exception {

        List<Position> positions = new LinkedList<>();
        debugLog("Parse packet with time {}.", datetime.toString());

        if (blocksNode != null) {
            if (blocksNode.isObject()) {
                /* This has been collapsed to a single data block. */
                positions.add(parseBlock(
                    blocksNode, deviceSession, datetime, flavour, version
                ));
            } else if (blocksNode.isArray()) {
                /* This is an array of data blocks. */
                for (JsonNode blockNode : blocksNode) {
                    positions.add(parseBlock(
                        blockNode, deviceSession, datetime, flavour, version
                    ));
                }
            } else {
                throw new Exception("Invalid document structure on block level.");
            }
        }

        return positions;
    }

    /**
     * Parses the given JSON node as an ISO datetime string and returns the
     * corresponding `ZonedDateTime` object. A missing or ill-formatted datetime
     * leads to an exception.
     */
    private ZonedDateTime parseDatetime(JsonNode datetimeNode) throws Exception {

        if (datetimeNode == null || datetimeNode.isNull()) {
            throw new Exception("No datetime given.");
        }
        try {
             return ZonedDateTime.parse(datetimeNode.asText());
        } catch (Exception e) {
            throw new Exception("Invalid datetime '" + datetimeNode.asText() + "'.");
        }
    }

    /**
     * Parses the given JSON node EITHER as an ISO datetime string and returns
     * the corresponding `ZonedDateTime` object OR as an integer offset, which
     * is then added to the given `datetimeDefault` value and returned. If the
     * datetime is not set (either non-existing or explicitly set to `null`),
     * the given default value is returned without any alteration. An ill-formed
     * datetime string or offset leads to an exception.
     */
    private ZonedDateTime parseDatetime(
        JsonNode datetimeNode, ZonedDateTime datetimeDefault
    ) throws Exception {

        if (datetimeNode == null || datetimeNode.isNull()) {
            return datetimeDefault;
        } else if (datetimeNode.isInt()) {
            return datetimeDefault.plusSeconds(datetimeNode.asInt());
        } else {
            try {
                 return ZonedDateTime.parse(datetimeNode.asText());
            } catch (Exception e) {
                throw new Exception("Invalid datetime '" + datetimeNode.asText() + "'.");
            }
        }
    }

    /**
     * Parses the flavour of the packet. If the flavour is not explicitly
     * specified, it is auto-magically derived from the MQTT topic.
     */
    private String parseFlavour(JsonNode flavourNode, String mqttTopic) throws Exception {

        /* GET the flavour string to look up. */
        String flavourString;
        if (flavourNode == null || flavourNode.isNull()) {
            /* We might support some more advanced parsing in the future... */
            flavourString = mqttTopic;
        } else {
            flavourString = flavourNode.asText();
        }

        /* LOOK UP that string. */
        String[] identifiers = {"contad", "sensofinder"};
        return lookupIdentifier(identifiers, flavourString);
    }

    /**
     * Parses the given JSON node as the version identifier. The node is
     * expected to be an array of three integers or a single integer otherwise.
     */
    private int[] parseVersion(JsonNode versionNode) throws Exception {

        int[] version = {-1, 0, 0};

        if (versionNode == null) {
            throw new Exception("Obligatory version field does not exist.");
        }
        if (versionNode.isArray()) {
            int i = 0;
            for (JsonNode ver : versionNode) {
                if (i > 2) {
                    throw new Exception("Too many versioning levels given.");
                }
                version[i] = ver.asInt();
                i += 1;
            }
        } else {
            version[0] = versionNode.asInt();
        }

        return version;
    }

    /**
     * Checks the given `jsonNode` for the given `key` and returns the
     * corresponding JSON node. If such a node does not exist, an alternative
     * key that consists just of the first letter from `key` is searched for
     * and returned. Note that the latter might still be non existent, which is
     * then up to the caller to decice on.
     */
    private JsonNode getNodeForKey(JsonNode jsonNode, String key) {

        JsonNode node = jsonNode.get(key);
        if (node == null && key.length() > 0) {
            final String shortKey = "" + key.charAt(0);
            return jsonNode.get(shortKey);
        } else {
            return node;
        }
    }

    /**
     * Returns the **first** array element that starts with the given look-up
     * string. This look-string is required to have a length of at least one.
     */
    private String lookupIdentifier(String[] identifiers, String lookup) throws Exception {

        if (lookup.length() < 1) {
            throw new Exception("Look-up string is empty.");
        }
        for (String identifier : identifiers) {
            if (identifier.startsWith(lookup)) {
                /* Found it. Return immediately! */
                return identifier;
            }
        }
        throw new Exception("Look-up string '" + lookup + "' cannot be found in identifier list.");
    }

    @Override
    protected Object decode(DeviceSession deviceSession, MqttPublishMessage message) throws Exception {

        String mqttTopic = deviceSession.get("mqttTopic");

        /*
         * The full message, either plain JSON or gzipped JSON (to be implemented).
         */
        final ByteBuf buf = message.payload();
        final String msgStr = buf.toString(StandardCharsets.UTF_8);

        /* Parse JSON. */
        final ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
        JsonNode bodyNode;
        try {
            bodyNode = objectMapper.readTree(msgStr);
        } catch (Exception e) {
            throw new Exception("Invalid JSON.");
        }

        /* Check version. */
        int[] version = parseVersion(getNodeForKey(bodyNode, "version"));
        debugLog("version {}.{}.{}.", version[0], version[1], version[2]);

        if (version[0] < 0) {
            throw new Exception("Given version cannot be handled.");
        }

        /* Check flavour. */
        String flavour = parseFlavour(
            getNodeForKey(bodyNode, "flavour"),
            mqttTopic
        );
        debugLog("flavour {}.", flavour);

        /* Extract global datetime. */
        ZonedDateTime datetime = parseDatetime(getNodeForKey(bodyNode, "datetime"));

        /* Parse all data blocks (if any). */
        List<Position> positions = parseBlocks(
            getNodeForKey(bodyNode, "blocks"), deviceSession, datetime, flavour, version
        );

        return positions.isEmpty() ? null : positions;
    }
}
