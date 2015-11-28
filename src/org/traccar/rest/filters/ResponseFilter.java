package org.traccar.rest.filters;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.util.CharsetUtil;
import org.traccar.Context;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static org.traccar.web.BaseServlet.*;

/**
 * Created by niko on 11/27/15.
 */
@Provider
public class ResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        headers.putSingle(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS, ALLOW_HEADERS_VALUE);
        headers.putSingle(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_METHODS_VALUE);
        headers.putSingle(HttpHeaders.Names.CONTENT_TYPE, APPLICATION_JSON);
        headers.putSingle(HttpHeaders.Names.CONTENT_ENCODING, CharsetUtil.UTF_8.name());

        String origin = requestContext.getHeaderString(HttpHeaders.Names.ORIGIN);
        String allowed = Context.getConfig().getString("web.origin");
        if (allowed == null) {
            headers.putSingle(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_VALUE);
        } else if (allowed.contains(origin)) {
            headers.putSingle(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }
    }
}