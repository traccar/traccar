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

Ext.define('Traccar.view.login.RegisterController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.register',

    onCreateClick: function() {
        var form = this.lookupReference('form');
        if (form.isValid()) {
            Ext.Ajax.request({
                scope: this,
                url: '/api/register',
                jsonData: form.getValues(),
                callback: this.onCreateReturn
            });
        }
    },
    
    onCreateReturn: function(options, success, response) {
        if (Traccar.ErrorManager.check(success, response)) {
            this.closeView();
            Ext.toast(strings.login_created);
        }
    }

});
