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

Ext.define('Traccar.Style', {
    singleton: true,

    panelPadding: 10,

    windowWidth: 640,
    windowHeight: 480,

    dateTimeFormat: 'Y-m-d H:i:s',
    timeFormat: 'H:i',
    weekStartDay: 1,

    deviceWidth: 350,

    reportHeight: 250,
    reportTime: 100,

    mapDefaultLat: 51.507222,
    mapDefaultLon: -0.1275,
    mapDefaultZoom: 6,

    mapMaxZoom: 19,
    mapSelectColor: 'rgba(0, 255, 0, 1.0)',
    mapSelectRadius: 10,
    mapReportColor: 'rgba(0, 0, 255, 1.0)',
    mapReportRadius: 5,
    mapLiveColor: 'rgba(255, 0, 0, 1.0)',
    mapLiveRadius: 7,
    mapStrokeColor: 'rgba(50, 50, 50, 1.0)',
    mapRouteWidth: 5,
    mapMarkerStroke: 2,
    mapDelay: 500
});
