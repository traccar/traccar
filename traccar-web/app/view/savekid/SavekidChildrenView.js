Ext.define('Traccar.view.savekid.SavekidChildrenView', {
    extend: 'Ext.window.Window',
    xtype: 'savekid-children-view',

    requires: [
        'Traccar.store.SavekidChildrenStore',
        'Traccar.view.savekid.SavekidChildForm',
        'Traccar.view.savekid.SavekidChildProfilePanel',
        'Traccar.controller.SavekidController'
    ],

    controller: 'savekid',
    referenceHolder: true,

    width: 950,
    height: 600,
    modal: true,
    layout: 'fit',
    closeAction: 'destroy',

    initComponent: function () {
        var me = this;

        me.store = Ext.create('Traccar.store.SavekidChildrenStore');

        Ext.apply(me, {
            items: [{
                xtype: 'gridpanel',
                reference: 'childrenGrid',
                store: me.store,
                border: false,
                columnLines: true,
                tbar: [{
                    text: 'Nuevo niño',
                    iconCls: 'x-fa fa-plus',
                    handler: 'onCreateChild'
                }, '->', {
                    xtype: 'textfield',
                    emptyText: 'Buscar por dispositivo',
                    reference: 'deviceFilterField',
                    width: 200
                }, {
                    text: 'Consultar',
                    iconCls: 'x-fa fa-search',
                    handler: 'onFilterByDevice'
                }],
                columns: [{
                    text: 'Nombre',
                    dataIndex: 'firstName',
                    flex: 1
                }, {
                    text: 'Apellido',
                    dataIndex: 'lastName',
                    flex: 1
                }, {
                    text: 'Fecha de nacimiento',
                    dataIndex: 'birthDate',
                    xtype: 'datecolumn',
                    format: 'Y-m-d',
                    width: 140
                }, {
                    text: 'Dispositivo asociado',
                    dataIndex: 'deviceName',
                    flex: 1
                }, {
                    text: 'Acciones',
                    xtype: 'actioncolumn',
                    width: 150,
                    items: [{
                        iconCls: 'x-fa fa-eye',
                        tooltip: 'Ver perfil',
                        handler: 'onViewProfile'
                    }, {
                        iconCls: 'x-fa fa-edit',
                        tooltip: 'Editar',
                        handler: 'onEditChild'
                    }, {
                        iconCls: 'x-fa fa-trash',
                        tooltip: 'Eliminar',
                        handler: 'onDeleteChild'
                    }]
                }],
                listeners: {
                    afterrender: function () {
                        me.store.load({
                            callback: function (records, operation, success) {
                                if (!success) {
                                    Ext.Msg.alert('Error', 'No fue posible obtener la lista de niños.');
                                }
                            }
                        });
                    }
                }
            }]
        });

        me.callParent(arguments);
    }
});
