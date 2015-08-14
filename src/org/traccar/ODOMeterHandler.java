/**
 * 
 */
package org.traccar;

import org.traccar.helper.DistanceCalculator;
import org.traccar.helper.Log;
import org.traccar.model.Position;

/**
 * <p>
 *  ODO Meter handler
 * </p>
 * 
 * @author Amila Silva
 *
 */
public class ODOMeterHandler extends BaseDataHandler {

	public ODOMeterHandler() {
		Log.debug("System based ODO meter calculation enabled for all devices");
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
			double odoMeter = distance + last.getOdoMeter();
//			Log.debug("::: Device Course : " + position.getDeviceId()
//					+ ", Distance :" + distance + "m, Odometer :" + odoMeter
//					+ " m");
			position.setOdoMeter(odoMeter);
		}
		return position;
	}

	@Override
	protected Position handlePosition(Position position) {
		return calculateDistance(position);
	}

}
