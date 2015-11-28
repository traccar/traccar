package org.traccar.rest.errorhandling;

import org.traccar.rest.ResponseBuilder;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.security.AccessControlException;

/**
 * Created by niko on 11/27/15.
 */
@Provider
public class AccessControlExceptionMapper implements ExceptionMapper<AccessControlException> {

    @Override
    public Response toResponse(AccessControlException e) {
        return ResponseBuilder.getResponse(HttpServletResponse.SC_UNAUTHORIZED, e);
    }
}