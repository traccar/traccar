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

Ext.define('Traccar.view.GeofenceDialog', {
    extend: 'Traccar.view.BaseDialog',

    requires: [
        'Traccar.view.GeofenceDialogController'
    ],

    controller: 'geofenceDialog',
    title: Strings.sharedGeofence,

    items: {
        xtype: 'form',
        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: Strings.sharedName
        }, {
            xtype: 'textfield',
            name: 'description',
            fieldLabel: Strings.sharedDescription
        }, {
            xtype: 'hiddenfield',
            name: 'area',
            allowBlank: false,
            reference: 'areaField'
        }]
    },

    buttons: [{
        text: Strings.sharedArea,
        glyph: 'xf21d@FontAwesome',
        handler: 'onAreaClick'
    }, {
        xtype: 'tbfill'
    }, {
        text: Strings.sharedSave,
        handler: 'onSaveClick'
    }, {
        text: Strings.sharedCancel,
        handler: 'closeView'
    }]
});
