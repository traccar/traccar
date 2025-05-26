/*
 * Copyright 2020 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2025 - 2025 Gerrit Maus (funk@maus.xyz)
 *
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.traccar.BaseMqttProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DcjbProtocolDecoder extends BaseMqttProtocolDecoder {

	public DcjbProtocolDecoder(Protocol protocol) {
		super(protocol);
	}

    @Override
    protected Object decode(
            DeviceSession deviceSession, MqttPublishMessage message) throws Exception {

        List<Position> positions = new LinkedList<>();

        ByteBuf buf = message.payload();

        return positions.isEmpty() ? null : positions;
    }

}

