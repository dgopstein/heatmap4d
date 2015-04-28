content = 'start';

// http://stackoverflow.com/questions/9765224/draw-line-between-two-points-using-openlayers

//function loadData() {
//  //return JSON.parse(points);
//  return points.slice(0,10000);
//}

function drawSegments(arr) {
  var vectorSource = sourceFromSegments(arr);

  var vectorLayer = new ol.layer.Vector({
    source: vectorSource
  });
  
  map.addLayer(vectorLayer);
}  

function main() {
  initMap();
  setExportMapHandler();
  var pts = loadData();
  var segments = pts.map(function(arr) {return [arr[1],arr[0], arr[3],arr[2]]});

  drawSegments(segments);
}

main();
