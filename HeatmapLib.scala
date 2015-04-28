package heatmap4d

import Math.sqrt
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, Graphics2D}

import HeatmapLib.{gend2i, sqr}

trait V4D[T] {
  val a: T
  val b: T
  val c: T
  val d: T

  val toSeq: Seq[T] = Seq(a, b, c, d)

  def dist(other: V4D[T]): Double
}

case class V4DI(a: Int, b: Int, c: Int, d: Int) extends V4D[Int] {
  def dist(other: V4D[Int]) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => sqr(y - x)}.sum)
  lazy val toV4DD = V4DD(a, b, c, d)
}

case class V4DD(a: Double, b: Double, c: Double, d: Double, count: Int = 1) extends V4D[Double]  {
  def dist(other: V4D[Double]) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => sqr(y - x)}.sum)
  def manhattanDist(o: V4D[Double]) = Math.abs(a - o.a) + Math.abs(b - o.b) + Math.abs(c - o.c) + Math.abs(d - o.d)

  def toI(size: Int) = {
    val d2i = gend2i(size)
    V4DI(d2i(a), d2i(b), d2i(c), d2i(d))
  }

  lazy val toV4DI = V4DI(a.round.toInt, b.round.toInt, c.round.toInt, d.round.toInt)
}

object HeatmapLib {
  def sqr(x: Int) = x * x
  def sqr(x: Double) = x * x

  def arrayToJson[T <: Any](a: Array[T]): String = a match {
    case ao: Array[Double] => ao.mkString("[", ",", "]")
    case aa: Array[Array[Double]] => aa.map(arrayToJson).mkString("[", ",", "]")
    case aa: Array[Array[Array[Double]]] => aa.map(arrayToJson).mkString("[", ",", "]")
    case aa: Array[Array[Array[Array[Double]]]] => aa.map(arrayToJson).mkString("[", ",", "]")
  }

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
    Seq.fill(20)(V4DD(1/4f, 1/8f, 1/4f, 7/8f)),
    Range(0, 100).map { n =>
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
    points.foreach { case V4DD(a, b, c, d, _) =>
      g2d.setColor(Color.getHSBColor(0f, 1f, 0f))
      g2d.setStroke(new BasicStroke(1))
      g2d.drawLine(d2i(a), d2i(b), d2i(c), d2i(d))
    }
    img
  }
}





abstract class HeatmapLib(size: Int) {
  val width = size
  val height = size

  def run(points: Seq[V4DI])

  var maxValue = 0d

  val maxMagnitude = sqrt(sqr(width) + sqr(height))

  val radiusPct: Double
  val radius = (size * radiusPct).round.toInt
  //val radius

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

  lazy val gradientMap: Array[Array[Array[Array[Double]]]] =
    Array.fill(width)(Array.fill(height){
      Array.fill(width)(Array.fill(height)(0))
    })

  def pointWeights: Seq[(V4DI, Double)] = {
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
    weights
  }

  def toImage = {
    val weights = pointWeights

    // only draw vectors that are represented
    val filtered = weights.filter(_._2 > 0)
    var sorted = filtered.sortBy(_._2)

    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    val g2d = img.createGraphics()
    g2d.setBackground(Color.getHSBColor(0, 0, 0))
    sorted.foreach { case (V4DI(a, b, c, d), weight) =>
      val norm = Math.log10 _
      val intensity = 1 - (norm(weight) / norm(maxValue.toDouble))
      val color1 = Color.getHSBColor((1.0-intensity).toFloat % 1f, (0.75 - 0.5*intensity).toFloat, intensity.toFloat)
      val color = new Color(color1.getRed, color1.getGreen, color1.getBlue, ((1f-.7*intensity) * 255).toInt)
      g2d.setColor(color)
      g2d.setStroke(new BasicStroke(1))
      g2d.drawLine(a, b, c, d)
    }

    g2d.setColor(Color.black)
    g2d.drawOval((radius*1.5).toInt, (radius*1.5).toInt, radius, radius)

    img
  }
}












import javax.swing._                                                                
import java.awt.Panel
import java.awt.image.BufferedImage                                           
import java.io.File                                                           
import javax.imageio.ImageIO                                                  

class ImagePanel(bufferedImage: BufferedImage) extends Panel {                                                                             
  /* override */ def paintComponent(g:Graphics2D) = {                                                                           
    if (null != bufferedImage) g.drawImage(bufferedImage, 0, 0, null)         
  }                                                                           
}                                                                             

// case class ImagePanelDemo(img: BufferedImage) extends SimpleSwingApplication {
//   def top = new MainFrame { title = "Image Panel Demo"; contents = new ImagePanel(img)}
// }

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

  def saveImage(img: BufferedImage, path: String) =
    ImageIO.write(img, "png", new File("output/"+path))

  def readImage(path: String) =
    ImageIO.read(new java.io.File(path))

  def arrayDistance(a1: Array[Int], a2: Array[Int]) =
    a1.zip(a2).map{ case (x, y) => sqr(y - x) }.sum

  def diffImages(img1: BufferedImage, img2: BufferedImage) = {
    val w = img1.getWidth
    val h = img1.getHeight
    val rgb1 = img1.getRGB(0, 0, w, h, null, 0, w)
    val rgb2 = img2.getRGB(0, 0, w, h, null, 0, w)

    arrayDistance(rgb1, rgb2)
  }
    
}
