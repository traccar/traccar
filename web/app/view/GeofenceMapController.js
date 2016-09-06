/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.define('Traccar.view.GeofenceMapController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.geofenceMap',

    requires: [
        'Traccar.GeofenceConverter'
    ],

    onSaveClick: function (button) {
        var geometry, projection;
        if (this.getView().getFeatures().getLength() > 0) {
            geometry = this.getView().getFeatures().pop().getGeometry();
            projection = this.getView().getMapView().getProjection();
            this.fireEvent('savearea', Traccar.GeofenceConverter.geometryToWkt(projection, geometry));
            button.up('window').close();
        }
    },

    onCancelClick: function (button) {
        button.up('window').close();
    },

    onTypeSelect: function (combo) {
        this.getView().removeInteraction();
        this.getView().addInteraction(combo.getValue());
    }
});
