var map;

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
  var ptFeatures = arr.map(function(pair) {
    var lat = pair[0];
    var lng = pair[1];
    //return new ol.Feature({geometry: new ol.geom.Point(lat,lng)});
    
    return new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.transform([lng, lat], 'EPSG:4326', 'EPSG:3857'))
    });
  });
  console.log(ptFeatures);
  var vectorSource = new ol.source.Vector({
    features: ptFeatures //add an array of features
  });
  var vectorLayer = new ol.layer.Vector({title: 'Point Layer', source: vectorSource});
  map.addLayer(vectorLayer);
  //map.addLayers([vector]);
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
      center: ol.proj.transform([-73.95, 40.75], 'EPSG:4326', 'EPSG:3857'),
      zoom: 13
    })
  });
  
  //map = new OpenLayers.Map('map');
  //map.addLayer(new OpenLayers.Layer.OSM());
}

function loadData() {
  //return JSON.parse(points);
  return points;
}


function main() {
  initMap();
  setExportMapHandler();

  var pts = loadData();
  var startPoints = pts.map(function(arr) {return [arr[1],arr[0]]});
  var endPoints = pts.map(function(arr) {return [arr[3],arr[2]]});

  drawPoints(startPoints);

  console.log('finished: ', pts.length);
}

main();
