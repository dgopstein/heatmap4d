import Math.sqrt
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, Graphics2D}

object Heatmap4D {
  def sqr(x: Int) = x * x
  def sqr(x: Double) = x * x

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

  val s1: Seq[V4DI] = Seq.fill(100) {
       Seq(Seq.fill(100)(V4DI(20,30, 17, 20)),
           Seq.fill(50)(V4DI(20,50, 17, 40)),
           Seq.fill(25)(V4DI(20,17, 30, 10)),
           Range(0, 50).map { n =>
             V4DI(4 + n % 4, 10 + n % 6, 26 + n % 8, 6 + n % 25)
           }
           ).flatten
    }.flatten

  val lineVsFan = Seq(
    Seq.fill(2)(V4DD(1/4f, 1/8f, 1/4f, 7/8f)),
    Range(0, 10).map { n =>
      V4DD(2/3f + (1/5f)*(n/10f-1/2f), 1/8f,
           2/3f - (1/5f)*(n/10f-1/2f), 7/8f)
    }
  ).flatten

  val samples: Seq[V4DD] = lineVsFan

  def gend2i(size: Int) = (d: Double) => (d * size).toInt

  def pointsToImg(size: Int, points: Seq[V4DD]) = {
    val img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    val d2i = gend2i(size)

    val g2d = img.createGraphics()
    g2d.setBackground(Color.getHSBColor(0,0,0))
    points.foreach { case V4DD(a, b, c, d) =>
      g2d.setColor(Color.getHSBColor(0f, 1f, 0f))
      g2d.setStroke(new BasicStroke(1))
      g2d.drawLine(d2i(a), d2i(b), d2i(c), d2i(d))
    }
    img
  }

  def main(args: Array[String]) {
    val s = samples
    println(s"mapping ${s.size} vectors")

    implicit val size = 50
    val hm = Heatmap4D(size, size)

    time("total") {
      ShowImage.showImage(pointsToImg(size*10, s))

      time("adding") {
        s.foreach(v => hm.add(v.toI(size)))
      }

      time("rendering") {
        ShowImage.showImage(hm.toImage)
      }
    }
  }
}

import Heatmap4D.{gend2i, sqr}

trait V4D[T] {
  val a: T
  val b: T
  val c: T
  val d: T

  def toSeq: Seq[T] = Seq(a, b, c, d)

  def dist(other: V4D[T]): Double
}

case class V4DI(a: Int, b: Int, c: Int, d: Int) extends V4D[Int] {
  def dist(other: V4D[Int]) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => sqr(y - x)}.sum)
}

case class V4DD(a: Double, b: Double, c: Double, d: Double) extends V4D[Double]  {
  def dist(other: V4D[Double]) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => sqr(y - x)}.sum)

  def toI(size: Int) = {
    val d2i = gend2i(size)
    V4DI(d2i(a), d2i(b), d2i(c), d2i(d))
  }
}



case class Heatmap4D(width: Int, height: Int) {

  var maxValue = 0

  val maxMagnitude = sqrt(sqr(width) + sqr(height))

  val radius = 5

  def gradientValue(p1: V4DI, p2: V4DI): Int = {
    //val colorDepth = radius

    val dist = p1.dist(p2)

    val value = 
      if (dist >= radius) 0
      else {
        //(1 - (dist / radius.toFloat)) * colorDepth
        radius - Math.round(dist)
      }
    value.toInt
  }

  val gradientMap: Array[Array[Array[Array[Int]]]] =
    Array.fill(width)(Array.fill(height){
      Array.fill(width)(Array.fill(height)(0))
    })

  def add(v: V4DI) = {
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
            c(dI) += gradientValue(v, V4DI(aI, bI, cI, dI))

            if (c(dI) > maxValue) maxValue = c(dI)
          }
        }
      }
    }
  }

  def toImage = {
    val weights = gradientMap.view./*par.*/zipWithIndex.flatMap { case (a, aI) =>
      if ( aI % 10 == 0 ) println("aI: "+aI);
      a.view.zipWithIndex.flatMap { case (b, bI) =>
        b.view.zipWithIndex.flatMap { case (c, cI) =>
          c.view.zipWithIndex.map { case (d, dI) =>
            (V4DI(aI, bI, cI, dI), d)
          }
        }
      }
    }

    // only draw vectors that are represented
    val filtered = weights.filter(_._2 > 0)
    var sorted = filtered.sortBy(_._2)

    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    val g2d = img.createGraphics()
    g2d.setBackground(Color.getHSBColor(0, 0, 0))
    sorted.foreach { case (V4DI(a, b, c, d), weight) =>
      val intensity = 1 - (weight / maxValue.toFloat)
      g2d.setColor(Color.getHSBColor(0, 0, intensity))
      g2d.setStroke(new BasicStroke(1))
      g2d.drawLine(a, b, c, d)
    }

    img
  }
}












import swing._                                                                

import java.awt.image.BufferedImage                                           
import java.io.File                                                           
import javax.imageio.ImageIO                                                  

class ImagePanel(bufferedImage: BufferedImage) extends Panel {                                                                             
  override def paintComponent(g:Graphics2D) = {                                                                           
    if (null != bufferedImage) g.drawImage(bufferedImage, 0, 0, null)         
  }                                                                           
}                                                                             

case class ImagePanelDemo(img: BufferedImage) extends SimpleSwingApplication {
  def top = new MainFrame { title = "Image Panel Demo"; contents = new ImagePanel(img)}
}

object ShowImage {
  def showImage(path: String) { showImage(javax.imageio.ImageIO.read(new java.io.File(path))) }
  def showImage(img: BufferedImage) {
    import javax.swing._
    import java.awt.{Dimension, Graphics}
  
    val frame = new JFrame()
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  
    val panel = new JPanel(){
        override def paintComponent(g: Graphics){
            super.paintComponent(g)
            val g2 = g.create()
            g2.drawImage(img, 0, 0, getWidth(), getHeight(), null)
            g2.dispose()
        }
  
        override def getPreferredSize = {
            new Dimension(img.getWidth(), img.getHeight())
        }
    }
  
    frame.add(panel)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }
}
