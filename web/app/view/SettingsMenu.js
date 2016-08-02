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

Ext.define('Traccar.view.SettingsMenu', {
    extend: 'Ext.button.Button',
    xtype: 'settingsMenu',

    requires: [
        'Traccar.view.SettingsMenuController'
    ],

    glyph: 'xf013@FontAwesome',
    text: getString('settingsTitle'),

    menu: {
        controller: 'settings',

        items: [{
            text: getString('settingsUser'),
            handler: 'onUserClick'
        }, {
            text: getString('settingsGroups'),
            handler: 'onGroupsClick'
        }, {
            text: getString('sharedGeofences'),
            handler: 'onGeofencesClick'
        }, {
            text: getString('settingsServer'),
            hidden: true,
            handler: 'onServerClick',
            reference: 'settingsServerButton'
        }, {
            text: getString('settingsUsers'),
            hidden: true,
            handler: 'onUsersClick',
            reference: 'settingsUsersButton'
        }, {
            text: getString('sharedNotifications'),
            handler: 'onNotificationsClick'
        }, {
            text: getString('loginLogout'),
            handler: 'onLogoutClick'
        }]
    }
});
