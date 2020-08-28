package org.traccar.notificators;

import org.traccar.Context;

public class NotificatorTraccar extends NotificatorFirebase {

    public NotificatorTraccar() {
        super(
                "https://www.traccar.org/push/",
                Context.getConfig().getString("notificator.traccar.key"));
    }

}
