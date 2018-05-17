/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import org.jboss.netty.channel.Channel;
import org.joda.time.format.ISODateTimeFormat;
import org.traccar.DeviceSession;
import org.traccar.JsonProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

/**
 *
 * @author Samson
 */
public class StrongTowerProtocolDecoder extends JsonProtocolDecoder {

    public StrongTowerProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        String js = (String) msg;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(js);
        String action = null;
        Position position = new Position(getProtocolName());
        Network network = new Network();
        Iterator<Map.Entry<String, JsonNode>> elements = json.fields();
        while (elements.hasNext()) {
            Map.Entry<String, JsonNode> element = elements.next();
            String key = element.getKey();
            JsonNode jValue = element.getValue();
            String value = jValue.asText();
            switch (key) {
                case "id":
                case "deviceid":
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, value);
                    if (deviceSession == null) {
                        throw new Exception("No Device Session found [" + js + "]");
                    }
                    position.setDeviceId(deviceSession.getDeviceId());
                    break;
                case "action":
                    action = value;
                    break;
                case "valid":
                    position.setValid(Boolean.parseBoolean(value));
                    break;
                case "timestamp":
                    try {
                        long timestamp = Long.parseLong(value);
                        if (timestamp < Integer.MAX_VALUE) {
                            timestamp *= 1000;
                        }
                        position.setTime(new Date(timestamp));
                    } catch (NumberFormatException error) {
                        if (value.contains("T")) {
                            position.setTime(new Date(
                                    ISODateTimeFormat.dateTimeParser().parseMillis(value)));
                        } else {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            position.setTime(dateFormat.parse(value));
                        }
                    }
                    break;
                case "lat":
                case "latitude":
                    position.setLatitude(Double.parseDouble(value));
                    break;
                case "lon":
                case "longitude":
                    position.setLongitude(Double.parseDouble(value));
                    break;
                case "location":
                    String[] location = value.split(",");
                    position.setLatitude(Double.parseDouble(location[0]));
                    position.setLongitude(Double.parseDouble(location[1]));
                    break;
                case "cell":
                    String[] cell = value.split(",");
                    if (cell.length > 4) {
                        network.addCellTower(CellTower.from(
                                Integer.parseInt(cell[0]), Integer.parseInt(cell[1]),
                                Integer.parseInt(cell[2]), Integer.parseInt(cell[3]), Integer.parseInt(cell[4])));
                    } else {
                        network.addCellTower(CellTower.from(
                                Integer.parseInt(cell[0]), Integer.parseInt(cell[1]),
                                Integer.parseInt(cell[2]), Integer.parseInt(cell[3])));
                    }
                    break;
                case "wifi":
                    String[] wifi = value.split(",");
                    network.addWifiAccessPoint(WifiAccessPoint.from(
                            wifi[0].replace('-', ':'), Integer.parseInt(wifi[1])));
                    break;
                case "speed":
                    position.setSpeed(convertSpeed(Double.parseDouble(value), "kn"));
                    break;
                case "course":
                case "bearing":
                case "heading":
                    position.setCourse(Double.parseDouble(value));
                    break;
                case "alt":
                case "altitude":
                case "elevation":
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case "accuracy":
                    position.setAccuracy(Double.parseDouble(value));
                    break;
                case "hdop":
                    position.set(Position.KEY_HDOP, Double.parseDouble(value));
                    break;
                case "batt":
                case "batteryLevel":
                    position.set(Position.KEY_BATTERY_LEVEL, Double.parseDouble(value));
                    break;
                case "driverUniqueId":
                    position.set(Position.KEY_DRIVER_UNIQUE_ID, value);
                    break;
                default:
                    try {
                        position.set(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        switch (value) {
                            case "true":
                                position.set(key, true);
                                break;
                            case "false":
                                position.set(key, false);
                                break;
                            default:
                                position.set(key, value);
                                break;
                        }
                    }
                    break;
            }
        }

        if (position.getDeviceId() != 0) {
			if ("login".equals(action) || "heartbeat".equals(action)) {
				if (channel != null) {
					channel.write("{\"status\":0}\r\n", remoteAddress);
				}
			} else if ("status".equals(action)){
				if (position.getFixTime() == null) {
					position.setTime(new Date());
				}
				if (network.getCellTowers() != null || network.getWifiAccessPoints() != null) {
					position.setNetwork(network);
				}
				if (!position.getValid() || (position.getLatitude() <= 0 && position.getLongitude() <= 0)) {
					getLastLocation(position, null);
				}
				if (channel != null) {
					channel.write("{\"status\":0}\r\n", remoteAddress);
				}
				if (position.getValid()) {
					return position;
				}
			} else {
				throw new Exception("Unknown Request in [" + js + "]");
			}
		}else{
            throw new Exception("Unknown Device ID in [" + js + "]");
		}		
        return null;
}
