package org.traccar.helper;

import javax.servlet.http.HttpServletRequest;


public final class ServletHelper {

    private ServletHelper() {
    }

    public static String retrieveRemoteAddress(HttpServletRequest request) {

        if (request != null) {
            String remoteAddress = request.getHeader("X-FORWARDED-FOR");

            if (remoteAddress != null && !remoteAddress.isEmpty()) {
                return remoteAddress.substring(0, remoteAddress.indexOf(",")); // removes the additional data
            } else {
                remoteAddress = request.getRemoteAddr();
                return remoteAddress;
            }
        } else {
            return null;
        }
    }

}
