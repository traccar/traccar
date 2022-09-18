/*
 * 2020 - NDTP v6 Protocol Encoder
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

public class NDTPv6ProtocolEncoder extends BaseProtocolEncoder {

  public NDTPv6ProtocolEncoder(Protocol protocol) {
    super(protocol);
  }

  private ByteBuf encodeCommand(String commandString) {
    ByteBuf buffer = Unpooled.buffer();
    buffer.writeBytes(commandString.getBytes(StandardCharsets.US_ASCII));
    return buffer;
  }

  @Override
  protected Object encodeCommand(Command command) {
    switch (command.getType()) {
      case Command.TYPE_IDENTIFICATION:
        return encodeCommand("BB+IDNT");
      case Command.TYPE_REBOOT_DEVICE:
        return encodeCommand("BB+RESET");
      case Command.TYPE_POSITION_SINGLE:
        return encodeCommand("BB+RRCD");
      default:
        return null;
    }
  }
}
