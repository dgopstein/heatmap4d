content = 'start';

function loadData() {
  //return JSON.parse(points);
  return points.slice(0,10000);
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


function drawPoints(arr) {
  var vectorSource = sourceFromPoints(arr);

  var vectorLayer = new ol.layer.Heatmap({
    source: vectorSource,
    //source: new ol.source.KML({
    //  extractStyles: false,
    //  projection: 'EPSG:3857',
    //  url: 'http://openlayers.org/en/v3.1.0/examples/data/kml/2012_Earthquakes_Mag5.kml'
    //}),
    radius: 2
  });
  
  
  vectorLayer.getSource().on('addfeature', function(event) {
    // 2012_Earthquakes_Mag5.kml stores the magnitude of each earthquake in a
    // standards-violating <magnitude> tag in each Placemark.  We extract it from
    // the Placemark's name instead.
    var name = event.feature.get('name');
    var magnitude = parseFloat(name.substr(2));
    event.feature.set('weight', magnitude - 5);
  });
  
  map.addLayer(vectorLayer);
}  
//  var map = new ol.Map({
//    layers: [raster, /*heat,*/ vector],
//    target: 'map',
//    view: new ol.View({
//      center: [0, 0],
//      zoom: 2
//    })
//  });
//}



function main() {
  initMap();
  setExportMapHandler();
  var pts = loadData();
  var startPoints = pts.map(function(arr) {return [arr[1],arr[0]]});
  var endPoints = pts.map(function(arr) {return [arr[3],arr[2]]});

  drawPoints(content !== 'end' ? startPoints : endPoints);
}

main();
