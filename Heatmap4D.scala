import HeatmapLib._
import Heatmap4D._

case class Heatmap4DBruteForce(width: Int, height: Int) extends HeatmapLib(width, height) {
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

case class Heatmap4DBruteImperative(width: Int, height: Int) extends HeatmapLib(width, height) {
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

// http://web.archive.org/web/20060718054020/http://www.acm.uiuc.edu/siggraph/workshops/wjarosz_convolution_2001.pdf
case class Heatmap4DBoxFilter(width: Int, height: Int) extends HeatmapLib(width, height) {
  import Math.{min, max}
  def run(points: Seq[V4DI]) = {
    val boxFilter = Seq.fill(radius)(1.0)

    // Initialize the array by laying all points down
    for (p@V4DI(startX, startY, endX, endY) <- points) {
      gradientMap(startX)(startY)(endX)(endY) += 1.0
    }

    def average(seq: Seq[Double]) = seq.sum / seq.length

    def convolve(x: Int, dim: (Int) => Double) = {
      val region = Range(x - radius, x + radius).zip(boxFilter)
                     .filter { case (i, weight) => i > 0 && i < width}
      val filtered = region.map{ case (i, weight) => weight * dim(i) }
      average(filtered)
    }

    // Run a box filter in each of the basic directions
    // TODO this misses the radius/2 pixels on each edge

    val dims =
      Seq(i => gradientMap(i)(startY)(endX)(endY),
          i => gradientMap(startX)(i)(endX)(endY),
          i => gradientMap(startX)(startY)(i)(endY),
          i => gradientMap(startX)(startY)(endX)(i))
    
    for (dim <- dims) {
      for {x1 <- Range(0, width)
           y1 <- Range(0, height)
           x2 <- Range(0, width)
           y2 <- Range(0, height)} {
        dim = convolve(dim)
      }
    }
  }
}

object Heatmap4D {
  def main(args: Array[String]) {
    val s = samples
    println(s"mapping ${s.size} vectors")

    val size = 50
    
    // p -> number of points
    // r -> radius of filter
    // n -> size of one dimension of image

    //val hm = Heatmap4DBruteForce(size, size)       // O(p*r^4)
    //val hm = Heatmap4DBruteImperative(size, size)  // O(p*r^4)
    val hm = Heatmap4DBoxFilter(size, size)          // O(n^4) - this can be optimized  by remembering which pixles have values so that not every pixel needs to be iterated, only those within the radius of the points

    time("total") {
      ShowImage.showImage(pointsToImg(size*10, s))

      time("adding") {
        //s.foreach(v => hm.add(v.toI(size)))
        hm.run(s.map(_.toI(size)))
      }

      time("rendering") {
        ShowImage.showImage(hm.toImage)
      }
    }
  }
}

