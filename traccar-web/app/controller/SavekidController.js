Ext.define('Traccar.controller.SavekidController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.savekid',

    // Controlador centralizado para manejar las acciones del módulo SaveKID.
    // Este patrón facilita el mantenimiento siguiendo el estilo ExtJS de Traccar.

    onCreateChild: function () {
        var me = this;
        Ext.create('Traccar.view.savekid.SavekidChildForm', {
            title: 'Crear niño',
            listeners: {
                saved: function () {
                    me.lookup('childrenGrid').getStore().reload();
                }
            }
        }).show();
    },

    onFilterByDevice: function () {
        var field = this.lookup('deviceFilterField');
        var store = this.lookup('childrenGrid').getStore();
        var deviceId = field.getValue();
        store.load({ params: deviceId ? { deviceId: deviceId } : {} });
    },

    onViewProfile: function (grid, rowIndex) {
        var record = grid.getStore().getAt(rowIndex);
        Ext.create('Traccar.view.savekid.SavekidChildProfilePanel', {
            childRecord: record
        }).show();
    },

    onEditChild: function (grid, rowIndex) {
        var record = grid.getStore().getAt(rowIndex);
        var me = this;
        Ext.create('Traccar.view.savekid.SavekidChildForm', {
            title: 'Editar niño',
            childRecord: record,
            listeners: {
                saved: function () {
                    grid.getStore().reload();
                }
            }
        }).show();
    },

    onDeleteChild: function (grid, rowIndex) {
        var store = grid.getStore();
        var record = store.getAt(rowIndex);
        Ext.Msg.confirm('Eliminar', '¿Desea eliminar este perfil?', function (choice) {
            if (choice === 'yes') {
                Ext.Ajax.request({
                    url: '/api/savekid/children/' + record.get('id'),
                    method: 'DELETE',
                    success: function () {
                        store.reload();
                    },
                    failure: function () {
                        Ext.Msg.alert('Error', 'No fue posible eliminar el perfil.');
                    }
                });
            }
        });
    },

    onLoadHealthHistory: function () {
        var view = this.getView();
        var childId = this.lookup('historyChildField').getValue();
        var fromDate = this.lookup('historyFromField').getValue();
        var toDate = this.lookup('historyToField').getValue();

        if (!childId) {
            Ext.Msg.alert('Validación', 'Seleccione un niño para consultar el historial.');
            return;
        }

        if (fromDate && toDate && fromDate > toDate) {
            Ext.Msg.alert('Validación', 'La fecha inicial debe ser anterior a la final.');
            return;
        }

        view.setLoading(true);
        view.historyStore.setChildContext(childId, fromDate, toDate);
        view.historyStore.load({
            callback: function (records, operation, success) {
                view.setLoading(false);
                if (success) {
                    if (Ext.isFunction(view.updateHistoryVisuals)) {
                        view.updateHistoryVisuals(records || []);
                    }
                } else {
                    Ext.Msg.alert('Error', 'No fue posible obtener el historial.');
                }
            }
        });
    },

    onLoadWeeklyReport: function () {
        var view = this.getView();
        var childId = this.lookup('weeklyChildField').getValue();
        var fromDate = this.lookup('weeklyFromField').getValue();
        var toDate = this.lookup('weeklyToField').getValue();

        if (!childId) {
            Ext.Msg.alert('Validación', 'Seleccione un niño para generar el reporte.');
            return;
        }

        if (fromDate && toDate && fromDate > toDate) {
            Ext.Msg.alert('Validación', 'La fecha inicial debe ser anterior a la final.');
            return;
        }

        view.setLoading(true);
        view.historyStore.setChildContext(childId, fromDate, toDate);
        view.historyStore.load({
            callback: function (records, operation, success) {
                view.setLoading(false);
                if (success) {
                    if (Ext.isFunction(view.updateWeeklyReport)) {
                        view.updateWeeklyReport(records || []);
                    }
                } else {
                    Ext.Msg.alert('Error', 'No fue posible generar el reporte.');
                }
            }
        });
    }
});
