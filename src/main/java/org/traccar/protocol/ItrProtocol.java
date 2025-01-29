package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

import java.util.List;

public class Itr120Protocol extends BaseProtocol {

    public Itr120Protocol() {
        super("itr120");
        setSupportedDataCommands(
            Command.TYPE_ENGINE_STOP,
            Command.TYPE_ENGINE_RESUME
        );
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new Itr120FrameDecoder());
                pipeline.addLast(new Itr120ProtocolDecoder(Itr120Protocol.this));
            }
        });
    }
}

class Itr120FrameDecoder extends io.netty.handler.codec.ByteToMessageDecoder {
    
    @Override
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) { // Minimum package size (mark + pid + size)
            return;
        }

        in.markReaderIndex();

        // Check start marker (0x28 0x28)
        if (in.readByte() != 0x28 || in.readByte() != 0x28) {
            in.resetReaderIndex();
            return;
        }

        // Read PID and size
        int pid = in.readByte();
        int size = in.readUnsignedShort();

        // Check if we have enough data
        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }

        // Extract the complete package
        ByteBuf frame = in.readRetainedSlice(size);
        out.add(frame);
    }
}

class Itr120ProtocolDecoder extends BaseProtocolDecoder {

    public Itr120ProtocolDecoder(Itr120Protocol protocol) {
        super(protocol);
    }

    private Position decodePosition(Channel channel, ByteBuf buf) {
        Position position = new Position(getProtocolName());

        // Time
        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        // Mask byte for optional fields
        int mask = buf.readUnsignedByte();

        // GPS data is present (bit 0)
        if ((mask & 0x01) != 0) {
            // Latitude
            position.setLatitude(buf.readInt() / 1800000.0);

            // Longitude
            position.setLongitude(buf.readInt() / 1800000.0);

            // Altitude
            position.setAltitude(buf.readShort());

            // Speed in km/h
            position.setSpeed(buf.readUnsignedShort());

            // Course
            position.setCourse(buf.readUnsignedShort());

            // Satellites
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        return position;
    }

    @Override
    protected Object decode(Channel channel, ByteBuf buf) throws Exception {
        if (buf.readableBytes() < 3) {
            return null;
        }

        int pid = buf.readUnsignedByte();
        int sequence = buf.readUnsignedShort();

        switch (pid) {
            case 0x01: // Login package
                // Handle login and send response
                String imei = buf.readSlice(8).toString(StandardCharsets.US_ASCII);
                identify(imei, channel, null);
                
                // Send login response
                if (channel != null) {
                    ByteBuf response = Unpooled.buffer();
                    response.writeShort(0x2828); // mark
                    response.writeByte(0x01); // pid
                    response.writeShort(0x09); // size
                    response.writeShort(sequence); // sequence
                    response.writeInt((int) (System.currentTimeMillis() / 1000)); // time
                    response.writeByte(0x01); // version
                    response.writeByte(0x03); // param-set action
                    channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
                }
                return null;

            case 0x12: // Location package
                Position position = decodePosition(channel, buf);
                
                if (position != null) {
                    // Additional data
                    position.set(Position.KEY_STATUS, buf.readUnsignedShort());
                    position.set(Position.KEY_BATTERY, buf.readUnsignedShort());
                    position.set(Position.KEY_INPUT, buf.readUnsignedShort()); // AIN0
                    buf.skipBytes(2); // AIN1
                    position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
                    
                    // Send response
                    if (channel != null) {
                        ByteBuf response = Unpooled.buffer();
                        response.writeShort(0x2828);
                        response.writeByte(0x12);
                        response.writeShort(0x02);
                        response.writeShort(sequence);
                        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
                    }
                    
                    return position;
                }
                return null;

            default:
                return null;
        }
    }
}
