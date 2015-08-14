/**
 * 
 */
package org.traccar;

import org.traccar.helper.DistanceCalculator;
import org.traccar.helper.Log;
import org.traccar.model.Position;

/**
 * <p>
 * Odometer - total mileage calculation handler
 * </p>
 * 
 * @author Amila Silva
 *
 */
public class OdometerHandler extends BaseDataHandler {

    public OdometerHandler() {
        Log.debug("System based odometer calculation enabled for all devices");
    }

    private Position getLastPosition(long deviceId) {
        if (Context.getConnectionManager() != null) {
            return Context.getConnectionManager().getLastPosition(deviceId);
        }
        return null;
    }

    private Position calculateDistance(Position position) {
        Position last = getLastPosition(position.getDeviceId());
        if (last != null) {
            double distance = DistanceCalculator.distance(
                    position.getLatitude(), position.getLongitude(),
                    last.getLatitude(), last.getLongitude());
            distance = Math.round((distance) * 100.0) / 100.0;
            double odometer = distance + last.getOdometer();
            position.setOdometer(odometer);
        }
        return position;
    }

    @Override
    protected Position handlePosition(Position position) {
        return calculateDistance(position);
    }

}
