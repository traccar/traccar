/*
 * Copyright 2019 - 2024 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.traccar.BaseHttpProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GlobalstarProtocolDecoder extends BaseHttpProtocolDecoder {

    private final DocumentBuilder documentBuilder;
    private final XPath xPath;
    private final XPathExpression messageExpression;

    public GlobalstarProtocolDecoder(Protocol protocol) {
        super(protocol);
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            builderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            builderFactory.setXIncludeAware(false);
            builderFactory.setExpandEntityReferences(false);
            documentBuilder = builderFactory.newDocumentBuilder();
            xPath = XPathFactory.newInstance().newXPath();
            messageExpression = xPath.compile("//stuMessages/stuMessage");
        } catch (ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponse(Channel channel, String messageId) throws TransformerException {

        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElement("stuResponseMsg");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute(
                "xsi:noNamespaceSchemaLocation", "http://cody.glpconnect.com/XSD/StuResponse_Rev1_0.xsd");
        rootElement.setAttribute("deliveryTimeStamp", new SimpleDateFormat("dd/MM/yyyy hh:mm:ss z").format(new Date()));
        rootElement.setAttribute("messageID", "00000000000000000000000000000000");
        rootElement.setAttribute("correlationID", messageId);
        document.appendChild(rootElement);

        Element state = document.createElement("state");
        state.appendChild(document.createTextNode("pass"));
        rootElement.appendChild(state);

        Element stateMessage = document.createElement("stateMessage");
        stateMessage.appendChild(document.createTextNode("Store OK"));
        rootElement.appendChild(stateMessage);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        ByteBuf content = Unpooled.buffer();
        transformer.transform(new DOMSource(document), new StreamResult(new ByteBufOutputStream(content)));

        if (channel != null) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers()
                    .add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                    .add(HttpHeaderNames.CONTENT_TYPE, "text/xml");
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        FullHttpRequest request = (FullHttpRequest) msg;

        Document document = documentBuilder.parse(new ByteBufferBackedInputStream(request.content().nioBuffer()));
        NodeList nodes = (NodeList) messageExpression.evaluate(document, XPathConstants.NODESET);

        List<Position> positions = new LinkedList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, xPath.evaluate("esn", node));
            if (deviceSession != null) {

                boolean atlas = "AtlasTrax".equalsIgnoreCase(getDeviceModel(deviceSession));
                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                position.setTime(new Date(Long.parseLong(xPath.evaluate("unixTime", node)) * 1000));

                ByteBuf buf = Unpooled.wrappedBuffer(
                        DataConverter.parseHex(xPath.evaluate("payload", node).substring(2)));

                int flags = buf.readUnsignedByte();
                int type;
                if (atlas) {
                    type = BitUtil.to(flags, 1);
                    position.setValid(true);
                    position.set(Position.PREFIX_IN + 1, !BitUtil.check(flags, 1));
                    position.set(Position.PREFIX_IN + 2, !BitUtil.check(flags, 2));
                    position.set(Position.KEY_CHARGE, !BitUtil.check(flags, 3));
                    if (BitUtil.check(flags, 4)) {
                        position.addAlarm(Position.ALARM_VIBRATION);
                    }
                    position.setCourse(BitUtil.from(flags, 5) * 45);
                } else {
                    type = BitUtil.to(flags, 2);
                    if (BitUtil.check(flags, 2)) {
                        position.set("batteryReplace", true);
                    }
                    position.setValid(!BitUtil.check(flags, 3));
                }

                double latitude = buf.readUnsignedMedium() * 90.0 / (1 << 23);
                position.setLatitude(latitude > 90 ? latitude - 180 : latitude);

                double longitude = buf.readUnsignedMedium() * 180.0 / (1 << 23);
                position.setLongitude(longitude > 180 ? longitude - 360 : longitude);

                int speed = 0;
                if (atlas) {
                    speed = buf.readUnsignedByte();
                    position.setSpeed(UnitsConverter.knotsFromKph(speed));
                    position.set("batteryReplace", BitUtil.check(buf.readUnsignedByte(), 7));
                } else if (type == 0) {
                    position.set(Position.KEY_INPUT, BitUtil.to(buf.readUnsignedByte(), 4));
                    int other = buf.readUnsignedByte();
                    if (BitUtil.check(other, 4)) {
                        position.addAlarm(Position.ALARM_VIBRATION);
                    }
                    position.set(Position.KEY_MOTION, BitUtil.check(other, 6));
                }

                if (speed != 0xff) {
                    positions.add(position);
                }

            }
        }

        sendResponse(channel, document.getFirstChild().getAttributes().getNamedItem("messageID").getNodeValue());
        return !positions.isEmpty() ? positions : null;
    }

}
