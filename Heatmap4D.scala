package heatmap4d

// head -200 trip_data_1_coords.csv | grep -v ',0,' | ruby -lane 'c = $_.split(",").map(&:to_f); lat=->(x){(x+74.05)*8}; lon=->(x){(x-40.7)*8}; puts [lat[c[0]],lon[c[1]],lat[c[2]],lon[c[3]]].join(",")' | grep -v ',[-1]' > normalized_taxi_200.csv

import heatmap4d.HeatmapLib._
import Heatmap4D._
import java.awt.image.BufferedImage

case class Heatmap4DBruteForce(size: Int, radiusPct: Double) extends HeatmapLib(size) {
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

case class Heatmap4DBruteImperative(size: Int, radiusPct: Double) extends HeatmapLib(size) {
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

case class Heatmap4DHashMap(size: Int, radiusPct: Double) extends HeatmapLib(size) {
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


case class Heatmap4DMeanShift(size: Int, radiusPct: Double) extends HeatmapLib(size) {

  import Math.{min, max}

  var gradientHashMap: Map[V4DI, Double] = Map()

  override def pointWeights = gradientHashMap.toSeq


  def run(points: Seq[V4DI]) = {
    // One line per cluster
    //gradientHashMap = MeanShift.meanShift4D(points.map(_.toV4DD)).groupBy(identity).map{case (k,v) => k.toV4DI -> v.size.toDouble}

    // Multiple lines per cluster
    gradientHashMap = points.zip(MeanShift.meanShift4D(radius, points.toVector.map(_.toV4DD))).groupBy(_._2)
      .flatMap{case (k: V4DD, pairs: Seq[(V4DI, V4DD)]) => pairs.map{_._1 -> pairs.size.toDouble}}

    maxValue = gradientHashMap.values.max
  }
}

case class Heatmap4DBucketGrid(size: Int, radiusPct: Double) extends HeatmapLib(size) {

  import Math.{min, max}

  var pointBucketWeights: Seq[(V4DI, Double)] = Seq()
  override def pointWeights: Seq[(V4DI, Double)] = {
    //println("pointBucketWeights.size: "+pointBucketWeights)
    pointBucketWeights
  }

  val bucketMap = new collection.mutable.HashMap[Bucket, Double]() {
    override def default(key: Bucket) = 0.0
  }

  case class Bucket(a: Int, b: Int, c: Int, d: Int) {
    val center = {
      def scale(i: Int) = (i * Bucket.size) - (Bucket.size / 2)
      val center = V4DI(scale(a), scale(b), scale(c), scale(d))
//      println("bucket -> center: "+(this, center))
      center
    }
  }
  object Bucket {
    val size = ((2/3d) * radius).toInt
  }

  def bucket(pt: V4DI): Bucket = pt match {
    case V4DI(a, b, c, d) =>
      Bucket(a / Bucket.size, b / Bucket.size, c / Bucket.size, d / Bucket.size)
  }

  // every offset from a point in which it should be inserted
  def bucketOffsets = Seq(1,1,1,1,0,0,0,0).combinations(4).flatMap(_.permutations)

  def buckets(pt: V4DI): Iterator[Bucket] = bucket(pt) match {
    case Bucket(a, b, c, d) => bucketOffsets.map {
      case List(oA, oB, oC, oD) => Bucket(a + oA, b + oB, c + oC, d + oD)
    }
  }

  def run(points: Seq[V4DI]) = {

    // Loop all points
    for (p@V4DI(startX, startY, endX, endY) <- points) {
      //println("p -> bucket: "+(p, bucket(p)))
      buckets(p).foreach{ bucket =>
        //println("p -> value: "+(p, bucket.center, radius, p.dist(bucket.center)))
        bucketMap(bucket) += max(radius - p.dist(bucket.center), 0)
//          bucketMap(bucket) += p.dist(bucket.center)

          maxValue = max(maxValue, bucketMap(bucket))
        }

    }

    pointBucketWeights = points.map{ case p@V4DI(startX, startY, endX, endY) =>
      //println("p -> map: "+(p, bucketMap(bucket(p))))
      p -> bucketMap(bucket(p))
    }
  }
}

// http://web.archive.org/web/20060718054020/http://www.acm.uiuc.edu/siggraph/workshops/wjarosz_convolution_2001.pdf
case class Heatmap4DConvolution(size: Int, radiusPct: Double) extends HeatmapLib(size) {

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
  type A4D = Array[Array[Array[Array[Double]]]]

  def baselineDistance(img: BufferedImage) =
    ShowImage.diffImages(ShowImage.readImage(""), img)

  def main(args: Array[String]) {
    def fileToV4DDs(filename: String) =
      io.Source.fromFile(filename).getLines()
        .map(_.split (',').map(_.toDouble)).toSeq.map {
        case Array (a, b, c, d) => V4DD (a, b, c, d)
      }.toSeq


    val classMap = Map(
      "BruteForce" -> Heatmap4DBruteForce,
      "BruteImperative" -> Heatmap4DBruteImperative,
      "Convolution" -> Heatmap4DConvolution,
      "HashMap" -> Heatmap4DHashMap,
      "MeanShift" -> Heatmap4DMeanShift,
      "BucketGrid" -> Heatmap4DBucketGrid)

    val algoName = args(0)

    val (algo, s, radiusPct) =
      args match {
        case Array(a) => (classMap(a), samples, 0.05)
        case Array(a, filename) => (classMap(a), fileToV4DDs(filename), 0.05)
        case Array(a, filename, rpct) => (classMap(a), fileToV4DDs(filename), rpct.toDouble)
      }




    val size = 1000
    println(s"image size: " + size + "^4")
    println(s"point pairs:" + s.size)
    
    // p -> number of points
    // r -> radius of filter
    // n -> size of one dimension of image

    //val hm = Heatmap4DBruteForce(size, size)       // O(p*r^4)
    //val hm = Heatmap4DBruteImperative(size, size)  // O(p*r^4)
    //val hm = Heatmap4DConvolution(size)          // O(n^4*r^4)
    //val hmhm = Heatmap4DHashMap(size, radiusPct)
    //val hm = Heatmap4DMeanShift(size, radiusPct)
    //val hm = Heatmap4DBucketGrid(size, radiusPct)
    val hm = algo(size, radiusPct)

    def meanSquareError(pointWeights: Seq[(V4DI, Double)], array4d: collection.mutable.HashMap[V4DI, Double]) = {
      // min is assumed to be 0
      val srcMax = 1//pointWeights.maxBy(_._2)._2
      val destMax = 1//array4d.values.max
      pointWeights.map { case (point, weight) =>
        println((weight/srcMax, array4d(point)/destMax)); sqr(weight/srcMax - array4d(point)/destMax)}.sum
    }

    // http://en.wikipedia.org/wiki/Root-mean-square_deviation
    def rmsd(pointWeights: Seq[(V4DI, Double)], array4d: collection.mutable.HashMap[V4DI, Double]) =
      Math.sqrt(meanSquareError(pointWeights, array4d) / pointWeights.size) / pointWeights.maxBy(_._2)._2

    println("kernel radius: " + hm.radius)

    time("total") {
      ShowImage.showImage(pointsToImg(size, s))

      time("adding") {
        hm.run(s.map(_.toI(size)))

        //println("running hashmap")
        //hmhm.run(s.map(_.toI(size)))

       //println("root mean square deviation: "+rmsd(hm.pointWeights, hmhm.gradientHashMap))
      }

      time("rendering") {

        val filename = s"output/heatmap4d_${algoName}_${size}_${s.size}_${hm.radius}_${System.currentTimeMillis / 1000}"
        //reflect.io.File(filename+".json").writeAll(arrayToJson(hm.gradientMap))

        Printer.printToFile(new java.io.File(filename+".json")) { _.write(hm.toJson) }
        ShowImage.saveImage(hm.toImage, filename+".png")
        ShowImage.showImage(hm.toImage)
      }
    }
  }

}

import scala.annotation.tailrec
object MeanShift {

  /*
  def sum4D(pts: Seq[V4DD]) = {
    @tailrec def sum1(pts1: Seq[V4DD], sumV: V4DD): V4DD = {
      val V4DD(a1, b1, c1, d1, n1) = sumV
      pts1 match {
        case V4DD(a, b, c, d, n) #:: xs => sum1(xs, V4DD(n*a + a1, n*b + b1, n*c + c1, n*d + d1, n + n1))
        case V4DD(a, b, c, d, n) :: xs => sum1(xs, V4DD(n*a + a1, n*b + b1, n*c + c1, n*d + d1, n + n1))
        case Nil => sumV
      }
    }

    sum1(pts, V4DD(0, 0, 0, 0))
  }

  def mean4D(pts: Seq[V4DD]) = {
    val V4DD(a, b, c, d, n) = sum4D(pts)
    V4DD(a / n, b / n, c / n, d / n)
  }
  */
  def mean4D(pts: Seq[V4DD]) = {
    var sumA = 0d
    var sumB = 0d
    var sumC = 0d
    var sumD = 0d
    var sumN = 0

    for (pt <- pts) {
      sumA += pt.a * pt.count
      sumB += pt.b * pt.count
      sumC += pt.c * pt.count
      sumD += pt.d * pt.count
      sumN += pt.count
    }

    V4DD(sumA/sumN, sumB/sumN, sumC/sumN, sumD/sumN)
  }

  def variance4D(pts: Seq[V4DD]) = {
    val mean = //time("mean") {
      mean4D(pts)//}
    val errs = //time("errors"){
      pts.map(_.manhattanDist(mean))//}
    val variance = //time("sum") {
        errs.sum / errs.size //}
    variance
  }

  // Selects the node in question along with others
  // Likely selects a->b as well as b->a (depending on how its called
  def inWindow(pts: Seq[V4DD], pt: V4DD, windowSize: Double) = pts.filter(_.dist(pt) < windowSize)
  def inWindowKD(pts: KdTree, pt: V4DD, windowSize: Double) = {

    val windowPts = pts.radiusQuery(pt, windowSize)

    //println("window points: "+windowPts)

    windowPts
  }

  def meanShiftStep4D(windowSize: Double, pts: Seq[V4DD]) = {
    val ptsKd = /*time("kdtree: ") {*/ KdTree(KdTree.purturb(pts)) //}
    val windows = //time("windows: ") {
      pts.map(pt => inWindowKD(ptsKd, pt, windowSize)) // {10k: 110s, 5k: 34}
      //pts.map(pt => inWindow(pts, pt, windowSize)) // {10k: 24s, 5k: 6s} n^2
    //}
    //val windows = pts.map(pt => inWindow(pts, pt, windowSize))
    val newMeans = /*time("new means: ") {*/ windows.map(mean4D) //}

    newMeans
  }

  def meanShift4D(windowSize: Double, pts: Seq[V4DD]) = {
    var newPts = pts

    var oldVariance = 0.0
    var varianceDelta = 99999999d
    for (i <- 0 to 9 if Math.abs(varianceDelta) > 0.000001) {

      time(" ") {
        val variance = variance4D(newPts)
        varianceDelta = oldVariance - variance
        oldVariance = variance


        print(s"[$i] varianceDelta: " + varianceDelta)
      }

      time(s"[$i] meanShift") {
        newPts = meanShiftStep4D(windowSize, pts) //TODO make sure this doesn't return a Stream... they're slow
      }
    }

    newPts
  }

}
