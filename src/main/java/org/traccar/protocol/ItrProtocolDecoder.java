
class ItrProtocolDecoder extends BaseProtocolDecoder {

    public ItrProtocolDecoder(ItrProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() < 5) {
            return null;
        }

        buf.skipBytes(2); // Marca inicial (0x28 0x28)
        int pid = buf.readUnsignedByte(); // Identificador do Pacote
        int length = buf.readUnsignedShort(); // Tamanho do pacote
        int sequence = buf.readUnsignedShort(); // Sequência do pacote

        if (pid == 0x12) { // Location Package
            Position position = new Position();
            position.setProtocol(getProtocolName());

            long time = buf.readUnsignedInt();
            position.setFixTime(new java.util.Date(time * 1000));

            double latitude = buf.readInt() / 1800000.0;
            double longitude = buf.readInt() / 1800000.0;
            position.setLatitude(latitude);
            position.setLongitude(longitude);

            int altitude = buf.readShort();
            position.setAltitude((double) altitude);

            int speed = buf.readUnsignedShort();
            position.setSpeed(speed * 0.539957); // Converter para nós

            int course = buf.readUnsignedShort();
            position.setCourse(course);

            return position;
        }

        return null;
    }
}
