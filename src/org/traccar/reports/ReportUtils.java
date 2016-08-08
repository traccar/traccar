package org.traccar.reports;

import java.util.ArrayList;
import java.util.Collection;

import org.traccar.Context;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>();
        result.addAll(deviceIds);
        for (long groupId : groupIds) {
            result.addAll(Context.getPermissionsManager().getGroupDevices(groupId));
        }
        return result;
    }

}
