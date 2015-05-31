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
package org.traccar.model;

public class Server implements Factory {

    @Override
    public Server create() {
        return new Server();
    }

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    private boolean registration;
    public boolean getRegistration() { return registration; }
    public void setRegistration(boolean registration) { this.registration = registration; }
    
    private double latitude;
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    private double longitude;
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    private int zoom;
    public int getZoom() { return zoom; }
    public void setZoom(int zoom) { this.zoom = zoom; }

}
