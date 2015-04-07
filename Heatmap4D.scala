package heatmap4d

// head -200 trip_data_1_coords.csv | grep -v ',0,' | ruby -lane 'c = $_.split(",").map(&:to_f); lat=->(x){(x+74.05)*8}; lon=->(x){(x-40.7)*8}; puts [lat[c[0]],lon[c[1]],lat[c[2]],lon[c[3]]].join(",")' | grep -v ',[-1]' > normalized_taxi_200.csv

import heatmap4d.HeatmapLib._
import Heatmap4D._
import java.awt.image.BufferedImage

case class Heatmap4DBruteForce(size: Int) extends HeatmapLib(size) {
  def run(points: Seq[V4DI]) = {
    def sliceX(x: Int) = (Seq(0, x - radius).max, Seq(width, x + radius).min)
    def sliceY(y: Int) = (Seq(0, y - radius).max, Seq(height, y + radius).min)

    points.foreach { v =>
      val sA = sliceX(v.a)
      val sB = sliceY(v.b)
      val sC = sliceX(v.c)
      val sD = sliceY(v.d)
      gradientMap.zipWithIndex.slice(sA._1, sA._2).foreach { case (a, aI) =>
        a.zipWithIndex.slice(sB._1, sB._2).foreach { case (b, bI) =>
          b.zipWithIndex.slice(sC._1, sC._2).foreach { case (c, cI) =>
            Range(sD._1, sD._2).foreach { dI =>

              c(dI) += gradientValue(v, V4DI(aI, bI, cI, dI))
              if (c(dI) > maxValue) maxValue = c(dI)
            }
          }
        }
      }
    }
  }
}

case class Heatmap4DBruteImperative(size: Int) extends HeatmapLib(size) {
  import Math.{min, max}
  def run(points: Seq[V4DI]) = {

    // Loop all points
    for (p@V4DI(startX, startY, endX, endY) <- points) {

      // Loop the area in the filter
      for {
           x1 <- Range(max(0, startX-radius), min(width,  startX+radius))
           y1 <- Range(max(0, startY-radius), min(height, startY+radius))
           x2 <- Range(max(0, endX-radius),   min(width,  endX+radius))
           y2 <- Range(max(0, endY-radius),   min(height, endY+radius))
           } {

        gradientMap(x1)(y1)(x2)(y2) += gradientValue(p, V4DI(x1, y1, x2, y2))
        maxValue = max(maxValue, gradientMap(x1)(y1)(x2)(y2))
      }
    }
  }
}

case class Heatmap4DHashMap(size: Int) extends HeatmapLib(size) {
  import Math.{min, max}

  val gradientHashMap = new collection.mutable.HashMap[V4DI, Double]() { override def default(key: V4DI) = 0.0 }

  override def pointWeights = gradientHashMap.toSeq

  def run(points: Seq[V4DI]) = {

    // Loop all points
    for (p@V4DI(startX, startY, endX, endY) <- points) {

      // Loop the area in the filter
      for {
           x1 <- Range(max(0, startX-radius), min(width,  startX+radius))
           y1 <- Range(max(0, startY-radius), min(height, startY+radius))
           x2 <- Range(max(0, endX-radius),   min(width,  endX+radius))
           y2 <- Range(max(0, endY-radius),   min(height, endY+radius))
           } {

        val vec = V4DI(x1, y1, x2, y2)

        gradientHashMap(vec) += gradientValue(p, vec)
        maxValue = max(maxValue, gradientHashMap(vec))
      }
    }
  }
}

// http://web.archive.org/web/20060718054020/http://www.acm.uiuc.edu/siggraph/workshops/wjarosz_convolution_2001.pdf
case class Heatmap4DConvolution(size: Int) extends HeatmapLib(size) {

  import Math.{min, max}
  def run(points: Seq[V4DI]) = {
    // Initialize the array by laying all points down
    for (p@V4DI(startX, startY, endX, endY) <- points) {
      gradientMap(startX)(startY)(endX)(endY) += 1.0
    }

    // Iterate every point in the image, regardless of whether it has a point there
    for {startX <- Range(0, width)
         startY <- Range(0, height)
         endX <- Range(0, width)
         endY <- Range(0, height)} {

      val p = V4DI(startX, startY, endX, endY)

      // Convolve every pixel with the weighted sum of all its neighbors
      for {
           x1 <- Range(max(0, startX-radius), min(width,  startX+radius))
           y1 <- Range(max(0, startY-radius), min(height, startY+radius))
           x2 <- Range(max(0, endX-radius),   min(width,  endX+radius))
           y2 <- Range(max(0, endY-radius),   min(height, endY+radius))
           } {
        gradientMap(startX)(startY)(endX)(endY) += gradientValue(p, V4DI(x1, y1, x2, y2))
        maxValue = max(maxValue, gradientMap(startX)(startY)(endX)(endY))
      }
    }
  }
}

object Heatmap4D {
  def baselineDistance(img: BufferedImage) =
    ShowImage.diffImages(ShowImage.readImage(""), img)

  def main(args: Array[String]) {
    val s =
      if (args.isEmpty) {
        samples
      } else {
        io.Source.fromFile(args(0)).getLines().map(_.split(',').map(_.toDouble)).toSeq
          .map{case Array(a,b,c,d) => V4DD(a,b,c,d)}.toSeq
      }

    val size = 100
    println(s"image size: " + size + "^4")
    println(s"point pairs:" + s.size)
    
    // p -> number of points
    // r -> radius of filter
    // n -> size of one dimension of image

    //val hm = Heatmap4DBruteForce(size, size)       // O(p*r^4)
    //val hm = Heatmap4DBruteImperative(size, size)  // O(p*r^4)
    //val hm = Heatmap4DConvolution(size)          // O(n^4*r^4)
    val hm = Heatmap4DHashMap(size)

    println("kernel radius: " + hm.radius)

    time("total") {
      ShowImage.showImage(pointsToImg(size*10, s))

      time("adding") {
        //s.foreach(v => hm.add(v.toI(size)))
        hm.run(s.map(_.toI(size)))
      }

      time("rendering") {

        val filename = s"heatmap4d_${size}_${s.size}_${hm.radius}_${System.currentTimeMillis / 1000}"
        reflect.io.File(filename+".json").writeAll(arrayToJson(hm.gradientMap))
        ShowImage.saveImage(hm.toImage, filename+".png")
        ShowImage.showImage(hm.toImage)
      }
    }
  }
}

