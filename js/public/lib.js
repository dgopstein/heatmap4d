//var Color = require("color");
//var Color = net.brehaut.Color;

var rgbaBlue = 'rgba(51, 153, 204, 1)'
var rgbaRed = 'rgba(204, 81, 81, 1)'
var rgbaPurple = 'rgba(116, 79, 196, 1)'
var rgbaPurple2 = 'rgba(88, 61, 184, 1)'

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

function intensityColor(weight) {
  var intensity = weight

// My own
  //var h = (1.0-intensity) % 1.0;
  //var s = 0.75 - 0.5*intensity;
  //var l = intensity;
  //var a = 1.0 - 0.7*intensity;

// SO's
  var h = intensity * 240
  var a = 1//(.3 * Math.log(5 - intensity))

  var c = tinycolor({h: h, s: 1, l: .5, a: a});
  return c;
}

function logB(b, val) {
  return Math.log(val) / Math.log(b);
}

function norm(weight) {
  return 1 - logB(3.5*maxValue, weight)
}

function sourceFromSegments(arr) {
  var ptFeatures = arr.map(function(pair) {
    var pt1 = transform(pair[0]);
    var pt2 = transform(pair[1]);
    var segment = new ol.geom.LineString([pt1, pt2])


    var ftObj = { geometry: segment }

    var feature = new ol.Feature(ftObj);
    if (typeof(pair[2]) !=='undefined') {
      var intensity = norm(pair[2]);
      var color = intensityColor(intensity);
      //console.log("color: ", color);
      var colorStr = color.toRgbString();
      //console.log("color: ", colorStr);
      var style = new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: colorStr,
                width: 1
            }),
        })
      feature.setStyle(style)
    }
    
    return feature
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
  //console.log(ptFeatures);
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
