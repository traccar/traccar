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
    }
});
