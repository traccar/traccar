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
package org.traccar.http;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;

public class DeviceServlet extends HttpServlet {
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String command = req.getPathInfo();

        if (command == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else if (command.equals("/get")) {
            get(req, resp);
        } else if (command.equals("/add")) {
            add(req, resp);
        } else if (command.equals("/update")) {
            update(req, resp);
        } else if (command.equals("/remove")) {
            remove(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        long userId = (Long) req.getSession().getAttribute(MainServlet.USER_ID);
        
        JsonObjectBuilder result = Json.createObjectBuilder();
        
        try {
            result.add("success", true);
            result.add("data", Context.getDataManager().getDevices(userId));
        } catch(SQLException error) {
            result.add("success", false);
            result.add("error", error.getMessage());
        }
        
        resp.getWriter().println(result.build().toString());
    }
    
    private void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        long userId = (Long) req.getSession().getAttribute(MainServlet.USER_ID);
        
        JsonReader jsonReader = Json.createReader(req.getReader());
        JsonObject jsonObject = jsonReader.readObject();
        Device device = new Device();
        try {
            device.fromJson(jsonObject);
        } catch (ParseException error) {
            Log.warning(error);
        }
        
        JsonObjectBuilder result = Json.createObjectBuilder();
        
        try {
            result.add("success", true);
            Context.getDataManager().addDevice(device);
            Context.getDataManager().linkDevice(userId, device.getId());
        } catch(SQLException error) {
            result.add("success", false);
            result.add("error", error.getMessage());
        }
        
        resp.getWriter().println(result.build().toString());
    }
    
    private void update(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }
    
    private void remove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    }

}
