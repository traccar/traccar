package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.database.IdentityManager;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class DigitalPortHandler extends BaseDataHandler {

    private final IdentityManager identityManager;

    public DigitalPortHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Position handlePosition(Position position) {

        // no dp || 0 -> 0 || 1 -> 0 - clear
        if(!position.getBoolean(Position.KEY_DP2)){
            return position;
        }

        Position last = identityManager.getLastPosition(position.getDeviceId());
        //0 -> 1 - start counting
        if(null == last || !last.getBoolean(Position.KEY_DP2)) {
            position.set(Position.KEY_DP2_TIME, 0);
        }
        else { //1 -> 1 - keep counting
            long dpTime = last.getLong(Position.KEY_DP2_TIME);
            long diff = position.getFixTime().getTime() - last.getFixTime().getTime();
            position.set(Position.KEY_DP2_TIME, dpTime+diff);
        }

        return position;
    }
}
