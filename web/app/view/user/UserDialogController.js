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

Ext.define('Traccar.view.user.UserDialogController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.userDialog',

    init: function() {
        if (Traccar.app.getUser().get('admin')) {
            this.lookupReference('adminField').setDisabled(false);
        }
    },

    onSaveClick: function(button) {
        var dialog = button.up('window').down('form');
        dialog.updateRecord();
        var record = dialog.getRecord();
        if (record === Traccar.app.getUser()) {
            record.save();
        } else {
            var store = Ext.getStore('Users');
            if (record.phantom) {
                store.add(record);
            }
            store.sync({
                failure: function(batch) {
                    store.rejectChanges(); // TODO
                    Traccar.ErrorManager.check(true, batch.exceptions[0].getResponse());
                }
            });
        }
        button.up('window').close();
    },

    onCancelClick: function(button) {
        button.up('window').close();
    }

});
