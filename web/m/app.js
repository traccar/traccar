var mapPanel, toolbar, map;

Ext.application({
    name: "Traccar",

    launch: function() {

        toolbar = Ext.create('Ext.Toolbar', {
            docked: 'top',
            ui: 'light',
            title: 'Traccar'
        });

        mapPanel = Ext.create('Ext.Panel', {
            listeners: {
                painted: function() {

                    var layer = new ol.layer.Tile({ source: new ol.source.OSM({
                    })});

                    var view = new ol.View({
                        center: ol.proj.fromLonLat([ -0.1275, 51.507222 ]),
                        zoom: 6,
                        maxZoom: 16
                    });

                    map = new ol.Map({
                        target: this.bodyElement.dom.id,
                        layers: [ layer ],
                        view: view
                    });
                },

                resize: function() {
                    map.updateSize();
                }
            }
        });

        Ext.create('Ext.Panel', {
            fullscreen: true,
            layout: 'fit',
            items: [toolbar, mapPanel]
        });

    }
});
