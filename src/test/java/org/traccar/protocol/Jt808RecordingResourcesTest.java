package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.media.VideoRecordingManager;
import org.traccar.model.Command;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Jt808RecordingResourcesTest extends ProtocolTest {

    private static final String BODY = "000100000002012610222000002610222005008000000000000001020101f000000003261023080000261023081000000000000000000000020200000100";
    private static final String RESPONSE = "7e1205003e0b3a73ce2ff23456000100000002012610222000002610222005008000000000000001020101f0000000032610230800002610230810000000000000000000000202000001007c7e";

    @Test
    public void testEncodeResourceQuery() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        var channel = channel(decoder, encoder);
        Command command = query();
        verifyEncode(channel, command, binary("7e920500180b3a73ce2ff212340126102220000026102221000080000000000000010201017b7e"));

        decoder.setProtocolVersion(3);
        ByteBuf encoded = (ByteBuf) encoder.encodeCommand(channel, command);
        try {
            assertEquals(0x9205, encoded.getUnsignedShort(1));
            assertEquals(0x4018, encoded.getUnsignedShort(3));
            assertEquals(3, encoded.getUnsignedByte(5));
            assertEquals(0x1234, encoded.getUnsignedShort(16));
            assertEquals(1, encoded.getUnsignedByte(18));
        } finally {
            encoded.release();
        }
    }

    private Command query() {
        var command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VIDEO_RECORDING_LIST);
        command.set("from", "2026-10-22T12:00:00Z");
        command.set("to", "2026-10-22T13:00:00Z");
        command.set(Command.KEY_INDEX, 1);
        command.set("resourceType", 2);
        command.set("streamType", 1);
        command.set("storageType", 1);
        command.set("alarmMask", "8000000000000001");
        command.set("sequence", 0x1234);
        return command;
    }

    @Test
    public void testDecodeResourcesWithoutGpsPositions() throws Exception {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        verifyDecode(decoder, binary(RESPONSE));

        var result = manager.get(1, 10, request.id());
        assertEquals("completed", result.state());
        assertEquals(2, result.recordings().size());
        var first = result.recordings().getFirst();
        assertEquals("2026-10-22T12:00:00Z", first.from());
        assertEquals("2026-10-22T12:05:00Z", first.to());
        assertEquals("8000000000000001", first.alarmMask());
        assertEquals(4026531840L, first.size());
        assertEquals(3, result.recordings().get(1).channel());
        assertEquals("2026-10-23T00:00:00Z", result.recordings().get(1).from());
        assertEquals(2, result.recordings().get(1).storageType());
    }

    private ByteBuf packet(int count, int index, byte[] body) {
        ByteBuf result = Unpooled.buffer();
        result.writeByte(0x7e);
        result.writeShort(0x1205);
        result.writeShort(0x2000 | body.length);
        result.writeBytes(DataConverter.parseHex("0b3a73ce2ff2"));
        result.writeShort(0x3456);
        result.writeShort(count);
        result.writeShort(index);
        result.writeBytes(body);
        result.writeByte(Checksum.xor(result.nioBuffer(1, result.readableBytes() - 1)));
        result.writeByte(0x7e);
        return result;
    }

    @Test
    public void testSegmentedResponseOutOfOrderAndDuplicateAcknowledgments() throws Exception {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        byte[] body = HexFormat.of().parseHex(BODY);
        var channel = channel();
        byte[] first = Arrays.copyOfRange(body, 0, 17);
        byte[] second = Arrays.copyOfRange(body, 17, body.length);
        var frames = new ByteBuf[] {packet(2, 2, second), packet(2, 2, second), packet(2, 1, first)};
        try {
            for (ByteBuf frame : frames) {
                assertNull(decoder.decode(channel, null, frame));
            }
            assertEquals(3, channel.outboundMessages().size());
            assertEquals("completed", manager.get(1, 10, request.id()).state());
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }

    @Test
    public void testInvalidBodyFailsInsteadOfReportingEmptySuccess() throws Exception {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        ByteBuf frame = packet(1, 1, DataConverter.parseHex("000100000001"));
        try {
            assertThrows(IllegalArgumentException.class, () -> decoder.decode(null, null, frame));
            assertEquals("failed", manager.get(1, 10, request.id()).state());
        } finally {
            frame.release();
        }
    }

    @Test
    public void testEmptyResponse() throws Exception {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        ByteBuf frame = packet(1, 1, DataConverter.parseHex("000100000000"));
        try {
            assertNull(decoder.decode(null, null, frame));
            assertTrue(manager.get(1, 10, request.id()).recordings().isEmpty());
            assertEquals("completed", manager.get(1, 10, request.id()).state());
        } finally {
            frame.release();
        }
    }

    @Test
    public void testStrictResourceValidation() {
        for (String body : new String[] {
                "000100000001",
                "0001ffffffff",
                BODY.substring(0, BODY.length() - 2),
                BODY.substring(0, 14) + "2a" + BODY.substring(16),
        }) {
            ByteBuf data = Unpooled.wrappedBuffer(DataConverter.parseHex(body));
            try {
                assertThrows(RuntimeException.class, () -> Jt808RecordingResources.decode(data, ZoneId.of("UTC")));
            } finally {
                data.release();
            }
        }
    }

    @Test
    public void testInvalidQueryRejectedBeforeEncoding() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        var encoder = inject(new Jt808ProtocolEncoder(null));
        var channel = channel(decoder, encoder);
        var command = query();
        command.set(Command.KEY_INDEX, 32);
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeCommand(channel, command));
    }

    @Test
    public void testAcknowledgmentIsNotARecordingListAndRejectionFailsQuery() throws Exception {
        var manager = new VideoRecordingManager();
        var request = manager.create(1, 10);
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(manager);
        verifyDecode(decoder, binary("7e000100050b3a73ce2ff200010001920500c27e"));
        assertEquals("pending", manager.get(1, 10, request.id()).state());
        verifyDecode(decoder, binary("7e000100050b3a73ce2ff200010001920503c17e"));
        assertEquals("failed", manager.get(1, 10, request.id()).state());
    }

    @Test
    public void testChecksumRejected() throws Exception {
        var decoder = inject(new Jt808ProtocolDecoder(null));
        decoder.setRecordingManager(new VideoRecordingManager());
        ByteBuf data = binary(RESPONSE);
        data.setByte(data.writerIndex() - 2, 0);
        try {
            assertThrows(IllegalArgumentException.class, () -> decoder.decode(null, null, data));
        } finally {
            data.release();
        }
    }
}
