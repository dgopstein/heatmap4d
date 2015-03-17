import Math.sqrt
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, Graphics2D}
import scala.io.Source

case class V4D(a: Int, b: Int, c: Int, d: Int) {
  def toSeq = Seq(a, b, c, d)

  // Euclidean Distance
  def distance(other: V4D) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => Math.pow(y - x, 2)}.sum)
}

object Heatmap4D {
  def sqr(x: Int) = x * x

  def withTime[T](block: => T): (T, Long) = {
      val startTime = System.currentTimeMillis()
      val ret = block
      val elapsedTime = System.currentTimeMillis() - startTime
      (ret, elapsedTime)
  }

   // prints the number of milliseconds an expression takes to compute
  def time[R](msg: String)(block: => R): R = {
      val (res, elapsedTime) = withTime(block)
      println(msg + f" took: ${elapsedTime/1000.0}%.3fs")
      res
  }


  lazy val samples: Seq[V4D] = 
    Seq.fill(100) {
       Seq(Seq.fill(100)(V4D(20,30, 70, 20)),
           Seq.fill(50)(V4D(20,50, 70, 40)),
           Seq.fill(25)(V4D(20,70, 30, 10)),
           Range(0, 200).map { n =>
             V4D(40 + n % 4, 10 + n % 6, 60 + n % 8, 60 + n % 25)
           }
           ).flatten
    }.flatten

  //val size = 200
  val size = 100
  def main(args: Array[String]) {
    val s: Seq[V4D] = args.headOption.map(parseFile).getOrElse(samples)

    heatmap(s)
  }

  def heatmap(samples: Seq[V4D]) = {
    val hm: Heatmap4D = new Heatmap4D(size, size)

    val s = samples
    println(s"mapping ${s.size} vectors")

    time("total") {
      time("adding") {
        val printerval = 1000
        print(s"adding ${printerval} [")
        s.view.zipWithIndex.foreach { case (vect, i) =>
          if (i % printerval == 0) print(".")
          hm.add(vect)
        }
        println("]")
      }

      //ShowImage.showImage("/Users/dgopstein/nyu/subway/layout/heatmap_R68_random.png")
      time("rendering") {
        ShowImage.showImage(hm.toImage)
      }
    }
  }

  def parseFile(filename: String): Option[Seq[V4D]] = 
    Source.fromFile(filename).getLines.map(_.split(",").map(x => (x.toFloat * size).toInt)).map{ case Array(a: Int, b: Int, c: Int, d: Int) => V4D(a, b, c, d) }.toSeq
}

import Heatmap4D.sqr

case class Heatmap4D(width: Int, height: Int) {

  var maxValue = 0

  val maxMagnitude = sqrt(sqr(width) + sqr(height))

  val radius = 8

  def gradientValue(p1: V4D, p2: V4D): Int = {
    //val colorDepth = radius

    val distance = p1.distance(p2)

    val value = 
      if (distance >= radius) 0
      else {
        //(1 - (dist / radius.toFloat)) * colorDepth
        radius - Math.round(distance)
      }
    value.toInt
  }

  val gradientMap: Array[Array[Array[Array[Int]]]] =
    Array.fill(width)(Array.fill(height){
      Array.fill(width)(Array.fill(height)(0))
    })

  def add(v: V4D) = {
    def sliceX(x: Int) = (Seq(0, x - radius).max, Seq(width, x + radius).min)
    def sliceY(y: Int) = (Seq(0, y - radius).max, Seq(height, y + radius).min)

    val sA = sliceX(v.a)
    val sB = sliceY(v.b)
    val sC = sliceX(v.c)
    val sD = sliceY(v.d)
    gradientMap.zipWithIndex.slice(sA._1, sA._2).foreach { case (a, aI) =>
      a.zipWithIndex.slice(sB._1, sB._2).foreach { case (b, bI) =>
        b.zipWithIndex.slice(sC._1, sC._2).foreach { case (c, cI) =>
          Range(sD._1, sD._2).foreach { dI =>
            c(dI) += gradientValue(v, V4D(aI, bI, cI, dI))

            if (c(dI) > maxValue) maxValue = c(dI)
          }
        }
      }
    }
  }

  def toImage = {
    val printerval = 10
    print(s"rendering ${printerval} [")
    val weights = gradientMap.view./*par.*/zipWithIndex.flatMap { case (a, aI) =>
      if ( aI % 10 == 0 ) print("aI: "+aI);
      a.view.zipWithIndex.flatMap { case (b, bI) =>
        b.view.zipWithIndex.flatMap { case (c, cI) =>
          c.view.zipWithIndex.map { case (d, dI) =>
            (V4D(aI, bI, cI, dI), d)
          }
        }
      }
    }
    println("]")

    // only draw vectors that are represented
    val filtered = weights.filter(_._2 > 0)
    var sorted = filtered.sortBy(_._2)

    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    def interpColors(x: Float) = {
      val colors = Seq(Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED, Color.RED, Color.RED)
      
      //val start = (x * (colors.size - 2))
      //val startI = start.toInt
      //val offset = start - startI
      val offset = x
      //val Seq(a: Color, b: Color) = colors.slice(startI, startI + 2)
      val a = Color.BLACK
      val b = Color.WHITE

      val channels = 
       (offset * (a.getRed()/255.0) + (1-offset) * (b.getRed()/255.0),
        offset * (a.getGreen()/255.0) + (1-offset) * (b.getGreen()/255.0),
        offset * (a.getBlue()/255.0) + (1-offset) * (b.getBlue()/255.0))

      new Color(channels._1.toFloat, channels._2.toFloat, channels._3.toFloat)
    }

    val g2d = img.createGraphics();
    g2d.setBackground(Color.WHITE);
    g2d.setColor(Color.BLACK);
    sorted.foreach { case (V4D(a, b, c, d), weight) =>
      val intensity = weight / maxValue.toFloat
      g2d.setColor( interpColors(intensity) )

      g2d.setStroke(new BasicStroke(2));
      g2d.drawLine(a, b, c, d)
    }

    img
  }
}

import Heatmap4D._

object Histo4D {
  def histo(vects: Seq[V4D]) = {
    // percentage of interest
    val poi = .05

    val bins = vects
  }

  def main(args: Array[String]) {
    val s: Seq[V4D] = args.headOption.map(parseFile).getOrElse(samples)

    histo(s)
  }
}

