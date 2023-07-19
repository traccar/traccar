/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network {

    public Network() {
    }

    public Network(CellTower cellTower) {
        addCellTower(cellTower);
    }

    public Network(WifiAccessPoint wifiAccessPoint) {
        addWifiAccessPoint(wifiAccessPoint);
    }

    private Integer homeMobileCountryCode;

    public Integer getHomeMobileCountryCode() {
        return homeMobileCountryCode;
    }

    public void setHomeMobileCountryCode(Integer homeMobileCountryCode) {
        this.homeMobileCountryCode = homeMobileCountryCode;
    }

    private Integer homeMobileNetworkCode;

    public Integer getHomeMobileNetworkCode() {
        return homeMobileNetworkCode;
    }

    public void setHomeMobileNetworkCode(Integer homeMobileNetworkCode) {
        this.homeMobileNetworkCode = homeMobileNetworkCode;
    }

    private String radioType = "gsm";

    public String getRadioType() {
        return radioType;
    }

    public void setRadioType(String radioType) {
        this.radioType = radioType;
    }

    private String carrier;

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    private Boolean considerIp = false;

    public Boolean getConsiderIp() {
        return considerIp;
    }

    public void setConsiderIp(Boolean considerIp) {
        this.considerIp = considerIp;
    }

    private Collection<CellTower> cellTowers;

    public Collection<CellTower> getCellTowers() {
        return cellTowers;
    }

    public void setCellTowers(Collection<CellTower> cellTowers) {
        this.cellTowers = cellTowers;
    }

    public void addCellTower(CellTower cellTower) {
        if (cellTowers == null) {
            cellTowers = new ArrayList<>();
        }
        cellTowers.add(cellTower);
    }

    private Collection<WifiAccessPoint> wifiAccessPoints;

    public Collection<WifiAccessPoint> getWifiAccessPoints() {
        return wifiAccessPoints;
    }

    public void setWifiAccessPoints(Collection<WifiAccessPoint> wifiAccessPoints) {
        this.wifiAccessPoints = wifiAccessPoints;
    }

    public void addWifiAccessPoint(WifiAccessPoint wifiAccessPoint) {
        if (wifiAccessPoints == null) {
            wifiAccessPoints = new ArrayList<>();
        }
        wifiAccessPoints.add(wifiAccessPoint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Network network = (Network) o;
        return Objects.equals(homeMobileCountryCode, network.homeMobileCountryCode)
                && Objects.equals(homeMobileNetworkCode, network.homeMobileNetworkCode)
                && Objects.equals(radioType, network.radioType)
                && Objects.equals(carrier, network.carrier)
                && Objects.equals(cellTowers, network.cellTowers)
                && Objects.equals(wifiAccessPoints, network.wifiAccessPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                homeMobileCountryCode, homeMobileNetworkCode, radioType, carrier, cellTowers, wifiAccessPoints);
    }

}
