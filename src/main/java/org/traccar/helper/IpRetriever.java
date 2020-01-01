package org.traccar.helper;

import javax.servlet.http.HttpServletRequest;

/**
 * Gets the client's IP address regardless of whether the server is behind a proxy server or a load balancer.
 *
 */
public final class IpRetriever
    {


        /**
         * Retrieves the client's IP address.
         * Handles the cases like whether the server is behind a proxy server or a load balancer
         * also if the request is being made by using a reverse proxy.
         *
         * @param request {@link HttpServletRequest} instance
         * @return client's IP address
         */
    public static String retrieveIP(HttpServletRequest request) {

        if(request != null){
                String ipAddress = request.getHeader("X-FORWARDED-FOR");

                if (ipAddress != null && !ipAddress.isEmpty()) {
                        return removeUnwantedData(ipAddress);
                }
                else{
                    ipAddress = request.getRemoteAddr();
                    return ipAddress;
                }

        } else return null;

    }

        /**
         * If the request uses a reverse proxy, the header value will also contain load balancer and reverse proxy server IPs
         * This method gets rid of them.
         *
         * @param ipAddress IP address value from the header
         * @return IP address of the client
         */
    private static String removeUnwantedData(String ipAddress){
        return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
        }
}
