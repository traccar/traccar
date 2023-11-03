package org.traccar.database;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.*;
import org.traccar.BaseProtocol;
import org.traccar.ServerManager;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.QueuedCommand;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;
import org.traccar.sms.SmsManager;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CommandsManagerTest {

    @InjectMocks
    private CommandsManager commandsManager;
    @Mock
    private Storage storage;
    @Mock
    private ServerManager serverManager;
    @Mock
    private SmsManager smsManager;
    @Mock
    private ConnectionManager connectionManager;
    @Mock
    private BroadcastService broadcastService;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private DeviceSession deviceSession;
    @Mock
    private QueuedCommand queuedCommand;

    @Mock
    private BaseProtocol CarcellProtocol;
    @Mock
    private Config config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static Command createCommand(long deviceId, boolean textChannel) {
        Command command = new Command();
        command.setDeviceId(deviceId);
        command.setTextChannel(textChannel);
        command.setType("custom");
        return command;
    }

    private static Device createDevice(long positionId, String phone, long uniqueId) {
        Device device = new Device();
        device.setId(uniqueId);
        device.setPositionId(positionId);
        device.setPhone(phone);
        return device;
    }

    private static Position createPosition(long PositionId, String protocol) {
        Position position = new Position();
        position.setProtocol(protocol);
        position.setId(PositionId);
        return position;
    }

    @Test
    public void sendCommand_SMSNotConfigured() {
        CommandsManager commandsManager = new CommandsManager(storage, serverManager, null, connectionManager, broadcastService, notificationManager);
        Command command = createCommand(1, true);
        assertThrows(RuntimeException.class, () -> commandsManager.sendCommand(command));
    }

    @Test
    public void sendCommand_sendTextCommand() throws Exception {
        Command command = createCommand(1, true);
        Device device = createDevice(1, "samsung", 1);
        Position position = createPosition(1, "carrell");
        BaseProtocol protocol = mock(BaseProtocol.class);
        Mockito.when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        Mockito.when(storage.getObject(eq(Position.class), any(Request.class))).thenReturn(position);
        Mockito.doReturn(CarcellProtocol).when(serverManager).getProtocol(position.getProtocol());
        doNothing().when(protocol).sendTextCommand(device.getPhone(), command);
        assertNull(commandsManager.sendCommand(command));
    }

    @Test
    public void sendCommand_sendMessage() throws Exception {
        Command command = createCommand(1, true);
        Device device = createDevice(1, "samsung", 1);
        Position position = createPosition(1, "carrell");
        Mockito.when(storage.getObject(eq(Device.class), any(Request.class))).thenReturn(device);
        Mockito.when(storage.getObject(eq(Position.class), any(Request.class))).thenReturn(null);
        Mockito.doReturn(CarcellProtocol).when(serverManager).getProtocol(position.getProtocol());
        doNothing().when(smsManager).sendMessage(device.getPhone(), command.getString(Command.KEY_DATA), true);
        commandsManager.sendCommand(command);
        verify(smsManager, times(1)).sendMessage(device.getPhone(), command.getString(Command.KEY_DATA), true);
        assertNull(commandsManager.sendCommand(command));
    }


    @Test
    public void sendCommand_deviceSessionSendCommand() throws Exception {
        Command command = createCommand(1, false);
        DeviceSession deviceSession = mock(DeviceSession.class);
        Mockito.when(connectionManager.getDeviceSession(1)).thenReturn(deviceSession);
        Mockito.when(deviceSession.supportsLiveCommands()).thenReturn(true);
        commandsManager.sendCommand(command);
        verify(deviceSession, times(1)).sendCommand(command);
        assertNull(commandsManager.sendCommand(command));
    }

    @Test
    public void sendCommand_queuedCommand() throws Exception {
        Command command = createCommand(1, false);
        Mockito.when(connectionManager.getDeviceSession(1)).thenReturn(null);
        Mockito.when(deviceSession.supportsLiveCommands()).thenReturn(true);
        doNothing().when(queuedCommand).setId(1);
        assertNotNull(commandsManager.sendCommand(command));
    }

}
