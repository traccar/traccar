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

import org.traccar.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PositionServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (command.equals("/get")) {
            get(req, resp);
        }
        return true;
    }
    
    private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getPositions(
                            getUserId(req.getSession()),
                            Long.valueOf(req.getParameter("deviceId")),
                            JsonConverter.parseDate(req.getParameter("from")),
                            JsonConverter.parseDate(req.getParameter("to")))));
    }

}
