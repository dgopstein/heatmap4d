content = 'end';

pointStyles = {
  none: undefined,
  original:
    new ol.style.Style({
      image: new ol.style.Circle({
        radius: 5,
        fill: new ol.style.Fill({
          color: 'rgba(255, 255, 255, .4)'
        }),
        stroke: new ol.style.Stroke({
          color: 'rgba(51, 153, 204, 1)'
          ,width: 1.2
        })
    })}),
  start:
    new ol.style.Style({
      image: new ol.style.Circle({
        radius: 5,
        fill: new ol.style.Fill({
          color: 'rgba(255, 255, 255, .4)'
        }),
        stroke: new ol.style.Stroke({
          color: 'rgba(51, 153, 204, 1)'
          ,width: 1.2
        })
      })
    }),
  end:
    new ol.style.Style({
      image: new ol.style.Circle({
        radius: 5,
        fill: new ol.style.Fill({
          color: 'rgba(255, 255, 255, .4)'
        }),
        stroke: new ol.style.Stroke({
          color: 'rgba(204, 81, 81, 1)'
          ,width: 1.2
        })
      })
    })
}

function drawPoints(arr) {
   var vectorSource = sourceFromPoints(arr);
  /*var*/ vectorLayer = new ol.layer.Vector({
    title: 'Point Layer',
    source: vectorSource,
    style: pointStyles[content]
  });
  console.log('vectorLayer', vectorLayer.style);
  map.addLayer(vectorLayer);
  //map.addLayers([vector]);
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
