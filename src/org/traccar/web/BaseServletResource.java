/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.helper.CommandCall;
import org.traccar.helper.Clazz;

/**
 *
 * @author Rafael
 */
public abstract class BaseServletResource<T> extends BaseServlet {

    private final Class<T> clazz = Clazz.getGenericArgumentType(getClass());

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    public static final String PATH_PARAM_ID = "/\\d";
    public static final String SLASH = "/";
    public static final String VOID = "";

    @Override
    protected String getCommand(HttpServletRequest req) {
        String command = req.getPathInfo();
        if (command == null || command.matches(PATH_PARAM_ID)) {
            switch (req.getMethod()) {
                case GET:
                    command = "/get";
                    break;
                case POST:
                    command = "/add";
                    break;
                case PUT:
                    command = "/update";
                    break;
                case DELETE:
                    command = "/remove";
                    break;
                default:
                    command = "";
            }
        }
        return command;
    }

    protected String getPathParamId(String pathInfo) {
        if (pathInfo != null && pathInfo.matches(PATH_PARAM_ID)) {
            return pathInfo.replaceAll(SLASH, VOID);
        }
        return null;
    }

    protected abstract void get(HttpServletRequest req, HttpServletResponse resp) throws Exception;

    protected void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        add(req, resp, null);
    }

    protected void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        update(req, resp, null);
    }

    protected void remove(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        remove(req, resp, null);
    }

    protected void add(HttpServletRequest req, HttpServletResponse resp, CommandCall<T> commandCall) throws Exception {
        if (commandCall != null) {
            commandCall.before();
        }

        T entity = JsonConverter.objectFromJson(req.getReader(), this.clazz);
        long userId = getUserId(req);
        if (commandCall != null) {
            commandCall.setUserId(userId);
            commandCall.setEntity(entity);
            commandCall.check();
        }

        Context.getDataManager().add(entity);

        long entityId = Clazz.getId(entity);
        Context.getDataManager().link(this.clazz, userId, entityId);

        if (commandCall != null) {
            commandCall.after();
        }

        sendResponse(resp.getWriter(), JsonConverter.objectToJson(entity));
    }

    protected void update(HttpServletRequest req, HttpServletResponse resp,
                          CommandCall<T> commandCall) throws Exception {
        if (commandCall != null) {
            commandCall.before();
        }

        T entity = JsonConverter.objectFromJson(req.getReader(), this.clazz);
        String entityId = getPathParamId(req.getPathInfo());
        if (entityId != null) {
            Clazz.setId(entity, Long.parseLong(entityId));
        }
        long userId = getUserId(req);

        if (commandCall != null) {
            commandCall.setUserId(userId);
            commandCall.setEntity(entity);
            commandCall.check();
        }

        Context.getDataManager().update(entity);

        if (commandCall != null) {
            commandCall.after();
        }

        sendResponse(resp.getWriter(), true);
    }

    protected void remove(HttpServletRequest req, HttpServletResponse resp,
                          CommandCall<T> commandCall) throws Exception {
        if (commandCall != null) {
            commandCall.before();
        }

        T entity = Clazz.newInstance(this.clazz);
        String entityId = getPathParamId(req.getPathInfo());
        if (entityId != null) {
            Clazz.setId(entity, Long.parseLong(entityId));
        } else {
            entity = JsonConverter.objectFromJson(req.getReader(), this.clazz);
        }
        long userId = getUserId(req);

        if (commandCall != null) {
            commandCall.setUserId(userId);
            commandCall.setEntity(entity);
            commandCall.check();
        }

        Context.getDataManager().remove(entity);

        if (commandCall != null) {
            commandCall.after();
        }

        sendResponse(resp.getWriter(), true);
    }

}
