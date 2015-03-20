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

        //val distance = Math.sqrt(
        //    sqr(startX-x1)+
        //    sqr(startY-y1)+
        //    sqr(endX-x2) +
        //    sqr(endY-y2))

        gradientMap(x1)(y1)(x2)(y2) += gradientValue(p, V4DI(x1, y1, x2, y2))
        maxValue = max(maxValue, gradientMap(x1)(y1)(x2)(y2))
      }
    }
  }
}

object Heatmap4D {
  def main(args: Array[String]) {
    val s = samples
    println(s"mapping ${s.size} vectors")

    implicit val size = 50
    //val hm = Heatmap4DBruteForce(size, size)
    val hm = Heatmap4DBruteImperative(size, size)

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

