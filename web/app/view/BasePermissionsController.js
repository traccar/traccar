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

Ext.define('Traccar.view.BasePermissionsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.basePermissionsController',

    init: function () {
        var params = {}, linkStoreName, storeName;
        params[this.getView().baseObjectName] = this.getView().baseObject;
        linkStoreName = this.getView().linkStoreName;
        storeName = this.getView().storeName;
        linkStoreName = (typeof linkStoreName === 'undefined') ? storeName : linkStoreName;
        this.getView().setStore(Ext.getStore(storeName));
        this.getView().getStore().load({
            scope: this,
            callback: function (records, operation, success) {
                var linkStore = Ext.create('Traccar.store.' + linkStoreName);
                linkStore.load({
                    params: params,
                    scope: this,
                    callback: function (records, operation, success) {
                        var i, index;
                        if (success) {
                            for (i = 0; i < records.length; i++) {
                                index = this.getView().getStore().getById(records[i].getId());
                                this.getView().getSelectionModel().select(index, true, true);
                            }
                        }
                    }
                });
            }
        });
    },

    onBeforeSelect: function (object, record, index) {
        var data = {};
        data[this.getView().baseObjectName] = this.getView().baseObject;
        data[this.getView().linkObjectName] = record.getId();
        Ext.Ajax.request({
            scope: this,
            url: this.getView().urlApi,
            jsonData: Ext.util.JSON.encode(data),
            callback: function (options, success, response) {
                if (!success) {
                    Traccar.app.showError(response);
                }
            }
        });
    },

    onBeforeDeselect: function (object, record, index) {
        var data = {};
        data[this.getView().baseObjectName] = this.getView().baseObject;
        data[this.getView().linkObjectName] = record.getId();
        Ext.Ajax.request({
            scope: this,
            method: 'DELETE',
            url: this.getView().urlApi,
            jsonData: Ext.util.JSON.encode(data),
            callback: function (options, success, response) {
                if (!success) {
                    Traccar.app.showError(response);
                }
            }
        });
    }
});
