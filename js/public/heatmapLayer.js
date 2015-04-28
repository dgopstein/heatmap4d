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
  var sgmts = loadData();
  var startPoints = sgmts.map(function(arr) {return arr[0]});
  var endPoints = sgmts.map(function(arr) {return arr[1]});

  drawPoints(content !== 'end' ? startPoints : endPoints);
}

main();
