/*
 * 2020 - NDTP v6 Protocol Decoder
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

public class NDTPv6ProtocolDecoder extends BaseProtocolDecoder {

    public NDTPv6ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final byte[] SIGNATURE = {0x7E, 0x7E};

    private static final int NPL_FLAG_CRC = 2;
    private static final int NPH_RESULT_OK = 0x00000000;
    private static final int NPL_TYPE_NPH = 2;
    private static final int NPL_ADDRESS_SERVER = 0;

    /* common packets for all services */
    private static final int NPH_RESULT = 0;

    /* NPH service types */
    private static final int NPH_SRV_GENERIC_CONTROLS = 0;
    private static final int NPH_SRV_NAVDATA = 1;

    /* NPH_SRV_GENERIC_CONTROLS packets */
    private static final int NPH_SGC_RESULT = NPH_RESULT;
    private static final int NPH_SGC_CONN_REQUEST = 100;

    /* NPH_SRV_NAVDATA packets */
    private static final int NPH_SND_RESULT = NPH_RESULT;

    private static void sendResultResponse(
            Channel channel,
            short serviceId,
            int requestId,
            int nphSendResult,
            int nphResult
    ) {
        // Формирование пакета данных
        byte[] serviceIdBytes = ByteBuffer
                .allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(serviceId)
                .array();
        byte[] nphSendResultBytes = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(nphSendResult)
                .array();
        byte[] requestIdBytes = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(requestId)
                .array();
        byte[] nphResultBytes = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(nphResult)
                .array();

        byte[] allByteArray = new byte[serviceIdBytes.length +
                requestIdBytes.length +
                nphSendResultBytes.length +
                nphResultBytes.length];

        System.arraycopy(serviceIdBytes, 0, allByteArray, 0, serviceIdBytes.length);
        System.arraycopy(
                nphSendResultBytes,
                0,
                allByteArray,
                serviceIdBytes.length,
                nphSendResultBytes.length
        );
        System.arraycopy(
                requestIdBytes,
                0,
                allByteArray,
                serviceIdBytes.length + nphSendResultBytes.length,
                requestIdBytes.length
        );
        System.arraycopy(
                nphResultBytes,
                0,
                allByteArray,
                serviceIdBytes.length + requestIdBytes.length + nphSendResultBytes.length,
                nphResultBytes.length
        );

        // ПАКЕТ ОТВЕТА КЛИЕНТУ
        ByteBuf response = Unpooled.buffer();
        // NPL
        response.writeBytes(SIGNATURE);
        response.writeShortLE(allByteArray.length); // Размер данных
        response.writeShortLE(NPL_FLAG_CRC); // Флаги

        response.writeShort(
                Checksum.crc16(Checksum.CRC16_MODBUS, ByteBuffer.wrap(allByteArray))
        ); // CRC
        response.writeByte(NPL_TYPE_NPH); // Тип
        response.writeIntLE(NPL_ADDRESS_SERVER); // peer_address
        response.writeShortLE(0); // request_id

        response.writeBytes(allByteArray);

        channel.writeAndFlush(
                new NetworkMessage(response, channel.remoteAddress())
        );
    }

    private static final short MAIN_NAV_DATA = 0;
    private static final short ADDITIONAL_NAV_DATA = 2;

    private void decodeData(ByteBuf buf, Position position, int dataType) {
        if (dataType == NPH_SRV_NAVDATA) {
            short cellType;
            short cellNumber;

            cellType = buf.readUnsignedByte(); // Тип ячейки
            cellNumber = buf.readUnsignedByte(); // Номер ячейки
            if (cellType == MAIN_NAV_DATA && (cellNumber == 0 || cellNumber == 1)) {
                position.setTime(new Date(buf.readUnsignedIntLE() * 1000)); // Значение реального времени unix
                position.setLongitude(buf.readIntLE() / 10000000.0); // Долгота в градусах, умноженная на 10 000 000
                position.setLatitude(buf.readIntLE() / 10000000.0); // Широта в градусах, умноженная на 10 000 000

                short flags = buf.readUnsignedByte(); // Достоверность навигационных данных:
                // bit7 - достоверность нав. данных (1 - достоверны, 0 – нет);
                // bit6 - полушарие долготы (1 – E, 0 – W);
                // bit5 - полушарие широты (1 – N, 0 – S);
                // bit4 - флаг работы от встроенного аккумулятора;
                // bit3 – флаг первоначального включения;
                // bit2 – состояние SOS (1 – SOS, 0 – нет SOS);
                // bit1 – флаг тревожной информации (один из параметров
                // находится в диапазоне тревоги)
                position.setValid(BitUtil.check(flags, 7));
                if (BitUtil.check(flags, 1)) {
                    position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
                }
                position.set(Position.KEY_BATTERY, buf.readUnsignedByte() * 20); // Напряжение батареи, 1бит = 20мВ
                position.set(Position.KEY_OBD_SPEED, buf.readUnsignedShortLE()); // Средняя скорость за период в км/ч
                position.setSpeed(buf.readUnsignedShortLE() / 1.85); // Максимальная скорость за период в км/ч

                int course = buf.readUnsignedShortLE(); // Направление движения
                position.setCourse(course);

                position.set(Position.KEY_DISTANCE, buf.readUnsignedShortLE()); // Пройденный путь, м
                position.setAltitude(buf.readShortLE()); // Высота над уровнем моря в метрах (-18000 - +18000)
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte()); // Количество видимых спутников
                position.set(Position.KEY_PDOP, buf.readUnsignedByte()); // PDOP
            }
            cellType = buf.readUnsignedByte(); // Тип ячейки
            cellNumber = buf.readUnsignedByte(); // Номер ячейки
            if (cellType == ADDITIONAL_NAV_DATA && cellNumber == 0) {
                int analogIn1 = buf.readUnsignedShortLE(); // Значение 0 аналогового входа в 16 битном формате
                //(analogIn1 - отражает напряжение на батерии для радиоборта)
                int analogIn2 = buf.readUnsignedShortLE(); // Значение 1 аналогового входа в 16 битном формате
                int analogIn3 = buf.readUnsignedShortLE(); // Значение 2 аналогового входа в 16 битном формате
                int analogIn4 = buf.readUnsignedShortLE(); // Значение 3 аналогового входа в 16 битном формате

                buf.readUnsignedByte(); // Значение цифровых входов
                buf.readUnsignedByte(); // Состояние дискретных выходов
                buf.readUnsignedShortLE(); // Количество импульсов на дискретном входе 0 с предыдущей нав. отметки
                buf.readUnsignedShortLE(); // Количество импульсов на дискретном входе 1 с предыдущей нав. отметки
                buf.readUnsignedShortLE(); // Количество импульсов на дискретном входе 2 с предыдущей нав. отметки
                buf.readUnsignedShortLE(); // Количество импульсов на дискретном входе 3 с предыдущей нав. отметки
                buf.readUnsignedIntLE(); // Длина трека с момента первого включения

                position.set(Position.KEY_ANTENNA, buf.readUnsignedByte()); // Сила GSM сигнала
                position.set(Position.KEY_GPS, buf.readUnsignedByte()); // Состояние GPRS подключения
                position.set(Position.KEY_ACCELERATION, buf.readUnsignedByte()); // Акселерометр - энергия
                position.set(Position.KEY_POWER, buf.readUnsignedByte() * 200); // Напряжение борт. сети (1бит - 200мв)

                position.set(Position.PREFIX_ADC + 1, analogIn1 * 1);
                position.set(Position.PREFIX_ADC + 2, analogIn2 * 1);
                position.set(Position.PREFIX_ADC + 3, analogIn3 * 1);
                position.set(Position.PREFIX_ADC + 4, analogIn4 * 1);

                // Расчет уровня батареи
                // float Voltage = 5 / 4096 * analogIn1; // Вольтаж

                float batteryLevel = (analogIn1 - 3600) / 6;

                if (batteryLevel > 100) {
                    batteryLevel = 100;
                }
                if (batteryLevel < 0) {
                    batteryLevel = 0;
                }

                position.set(Position.KEY_BATTERY_LEVEL, batteryLevel);
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel,
            SocketAddress remoteAddress,
            Object msg
    )
            throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        // Заголовок NPL
        buf.skipBytes(2); // Сигнатура (7e 7e)
        buf.readUnsignedShortLE(); // Размер данных (nph + размер массива данных)
        buf.readUnsignedShortLE(); // Флаги соединения (2 - проверять crc)
        buf.readUnsignedShortLE(); // CRC (Modbus)
        buf.readUnsignedByte(); // Тип пакета (nph)
        buf.readUnsignedIntLE(); // Адрес участника соединения
        buf.readUnsignedShortLE(); // Идентификатор NPL

        // Заголовок NPH
        int serviceId = buf.readUnsignedShortLE(); // Идентификатор услуги (NPH_SRV_NAVDATA) service_id
        int serviceType = buf.readUnsignedShortLE(); // Тип пакета
        buf.readUnsignedShortLE(); // Флаг (1 - требуется подтверждение) NPH_FLAG_REQUEST
        long requestId = buf.readUnsignedIntLE(); // Идентификатор nph

        if (
                deviceSession == null &&
                        serviceId == NPH_SRV_GENERIC_CONTROLS &&
                        serviceType == NPH_SGC_CONN_REQUEST
        ) { // Регистрация устройства
            buf.readUnsignedShortLE(); // Версия протокола NDTP (старший номер)
            buf.readUnsignedShortLE(); // Версия протокола NDTP (младший номер)
            buf.readUnsignedShortLE(); // Опции соединения (connection_flags)
            // Определяет настройки соединения,
            // которые будут использоваться после установки соединения.
            // На данный момент их две:
            //  - бит0: шифровать пакеты (0 - нет, 1 — да)
            //  - бит1: рассчитывать CRC пакетов (0 - нет, 1 — да)
            //  - бит2: подключение симулятора (0 — подключается обычный клиент,
            //                                  1 подключается симулятор)
            //  - бит3:  тип алгоритма шифрования.
            //  -            0 - blowfish
            //  -            1 – ГОСТ
            //  - бит8: наличие поля IMEI (0 - нет, 1 — да)
            //  - бит9: наличие поля IMSI (0 - нет, 1 — да)
            //  - остальные биты не используются.
            int deviceId = buf.readUnsignedShortLE();
            Position position = new Position(getProtocolName());
            deviceSession =
                    getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
            position.setDeviceId(deviceSession.getDeviceId());

            if (channel != null) {
                sendResultResponse(
                        channel,
                        (short) serviceId,
                        (int) requestId,
                        NPH_SND_RESULT,
                        NPH_RESULT_OK
                );
            }

            position.set(Position.KEY_RESULT, String.valueOf(NPH_SGC_RESULT));
            position.setTime(new Date());
            getLastLocation(position, new Date());
            position.setValid(false);

            return position;
        }

        if (serviceId == NPH_SRV_NAVDATA) {
            deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (channel != null) {
                sendResultResponse(
                        channel,
                        (short) serviceId,
                        (int) requestId,
                        NPH_SND_RESULT,
                        NPH_RESULT_OK
                );
            }

            decodeData(buf, position, NPH_SRV_NAVDATA);

            return position;
        }

        return null;
    }
}
