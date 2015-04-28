
var map;
function initMap() {
  var pencilLayer = 
      new ol.layer.Tile({
        //source: new ol.source.OSM(),
        source: new ol.source.XYZ({
            url: 'http://api.tiles.mapbox.com/v4/dgopstein.18df0fc9/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiZGdvcHN0ZWluIiwiYSI6IkNNaFFYODAifQ.RO5unyKLMcbB-BPHdxer_w',

            //crossOriginKeyword: 'anonymous',
          }),
        //crossOriginKeyword: 'anonymous',
        //tileOptions: {crossOriginKeyword: 'anonymous'}
      }, { tileOptions: {crossOriginKeyword: 'anonymous'} });


  map = new ol.Map({
    layers: [ pencilLayer ],
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
  return points.slice(0,10000);
}

function sourceFromSegments(arr) {
  var ptFeatures = arr.map(function(pair) {
    var lat1 = pair[0];
    var lng1 = pair[1];
    var lat2 = pair[2];
    var lng2 = pair[3];
    var pt1 = ol.proj.transform([lng1, lat1], 'EPSG:4326', 'EPSG:3857');
    var pt2 = ol.proj.transform([lng2, lat2], 'EPSG:4326', 'EPSG:3857');
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
