package org.traccar.helper;

import javax.servlet.http.HttpServletRequest;


public final class ServletHelper {


    private ServletHelper() {
    }

    public static String retrieveRemoteAddress(HttpServletRequest request) {

        if (request != null) {
            String ipAddress = request.getHeader("X-FORWARDED-FOR");

            if (ipAddress != null && !ipAddress.isEmpty()) {
                return ipAddress.substring(0, ipAddress.indexOf(",")); //Removes the additional data
            } else {
                ipAddress = request.getRemoteAddr();
                return ipAddress;
            }

        } else {
            return null;
        }
    }
}
