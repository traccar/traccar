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
    dateFormat: 'Y-m-d',
    weekStartDay: 1,

    deviceWidth: 350,

    reportHeight: 250,
    reportTime: 100,

    mapDefaultLat: 51.507222,
    mapDefaultLon: -0.1275,
    mapDefaultZoom: 6,

    mapRouteColor: 'rgba(21, 127, 204, 1.0)',
    mapRouteWidth: 5,

    mapArrowStrokeColor: 'rgba(50, 50, 50, 1.0)',
    mapArrowStrokeWidth: 2,

    mapTextColor: 'rgba(50, 50, 50, 1.0)',
    mapTextStrokeColor: 'rgba(255, 255, 255, 1.0)',
    mapTextStrokeWidth: 2,
    mapTextOffset: 10,
    mapTextFont: 'bold 12px sans-serif',

    mapColorOnline: 'rgba(77, 250, 144, 1.0)',
    mapColorUnknown: 'rgba(250, 190, 77, 1.0)',
    mapColorOffline: 'rgba(255, 84, 104, 1.0)',
    mapColorReport: 'rgba(21, 127, 204, 1.0)',

    mapRadiusNormal: 9,
    mapRadiusSelected: 14,

    mapMaxZoom: 19,
    mapDelay: 500
});
