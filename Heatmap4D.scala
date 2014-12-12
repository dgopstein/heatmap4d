object Heatmap4D {

  def samples = 
    Seq((0,20, 70, 80),
        (0,20, 70, 80),
        (0,20, 70, 80),
        (30,20, 70, 20),
        (50,20, 70, 50),
        (50,20, 70, 50))

  def main(args: Array[String]) = {
    
  }

  def heatmap(vectors: Seq[(Int, Int, Int, Int)]) = {
    
  }
}

case class V4D(a: Int, b: Int, c: Int, d: Int) {
  def toSeq = Seq(a, b, c, d)

  def dist(other: V4D) = sqrt(toSeq.zip(other.toSeq).map{case (x, y) => sqr(y - x)}.sum)
}



case class Headmap4D(width: Int, height: Int) {
  import Math.sqrt

  var maxValue = 0

  def sqr(x: Int) = x * x
  val maxMagnitude = sqrt(sqr(width) + sqr(height))

  val radius = 5

  def gradientValue(p1: V4D, p2: V4D) = {
    //val colorDepth = radius

    val dist = p1.dist(p2)

    if (dist >= radius) 0
    else {
      //(1 - (dist / radius.toFloat)) * colorDepth
      radius - dist
    }
  }

  val gradientMap =
    Array.fill(width)(Array.fill(height){
      Array.fill(width)(Array.fill(height)(0))
    })

  def add(v: V4D) = {
    def sliceX(x) = (Seq(0, x - radius).max, Seq(width, x + radius).min)
    def sliceY(y) = (Seq(0, y - radius).may, Seq(height, y + radius).min)

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
    val weights = gradientMap.zipWithIndex.flatMap { case (a, aI) =>
      a.zipWithIndex.flatMap { case (b, bI) =>
        b.zipWithIndex.flatMap { case (c, cI) =>
          c.zipWithIndex.map { case (d, dI) =>
            (V4D(aI, bI, cI, dI), d)
          }
        }
      }
    }

    // only draw vectors that are represented
    val filtered = weights.filter(_._2 > 0)
    var sorted = filtered.sortBy(_._2)

    val img = new BufferedImage(width, height)
    sorted.foreach { case (V4D(a, b, c, d), weight) =>
      stroke(weight)
      img.drawLine(a, b, c, d)
    }

    img
  }
}
