var map = new ol.Map({
    target: 'map',
    layers: [
        new ol.layer.Tile({
            source: new ol.source.OSM()
        })
    ],
    view: new ol.View({
        center: ol.proj.transform([-0.92, 52.96], 'EPSG:4326', 'EPSG:3857'),
        zoom: 6
    })
});

var popup = new ol.Overlay.Popup();
map.addOverlay(popup);

map.on('singleclick', function(evt) {
    var content = '<h2>Close the popup?</h2>';
    content += '<p><a href="#" data-action="yes">Yes</a>, <a href="#" data-action="no">No</a></p>';
    popup.show(evt.coordinate, content);
});

// Add a click event handler to the popup DOM element which we will use to
// capture the user clicking on a link within the popup with an "action" data
// attribute
popup.getElement().addEventListener('click', function(e) {
    var action = e.target.getAttribute('data-action');
    if (action) {
        alert('You choose: ' + action);
        if (action === 'yes') {
            popup.hide();
        }
        e.preventDefault();
    }
}, false);

