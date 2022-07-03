/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

@StorageName("tc_servers")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Server extends ExtendedModel implements UserRestrictions {

    private boolean registration;

    public boolean getRegistration() {
        return registration;
    }

    public void setRegistration(boolean registration) {
        this.registration = registration;
    }

    private boolean readonly;

    @Override
    public boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    private boolean deviceReadonly;

    @Override
    public boolean getDeviceReadonly() {
        return deviceReadonly;
    }

    public void setDeviceReadonly(boolean deviceReadonly) {
        this.deviceReadonly = deviceReadonly;
    }

    private String map;

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    private String bingKey;

    public String getBingKey() {
        return bingKey;
    }

    public void setBingKey(String bingKey) {
        this.bingKey = bingKey;
    }

    private String mapUrl;

    public String getMapUrl() {
        return mapUrl;
    }

    public void setMapUrl(String mapUrl) {
        this.mapUrl = mapUrl;
    }

    private String overlayUrl;

    public String getOverlayUrl() {
        return overlayUrl;
    }

    public void setOverlayUrl(String overlayUrl) {
        this.overlayUrl = overlayUrl;
    }

    private double latitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    private double longitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    private int zoom;

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    private boolean twelveHourFormat;

    public boolean getTwelveHourFormat() {
        return twelveHourFormat;
    }

    public void setTwelveHourFormat(boolean twelveHourFormat) {
        this.twelveHourFormat = twelveHourFormat;
    }

    private boolean forceSettings;

    public boolean getForceSettings() {
        return forceSettings;
    }

    public void setForceSettings(boolean forceSettings) {
        this.forceSettings = forceSettings;
    }

    private String coordinateFormat;

    public String getCoordinateFormat() {
        return coordinateFormat;
    }

    public void setCoordinateFormat(String coordinateFormat) {
        this.coordinateFormat = coordinateFormat;
    }

    private boolean limitCommands;

    @Override
    public boolean getLimitCommands() {
        return limitCommands;
    }

    public void setLimitCommands(boolean limitCommands) {
        this.limitCommands = limitCommands;
    }

    private boolean disableReports;

    @Override
    public boolean getDisableReports() {
        return disableReports;
    }

    public void setDisableReports(boolean disableReports) {
        this.disableReports = disableReports;
    }

    private String poiLayer;

    public String getPoiLayer() {
        return poiLayer;
    }

    public void setPoiLayer(String poiLayer) {
        this.poiLayer = poiLayer;
    }

    private String announcement;

    public String getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    @QueryIgnore
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    private boolean emailEnabled;

    @QueryIgnore
    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    @QueryIgnore
    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    private boolean geocoderEnabled;

    @QueryIgnore
    public void setGeocoderEnabled(boolean geocoderEnabled) {
        this.geocoderEnabled = geocoderEnabled;
    }

    @QueryIgnore
    public boolean getGeocoderEnabled() {
        return geocoderEnabled;
    }

    private long[] storageSpace;

    @QueryIgnore
    public long[] getStorageSpace() {
        return storageSpace;
    }

    @QueryIgnore
    public void setStorageSpace(long[] storageSpace) {
        this.storageSpace = storageSpace;
    }

}
