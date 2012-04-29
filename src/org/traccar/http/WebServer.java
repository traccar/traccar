/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.traccar.model.DataManager;
import org.traccar.model.Device;
import org.traccar.model.Position;

/**
 * Integrated HTTP server
 */
public class WebServer {

    private Server server;

    public class WebHandler extends AbstractHandler {

        private DataManager dataManager;

        public static final int BUFFER_SIZE = 1024;

        public WebHandler(DataManager dataManager) {
            this.dataManager = dataManager;
        }

        public void handleIndex(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
                throws IOException, ServletException {

            response.setContentType("text/html");

            InputStream in = this.getClass().getClassLoader().getResourceAsStream("index.html");
            OutputStream out = response.getOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }

        public void handleIcon(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
                throws IOException, ServletException {

            response.setContentType("image/x-icon");

            InputStream in = this.getClass().getClassLoader().getResourceAsStream("favicon.ico");
            OutputStream out = response.getOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }

        public void handleData(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
                throws IOException, ServletException {

            response.setContentType("application/json");

            PrintWriter out = response.getWriter();
            out.print("{'success':true,'results':[");

            try {
                if (target.equals("/devices.json")) {

                    Iterator<Device> i = dataManager.getDevices().iterator();
                    while (i.hasNext()) {
                        Device device = i.next();
                        out.format("{'id':%d,'imei':'%s'}",
                                device.getId(),
                                device.getImei());
                        if (i.hasNext()) out.print(",");
                    }

                } else if (target.equals("/positions.json")) {

                    String deviceId = request.getParameter("deviceId");
                    if (deviceId != null) {
                        Iterator<Position> i = dataManager.getPositions(Long.valueOf(deviceId)).iterator();
                        while (i.hasNext()) {
                            Position position = i.next();
                            out.format("{'device_id':%d,'time':'%tF %tT','valid':%b,'latitude':%f,'longitude':%f,'speed':%f,'course':%f}",
                                    position.getDeviceId(),
                                    position.getTime(), position.getTime(),
                                    position.getValid(),
                                    position.getLatitude(),
                                    position.getLongitude(),
                                    position.getSpeed(),
                                    position.getCourse());
                            if (i.hasNext()) out.print(",");
                        }
                    }

                }
            } catch (Exception error) {
                System.out.println(error.getMessage());
            }

            //" {'id': 1, 'imei': '123456789012345'} ]}");
            out.print("]}");
            out.flush();
        }

        public void handleOther(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
                throws IOException, ServletException {
            response.sendRedirect(response.encodeRedirectURL("/"));
        }

        public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
                throws IOException, ServletException
        {
            if (target.equals("/") || target.equals("/index.html")) {
                handleIndex(target, baseRequest, request, response);
            } else if (target.matches("/favicon.ico")) {
                handleIcon(target, baseRequest, request, response);
            } else if (target.matches("/.+\\.json")) {
                handleData(target, baseRequest, request, response);
            } else {
                handleOther(target, baseRequest, request, response);
            }
        }
    }

    public WebServer(Integer port, DataManager dataManager) {
        server = new Server(port);
        server.setHandler(new WebHandler(dataManager));
    }

    public void start() {
        try {
            server.start();
        } catch (Exception error) {
            System.out.println(error.getMessage());
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception error) {
            System.out.println(error.getMessage());
        }
    }

}
