package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.Assert.assertEquals;

public class WatchProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        WatchProtocol protocol = new WatchProtocol();
        WatchProtocolEncoder encoder = new WatchProtocolEncoder(protocol);
        
        Command command;

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);
        //expected encoded cmd "[CS*123456789012345*0005*RESET]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030352A52455345545D"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SOS_NUMBER);
        command.set(Command.KEY_INDEX, 1);
        command.set(Command.KEY_PHONE, "123456789");
        //expected encoded cmd "[CS*123456789012345*000e*SOS1,123456789]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030652A534F53312C3132333435363738395D"), encoder.encodeCommand(null, command));

        //expected encoded cmd "[CS*123456789012345*001e*TK,#!AMR"+[some binary blob]+"]"
        //binary blob contains some testing char (e.g. some have to be escaped + some non ASCII char)
        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_VOICE_MESSAGE);
        command.set(Command.KEY_DATA, "2321414D520A7D5B5D2C2A41424344454655FFEF");                                       
        assertEquals(binary("5B43532A3132333435363738393031323334352A303031632A544B2C2321414D520A7D017D027D037D047D0541424344454655FFEF5D"), encoder.encodeCommand(null, command));
                             
        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "WORK,6-9,11-13,13-15,17-19");
        //expected encoded cmd "[CS*123456789012345*001a*WORK,6-9,11-13,13-15,17-19]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303031612A574F524B2C362D392C31312D31332C31332D31352C31372D31395D"), encoder.encodeCommand(null, command));

        command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "Europe/Amsterdam");
        //expected encoded cmd "[CS*123456789012345*0006*LZ,,+1]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030362A4C5A2C2C2B315D"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "GMT+01:30");
        //expected encoded cmd "[CS*123456789012345*0008*LZ,,+1.5]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030382A4C5A2C2C2B312E355D"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "Atlantic/Azores");
        //expected encoded cmd "[CS*123456789012345*0006*LZ,,-1]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030362A4C5A2C2C2D315D"), encoder.encodeCommand(null, command));

        command.set(Command.KEY_TIMEZONE, "GMT-11:30");
        //expected encoded cmd "[CS*123456789012345*0009*LZ,,-11.5]"
        assertEquals(binary("5B43532A3132333435363738393031323334352A303030392A4C5A2C2C2D31312E355D"), encoder.encodeCommand(null, command));

    }

}
