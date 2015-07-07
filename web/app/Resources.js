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

var styles = {
    panel_padding: 10,

    window_width: 640,
    window_height: 480,

    device_width: 350,

    report_height: 250,
    report_time: 100,
    report_format: 'Y-m-d H:i:s',

    map_center: [ -0.1275, 51.507222 ],
    map_zoom: 6,
    map_max_zoom: 16,
    map_select_color: 'rgba(0, 255, 0, 1.0)',
    map_select_radius: 10,
    map_report_color: 'rgba(0, 0, 255, 1.0)',
    map_report_radius: 5,
    map_live_color: 'rgba(255, 0, 0, 1.0)',
    map_live_radius: 7,
    map_stroke_color: 'rgba(50, 50, 50, 1.0)',
    map_route_width: 5,
    map_marker_stroke: 2,
    map_delay: 500
};

Ext.define('Traccar.Resources', {
});
