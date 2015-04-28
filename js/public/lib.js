
var map;
function initMap() {
  var pencilLayer = 
      new ol.layer.Tile({
        //source: new ol.source.OSM(),
        source: new ol.source.XYZ({
            url: 'http://api.tiles.mapbox.com/v4/dgopstein.18df0fc9/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiZGdvcHN0ZWluIiwiYSI6IkNNaFFYODAifQ.RO5unyKLMcbB-BPHdxer_w',
          }),
      });


  map = new ol.Map({
    layers: [ pencilLayer ],
    target: 'map',
    controls:
      ol.control.defaults({}),
    view: new ol.View({
      center: transform([-73.95, 40.75]),
      zoom: 13
    })
  });
  
  //map = new OpenLayers.Map('map');
  //map.addLayer(new OpenLayers.Layer.OSM());
}

function loadData() {
  //return JSON.parse(points);
  var pts =  points.slice(0,10000);
  var segments = pts.map(function(arr) {return [[arr[0],arr[1]], [arr[2],arr[3]]]});
  return segments;
}

function transform(pt) {
  return ol.proj.transform(pt, 'EPSG:4326', 'EPSG:3857');
}

function sourceFromSegments(arr) {
  var ptFeatures = arr.map(function(pair) {
    var pt1 = transform(pair[0]);
    var pt2 = transform(pair[1]);
    var segment = new ol.geom.LineString([pt1, pt2])
    
    return new ol.Feature({
      geometry: segment
    });
  });
  console.log(ptFeatures);
  var vectorSource = new ol.source.Vector({
    features: ptFeatures, //add an array of features
  });

  return vectorSource;
}

function sourceFromPoints(arr) {
  var ptFeatures = arr.map(function(pt) {
    return new ol.Feature({
      geometry: new ol.geom.Point(transform(pt))
    });
  });
  console.log(ptFeatures);
  var vectorSource = new ol.source.Vector({
    features: ptFeatures, //add an array of features
  });

  return vectorSource;
}

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
