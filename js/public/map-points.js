
function setExportMapHandler() {
  var exportPNGElement = document.getElementById('export-png');
  exportPNGElement.addEventListener('click', function(e) {
    map.once('postcompose', function(event) {
      var canvas = event.context.canvas;
      exportPNGElement.href = canvas.toDataURL('image/png');
    });
    map.renderSync();
  }, false);
}

function drawPoints(arr) {
  var vector = new ol.Layer.Vector('Point Layer');
  arr.forEach(function(lat, lng) {
    vector.addFeatures([ol.Geometry.Point(lat,lng)]))]);
  });
  map.addLayers([vector]);
}




function initMap() {
  map = new ol.Map({
    layers: [
      new ol.layer.Tile({
        source: new ol.source.OSM()
      })
    ],
    target: 'map',
    controls:
      ol.control.defaults({}),
    view: new ol.View({
      center: [0, 0],
      zoom: 2
    })
  });
}

function loadData() {
  return JSON.parse(points);
}

var map;

function main() {
  initMap();
  setExportMapHandler();
}

main();
