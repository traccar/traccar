/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package org.traccar.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class AdvancedConnection {

    /**
     * Database connection
     */
    private Connection connection;
    
    /**
     * Connection attributes
     */
    private String url;
    private String user;
    private String password;
    
    public AdvancedConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }
    
    public final void reset() throws SQLException {
        if (user != null && password != null) {
            connection = DriverManager.getConnection(url, user, password);
        } else {
            connection = DriverManager.getConnection(url);
        }
    }
    
    public Connection getInstance() throws SQLException {
        if (connection == null) {
            reset();
        }
        return connection;
    }

}
