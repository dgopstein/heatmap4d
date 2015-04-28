content = 'start';

function drawPoints(arr) {
  var vectorSource = sourceFromPoints(arr);

  var vectorLayer = new ol.layer.Heatmap({
    source: vectorSource,
    radius: 2
  });// , { tileOptions: {crossOriginKeyword: 'anonymous'} });
  
  map.addLayer(vectorLayer);
}  

function main() {
  initMap();
  setExportMapHandler();
  var pts = loadData();
  var startPoints = pts.map(function(arr) {return [arr[1],arr[0]]});
  var endPoints = pts.map(function(arr) {return [arr[3],arr[2]]});

  drawPoints(content !== 'end' ? startPoints : endPoints);
}

main();
