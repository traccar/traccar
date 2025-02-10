package org.traccar.tollroute;

public interface TollRouteProvider {
    interface TollRouteProviderCallback {
        void onSuccess(TollData tollCost);

        void onFailure(Throwable e);
    }

    void getTollRoute(double latitude, double longitude, TollRouteProviderCallback callback);
}
