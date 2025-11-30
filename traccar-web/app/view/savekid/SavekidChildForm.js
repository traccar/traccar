Ext.define('Traccar.view.savekid.SavekidChildForm', {
    extend: 'Ext.window.Window',
    xtype: 'savekid-child-form',

    requires: [
        'Traccar.store.Devices'
    ],

    width: 500,
    modal: true,
    layout: 'fit',
    closeAction: 'destroy',

    config: {
        childRecord: null
    },

    initComponent: function () {
        var me = this;
        var deviceStore = Ext.create('Traccar.store.Devices');

        Ext.apply(me, {
            items: [{
                xtype: 'form',
                bodyPadding: 10,
                defaults: {
                    anchor: '100%',
                    allowBlank: false,
                    msgTarget: 'side'
                },
                items: [{
                    xtype: 'textfield',
                    name: 'firstName',
                    fieldLabel: 'Nombre'
                }, {
                    xtype: 'textfield',
                    name: 'lastName',
                    fieldLabel: 'Apellido'
                }, {
                    xtype: 'datefield',
                    name: 'birthDate',
                    fieldLabel: 'Fecha de nacimiento',
                    format: 'Y-m-d'
                }, {
                    xtype: 'numberfield',
                    name: 'weight',
                    fieldLabel: 'Peso (kg)',
                    minValue: 0,
                    allowDecimals: true
                }, {
                    xtype: 'numberfield',
                    name: 'height',
                    fieldLabel: 'Altura (cm)',
                    minValue: 0,
                    allowDecimals: true
                }, {
                    xtype: 'textarea',
                    name: 'medicalConditions',
                    fieldLabel: 'Condiciones médicas',
                    allowBlank: true
                }, {
                    xtype: 'combobox',
                    fieldLabel: 'Dispositivo',
                    name: 'deviceId',
                    store: deviceStore,
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'remote',
                    forceSelection: true,
                    editable: false,
                    emptyText: 'Seleccione un dispositivo'
                }],
                buttons: [{
                    text: 'Guardar',
                    formBind: true,
                    handler: function () {
                        var form = this.up('form');
                        if (form.isValid()) {
                            me.saveChild(form.getValues());
                        }
                    }
                }, {
                    text: 'Cancelar',
                    handler: function () {
                        me.close();
                    }
                }]
            }]
        });

        me.on('afterrender', function () {
            if (me.getChildRecord()) {
                me.down('form').getForm().setValues(me.getChildRecord().getData());
            }
        });

        me.callParent(arguments);
    },

    saveChild: function (values) {
        var me = this;
        var record = me.getChildRecord();
        if (record) {
            values.id = record.get('id');
        }

        Ext.Ajax.request({
            url: '/api/savekid/children',
            method: 'POST',
            jsonData: values,
            success: function () {
                Ext.toast('Perfil guardado correctamente');
                me.fireEvent('saved');
                me.close();
            },
            failure: function (response) {
                var message = 'No fue posible guardar el perfil';
                if (response && response.responseText) {
                    message += ': ' + response.responseText;
                }
                Ext.Msg.alert('Error', message);
            }
        });
    }
});
