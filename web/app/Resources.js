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

var strings =  {
    shared_loading: 'Loading...',
    
    error_title: 'Error',
    error_unknown: 'Unknown error',
    
    login_title: 'Login',
    login_name: 'Name',
    login_email: 'Email',
    login_password: 'Password',
    login_register: 'Register',
    login_login: 'Login',
    login_failed: 'Incorrect email address or password',
    login_created: 'New user has been registered',

    device_dialog: 'Device',
    device_title: 'Devices',
    device_name: 'Name',
    device_identifier: 'Identifier',
    device_remove: 'Remove device?',

    report_title: 'Reports',

    dialog_save: 'Save',
    dialog_delete: 'Delete',
    dialog_cancel: 'Cancel',

    map_title: 'Map'
};

var styles = {
    panel_padding: 10,

    device_width: 350,

    report_height: 250,

    map_center: [ -0.1275, 51.507222 ],
    map_zoom: 6,
    map_max_zoom: 16
};

Ext.define('Traccar.Resources', {
});
