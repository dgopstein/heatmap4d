content = 'start';

// http://stackoverflow.com/questions/9765224/draw-line-between-two-points-using-openlayers
// http://stackoverflow.com/questions/26977151/styling-multilinestring-with-different-color

function loadData() {
  return points.slice(0,1000);
}

function drawSegments(arr) {
  var vectorSource = sourceFromSegments(arr);

  var vectorLayer = new ol.layer.Vector({
    source: vectorSource,
    style: new ol.style.Style({
        stroke: new ol.style.Stroke({
            color: rgbaPurple,
            width: 1
        }),
    })
  });
  
  map.addLayer(vectorLayer);
}  

function main() {
  initMap();
  setExportMapHandler();
  var pts = loadData();
  var segments = pts.map(function(arr) {return [[arr[0],arr[1]], [arr[2],arr[3]]]});

  drawSegments(segments);
}

main();
