Ext.define('Traccar.view.savekid.SavekidChildProfilePanel', {
    extend: 'Ext.window.Window',
    xtype: 'savekid-child-profile',

    requires: [
        'Ext.chart.CartesianChart',
        'Ext.chart.series.Line',
        'Ext.chart.axis.Time',
        'Ext.chart.axis.Numeric'
    ],

    width: 1000,
    height: 700,
    modal: true,
    layout: 'fit',
    closeAction: 'destroy',

    config: {
        childRecord: null,
        autoLoadLastChild: false,
        showHistoryOnly: false,
        showReportsOnly: false
    },

    initComponent: function () {
        var me = this;

        Ext.apply(me, {
            items: [{
                xtype: 'panel',
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                scrollable: true,
                bodyPadding: 10,
                items: [me.createPersonalData(), me.createPhysiology(), me.createEvents(), me.createLocation(), me.createChartsSection()]
            }]
        });

        me.on('afterrender', function () {
            me.loadChildProfile();
        });

        me.callParent(arguments);
    },

    createPersonalData: function () {
        return {
            xtype: 'fieldset',
            title: 'Datos personales',
            itemId: 'personalData',
            defaults: { xtype: 'displayfield', anchor: '100%' },
            items: [{
                fieldLabel: 'Nombre',
                name: 'firstName'
            }, {
                fieldLabel: 'Apellido',
                name: 'lastName'
            }, {
                fieldLabel: 'Fecha de nacimiento',
                name: 'birthDate'
            }, {
                fieldLabel: 'Condiciones médicas',
                name: 'medicalConditions'
            }, {
                fieldLabel: 'Dispositivo',
                name: 'deviceName'
            }]
        };
    },

    createPhysiology: function () {
        return {
            xtype: 'fieldset',
            title: 'Datos fisiológicos recientes',
            itemId: 'physiologyData',
            defaults: { xtype: 'displayfield', anchor: '100%' },
            items: [{
                fieldLabel: 'Peso (kg)',
                name: 'weight'
            }, {
                fieldLabel: 'Altura (cm)',
                name: 'height'
            }, {
                fieldLabel: 'Frecuencia cardíaca',
                name: 'heartRate'
            }, {
                fieldLabel: 'Temperatura (°C)',
                name: 'temperature'
            }]
        };
    },

    createEvents: function () {
        return {
            xtype: 'fieldset',
            title: 'Últimos eventos',
            itemId: 'eventsData',
            html: '<div class="savekid-events">Sin eventos recientes</div>'
        };
    },

    createLocation: function () {
        return {
            xtype: 'fieldset',
            title: 'Última ubicación',
            itemId: 'locationData',
            html: '<div class="savekid-location">Sin ubicación disponible</div>'
        };
    },

    createChartsSection: function () {
        return {
            xtype: 'container',
            layout: 'column',
            defaults: {
                columnWidth: 0.33,
                height: 250,
                padding: '0 10 10 0'
            },
            items: [this.createHeartChart(), this.createTemperatureChart(), this.createStepsChart()]
        };
    },

    createHeartChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'heartChart',
            insetPadding: 10,
            axes: [{ type: 'time', position: 'bottom', title: 'Hora' },
                { type: 'numeric', position: 'left', title: 'BPM' }],
            series: [{ type: 'line', xField: 'timestamp', yField: 'value', style: { stroke: '#e74c3c', lineWidth: 2 } }],
            store: { fields: ['timestamp', 'value'], data: [] },
            title: 'Últimas 24h - Frecuencia cardíaca'
        };
    },

    createTemperatureChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'temperatureChart',
            insetPadding: 10,
            axes: [{ type: 'time', position: 'bottom', title: 'Hora' },
                { type: 'numeric', position: 'left', title: '°C' }],
            series: [{ type: 'line', xField: 'timestamp', yField: 'value', style: { stroke: '#f39c12', lineWidth: 2 } }],
            store: { fields: ['timestamp', 'value'], data: [] },
            title: 'Últimas 24h - Temperatura'
        };
    },

    createStepsChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'stepsChart',
            insetPadding: 10,
            axes: [{ type: 'time', position: 'bottom', title: 'Fecha' },
                { type: 'numeric', position: 'left', title: 'Pasos' }],
            series: [{ type: 'line', xField: 'timestamp', yField: 'value', style: { stroke: '#27ae60', lineWidth: 2 } }],
            store: { fields: ['timestamp', 'value'], data: [] },
            title: 'Pasos diarios'
        };
    },

    loadChildProfile: function () {
        var me = this;
        var record = me.getChildRecord();

        if (!record && me.getAutoLoadLastChild()) {
            Ext.Ajax.request({
                url: '/api/savekid/children',
                method: 'GET',
                success: function (response) {
                    var data = Ext.decode(response.responseText || '[]');
                    if (data && data.length > 0) {
                        me.setChildRecord(Ext.create('Ext.data.Model', data[0]));
                        me.populateData();
                    }
                },
                failure: function () {
                    Ext.Msg.alert('Error', 'No fue posible cargar el perfil.');
                }
            });
            return;
        }

        if (record) {
            me.populateData();
        }
    },

    populateData: function () {
        var me = this;
        var record = me.getChildRecord();
        if (!record) {
            return;
        }

        var data = record.getData();
        var personalFieldset = me.down('#personalData');
        Ext.Array.forEach(personalFieldset.items.items, function (field) {
            var name = field.name;
            if (data[name] !== undefined) {
                field.setValue(data[name]);
            }
        });

        var physiologyFieldset = me.down('#physiologyData');
        Ext.Array.forEach(physiologyFieldset.items.items, function (field) {
            var name = field.name;
            if (data[name] !== undefined) {
                field.setValue(data[name]);
            }
        });

        this.loadCharts(record.get('id'));
        this.loadEvents(record.get('id'));
        this.loadLocation(record.get('id'));
    },

    loadCharts: function (childId) {
        var me = this;
        Ext.Ajax.request({
            url: '/api/savekid/children/' + childId + '/metrics',
            method: 'GET',
            success: function (response) {
                var payload = Ext.decode(response.responseText || '{}');
                me.updateChartStore('heartChart', payload.heartRate || []);
                me.updateChartStore('temperatureChart', payload.temperature || []);
                me.updateChartStore('stepsChart', payload.steps || []);
            },
            failure: function () {
                Ext.Msg.alert('Error', 'No fue posible cargar los gráficos de salud.');
            }
        });
    },

    updateChartStore: function (chartRef, data) {
        var chart = this.down('[reference=' + chartRef + ']');
        if (chart) {
            chart.getStore().setData(Ext.Array.map(data, function (item) {
                return {
                    timestamp: new Date(item.timestamp || item.date || new Date()),
                    value: item.value || 0
                };
            }));
        }
    },

    loadEvents: function (childId) {
        var me = this;
        Ext.Ajax.request({
            url: '/api/savekid/children/' + childId + '/events',
            method: 'GET',
            success: function (response) {
                var events = Ext.decode(response.responseText || '[]');
                var html = '<ul>' + Ext.Array.map(events, function (evt) {
                    return '<li>' + (evt.description || evt.type || 'Evento') + '</li>';
                }).join('') + '</ul>';
                me.down('#eventsData').update(html);
            },
            failure: function () {
                me.down('#eventsData').update('<div class="savekid-events">No fue posible cargar los eventos.</div>');
            }
        });
    },

    loadLocation: function (childId) {
        var me = this;
        Ext.Ajax.request({
            url: '/api/savekid/children/' + childId + '/location',
            method: 'GET',
            success: function (response) {
                var loc = Ext.decode(response.responseText || '{}');
                var html = 'Lat: ' + (loc.latitude || '-') + ', Lng: ' + (loc.longitude || '-') + ' a las ' + (loc.time || '-');
                me.down('#locationData').update('<div class="savekid-location">' + html + '</div>');
            },
            failure: function () {
                me.down('#locationData').update('<div class="savekid-location">No fue posible cargar la ubicación.</div>');
            }
        });
    }
});
