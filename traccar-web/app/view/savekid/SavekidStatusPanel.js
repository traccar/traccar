Ext.define('Traccar.view.savekid.SavekidStatusPanel', {
    extend: 'Ext.panel.Panel',
    xtype: 'savekid-status-panel',

    requires: [
        'Ext.util.TaskRunner',
        'Traccar.store.SavekidPositionsStore'
    ],

    config: {
        childId: null,
        deviceId: null,
        autoLoad: true
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    bodyPadding: 10,
    scrollable: true,

    initComponent: function () {
        var me = this;
        me.ensureStyles();

        me.positionsStore = Ext.create('Traccar.store.SavekidPositionsStore');
        me.runner = new Ext.util.TaskRunner();

        Ext.apply(me, {
            items: [me.createHeaderRow(), me.createMapBlock()]
        });

        me.on('afterrender', function () {
            if (me.getAutoLoad()) {
                me.loadStatusData();
                me.loadPositions();
            }
            me.startAutoRefresh();
        });

        me.on('destroy', function () {
            me.stopAutoRefresh();
        });

        me.callParent(arguments);
    },

    ensureStyles: function () {
        if (Traccar.view.savekid.SavekidStatusPanel.stylesInjected) {
            return;
        }
        var css = [
            '.savekid-status-card { border-radius: 8px; padding: 16px; color: #fff; }',
            '.savekid-status-ok { background: linear-gradient(135deg, #27ae60, #2ecc71); }',
            '.savekid-status-alerta { background: linear-gradient(135deg, #f1c40f, #f39c12); color: #1b1b1b; }',
            '.savekid-status-riesgo, .savekid-status-sos { background: linear-gradient(135deg, #c0392b, #e74c3c); }',
            '.savekid-status-title { font-size: 18px; opacity: 0.85; }',
            '.savekid-status-value { font-size: 42px; font-weight: 800; letter-spacing: 1px; }',
            '.savekid-hr-value { font-size: 48px; font-weight: 900; animation: savekidPulse 1.6s ease-in-out infinite; }',
            '.savekid-hr-label { color: #6c7a89; }',
            '.savekid-badge { display: inline-block; padding: 6px 10px; border-radius: 12px; font-weight: 700; }',
            '.savekid-badge-warn { background: #e74c3c; color: #fff; }',
            '.savekid-badge-ok { background: #2ecc71; color: #fff; }',
            '.savekid-steps { font-size: 24px; font-weight: 800; }',
            '.savekid-events-list { list-style: none; padding: 0; margin: 0; }',
            '.savekid-events-list li { padding: 6px 0; border-bottom: 1px solid #ececec; }',
            '.savekid-map-placeholder { background: #f8f8f8; border: 1px dashed #dcdcdc; height: 100%; display: flex; align-items: center; justify-content: center; color: #9b9b9b; }',
            '@keyframes savekidPulse { 0% { transform: scale(1); } 50% { transform: scale(1.05); } 100% { transform: scale(1); } }'
        ].join(' ');
        Ext.util.CSS.createStyleSheet(css, 'savekid-status-styles');
        Traccar.view.savekid.SavekidStatusPanel.stylesInjected = true;
    },

    createHeaderRow: function () {
        return {
            xtype: 'container',
            layout: 'column',
            defaults: {
                columnWidth: 0.33,
                padding: '0 10 10 0'
            },
            items: [this.createStatusCard(), this.createMetricsCard(), this.createEventsCard()]
        };
    },

    createStatusCard: function () {
        return {
            xtype: 'container',
            itemId: 'statusCard',
            cls: 'savekid-status-card savekid-status-ok',
            height: 180,
            layout: 'vbox',
            defaults: {
                xtype: 'component'
            },
            items: [{
                html: '<div class="savekid-status-title">Estado actual</div>'
            }, {
                itemId: 'statusValue',
                html: '<div class="savekid-status-value">OK</div>'
            }, {
                itemId: 'statusMeta',
                html: '<div>Actualizado: --</div>'
            }, {
                itemId: 'statusMessage',
                html: '<div>Sin alertas</div>'
            }]
        };
    },

    createMetricsCard: function () {
        return {
            xtype: 'panel',
            title: 'Métricas fisiológicas',
            itemId: 'metricsCard',
            height: 180,
            bodyPadding: 12,
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            defaults: {
                xtype: 'container',
                flex: 1,
                layout: 'vbox',
                align: 'center'
            },
            items: [{
                items: [{
                    xtype: 'component',
                    cls: 'savekid-hr-value',
                    itemId: 'heartRateValue',
                    html: '--'
                }, {
                    xtype: 'component',
                    cls: 'savekid-hr-label',
                    html: 'HR'
                }]
            }, {
                items: [{
                    xtype: 'component',
                    itemId: 'temperatureBadge',
                    cls: 'savekid-badge savekid-badge-ok',
                    html: 'Temp: -- °C'
                }, {
                    xtype: 'component',
                    cls: 'savekid-hr-label',
                    html: 'Temperatura'
                }]
            }, {
                items: [{
                    xtype: 'component',
                    itemId: 'stepsCounter',
                    cls: 'savekid-steps',
                    html: '--'
                }, {
                    xtype: 'component',
                    cls: 'savekid-hr-label',
                    html: 'Pasos acumulados'
                }]
            }]
        };
    },

    createEventsCard: function () {
        return {
            xtype: 'panel',
            title: 'Eventos recientes',
            itemId: 'eventsCard',
            height: 180,
            bodyPadding: 12,
            scrollable: true,
            html: '<ul class="savekid-events-list"><li>Sin eventos</li></ul>'
        };
    },

    createMapBlock: function () {
        var me = this;
        return {
            xtype: 'panel',
            title: 'Ubicación en mapa',
            flex: 1,
            layout: 'vbox',
            items: [{
                xtype: 'component',
                itemId: 'mapComponent',
                height: 350,
                html: '<div class="savekid-map-placeholder">Cargando mapa...</div>',
                listeners: {
                    afterrender: function (cmp) {
                        me.initMap(cmp.getEl());
                    }
                }
            }, {
                xtype: 'component',
                itemId: 'locationInfo',
                padding: 10,
                html: 'Última ubicación: --'
            }]
        };
    },

    initMap: function (el) {
        var me = this;
        if (window.L && el) {
            me.map = L.map(el.dom.id || Ext.id(el.dom)).setView([0, 0], 2);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(me.map);
        } else if (el) {
            el.update('<div class="savekid-map-placeholder">Componente de mapa no disponible</div>');
        }
    },

    startAutoRefresh: function () {
        var me = this;
        me.statusTask = me.runner.start({
            run: function () {
                me.loadStatusData();
            },
            interval: 10000
        });
        me.positionTask = me.runner.start({
            run: function () {
                me.loadPositions();
            },
            interval: 10000
        });
    },

    stopAutoRefresh: function () {
        var me = this;
        if (me.runner) {
            me.runner.stopAll();
        }
    },

    loadStatusData: function () {
        var me = this;
        if (!me.getChildId()) {
            return;
        }
        Ext.Ajax.request({
            url: Ext.String.format('/api/savekid/children/{0}/status', me.getChildId()),
            method: 'GET',
            success: function (response) {
                var data = Ext.decode(response.responseText || '{}');
                me.applyStatus(data);
                if (!me.getDeviceId() && data.deviceId) {
                    me.setDeviceId(data.deviceId);
                }
            },
            failure: function () {
                Ext.Msg.alert('Error', 'No fue posible cargar el estado actual.');
            }
        });
    },

    applyStatus: function (data) {
        var me = this;
        var status = (data.status || 'OK').toUpperCase();
        var statusCard = me.down('#statusCard');
        var statusValue = me.down('#statusValue');
        var statusMeta = me.down('#statusMeta');
        var statusMessage = me.down('#statusMessage');
        var heartRateValue = me.down('#heartRateValue');
        var temperatureBadge = me.down('#temperatureBadge');
        var stepsCounter = me.down('#stepsCounter');

        var cls = 'savekid-status-card ';
        if (status === 'OK') {
            cls += 'savekid-status-ok';
        } else if (status === 'ALERTA') {
            cls += 'savekid-status-alerta';
        } else {
            cls += 'savekid-status-riesgo';
        }

        statusCard.setCls(cls);
        statusValue.update(Ext.String.format('<div class="savekid-status-value">{0}</div>', status));
        statusMeta.update('Actualizado: ' + (data.timestamp || '--'));
        statusMessage.update(data.message ? Ext.String.htmlEncode(data.message) : 'Monitoreo activo');

        if (heartRateValue) {
            heartRateValue.update(data.heartRate ? Ext.String.format('{0} bpm', data.heartRate) : '--');
        }

        if (temperatureBadge) {
            var temp = data.temperature;
            var warn = temp && temp > (data.temperatureThreshold || 37.5);
            temperatureBadge.setCls('savekid-badge ' + (warn ? 'savekid-badge-warn' : 'savekid-badge-ok'));
            temperatureBadge.update(temp ? Ext.String.format('Temp: {0} °C', temp) : 'Temp: --');
        }

        if (stepsCounter) {
            stepsCounter.update(data.steps ? Ext.util.Format.number(data.steps, '0,000') : '--');
        }

        this.updateEvents(data.events || []);
    },

    updateEvents: function (events) {
        var list = '<ul class="savekid-events-list">';
        if (!events.length) {
            list += '<li>Sin eventos</li>';
        } else {
            Ext.Array.each(events, function (evt) {
                list += '<li><strong>' + Ext.String.htmlEncode(evt.type || 'Evento') + '</strong> - ' +
                    Ext.String.htmlEncode(evt.description || '') + ' <em>(' + (evt.time || '--') + ')</em></li>';
            });
        }
        list += '</ul>';
        this.down('#eventsCard').update(list);
    },

    loadPositions: function () {
        var me = this;
        if (!me.getDeviceId()) {
            return;
        }
        me.positionsStore.getProxy().setExtraParams({ deviceId: me.getDeviceId(), page: 1, limit: 1 });
        me.positionsStore.load({
            callback: function (records) {
                if (records && records.length) {
                    me.applyPosition(records[0].getData());
                }
            }
        });
    },

    applyPosition: function (position) {
        var me = this;
        var locationInfo = me.down('#locationInfo');
        if (locationInfo) {
            var text = Ext.String.format('Última ubicación: {0}, {1} a las {2}',
                position.latitude || '--', position.longitude || '--', position.fixTime || '--');
            if (position.address) {
                text += ' - ' + Ext.String.htmlEncode(position.address);
            }
            locationInfo.update(text);
        }

        if (me.map && position.latitude && position.longitude && window.L) {
            if (!me.marker) {
                me.marker = L.marker([position.latitude, position.longitude]).addTo(me.map);
            } else {
                me.marker.setLatLng([position.latitude, position.longitude]);
            }
            me.map.setView([position.latitude, position.longitude], position.zoom || 15);
        }
    }
});
