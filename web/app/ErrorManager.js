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

Ext.define('Traccar.ErrorManager', {
    singleton: true,

    check: function (success, response) {
        var result;
        if (success) {
            result = Ext.decode(response.responseText);
            if (result.success || result.error === undefined) {
                return true;
            } else {
                Ext.Msg.alert(Strings.errorTitle, result.error);
                return false;
            }
        } else {
            if (response.statusText) {
                Ext.Msg.alert(Strings.errorTitle, response.statusText);
            } else {
                Ext.Msg.alert(Strings.errorTitle, response.status.toString()); // TODO: text message
            }
            return false;
        }
    },

    error: function (message) {
        Ext.Msg.alert(Strings.errorTitle, message);
    }

});
