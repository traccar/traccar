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

Ext.define('Traccar.view.BaseEditDialogController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.baseEditDialog',

    onSaveClick: function (button) {
        var dialog, store, record;
        dialog = button.up('window').down('form');
        dialog.updateRecord();
        record = dialog.getRecord();
        store = record.store;
        if (store) {
            if (record.phantom) {
                store.add(record);
            }
            store.sync({
                success: function () {
                    store.reload(); // workaround for selection problem
                },
                failure: function (batch) {
                    store.rejectChanges();
                    Traccar.ErrorManager.check(true, batch.exceptions[0].getResponse());
                }
            });
        } else {
            record.save();
        }
        button.up('window').close();
    }
});
