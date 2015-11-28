package org.traccar.rest.errorhandling;

import org.traccar.rest.ResponseBuilder;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by niko on 11/27/15.
 */
@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(SecurityException e) {
        return ResponseBuilder.getResponse(HttpServletResponse.SC_FORBIDDEN, e);
    }
}