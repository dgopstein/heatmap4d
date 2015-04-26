package heatmap4d

import KdTree._

trait KdTree {
  //val axisPoint = axisGetter(point)

  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD]

  //def isContained(center: V4DD, radius: Double): Boolean
  def isIntersected(center: V4DD, radius: Double): Boolean

  def toSeq: Seq[V4DD]
}


case class Node(depth: Int, median: V4DD, left: Option[KdTree], right: Option[KdTree]) extends KdTree {
  val axisGetter = getAxisGetter(depth)

  //def isContained(center: V4DD, radius: Double): Boolean =
  //  left.map(_.isContained(center, radius)).getOrElse(true) &&
  //  right.map(_.isContained(center, radius)).getOrElse(true)

  def isIntersected(center: V4DD, radius: Double): Boolean =
    center.dist(median) < radius // Speed up by doing an axis-only comparison?

  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD] = {
    val (newLeft, newRight) =
      if (axisGetter(center) + radius < axisGetter(median)) {
        //println(s"[${depth%4}] ${center} -> left@${median}")
        (left.toSeq.flatMap(_.radiusQuery(center, radius)), Nil)
      } else if (axisGetter(center) - radius > axisGetter(median)) {
        //println(s"[${depth%4}] ${center} -> right@${median}")
        (Nil, right.toSeq.flatMap(_.radiusQuery(center, radius)))
      } else {
        //println(s"[${depth%4}] ${center} -> splitting@${median}")
        (left.toSeq.flatMap(_.radiusQuery(center, radius)),
          right.toSeq.flatMap(_.radiusQuery(center, radius)))
      }

    newLeft ++ newRight
  }

  def toSeq: Seq[V4DD] =
    left.toSeq.flatMap(_.toSeq) ++ right.toSeq.flatMap(_.toSeq)

  //def radiusQuery(center: V4DD, radius: Double): Option[KdTree] = {
  //  val newLeft =
  //    if ( left.exists(_.isContained(center, radius))) left else left.flatMap(_.radiusQuery(center, radius))
  //  val newRight =
  //    if (right.exists(_.isContained(center, radius))) right else right.flatMap(_.radiusQuery(center, radius))
  //
  //  Node(depth, newLeft, )
  //}
}

case class Leaf(point: V4DD, count: Int = 1) extends KdTree {
  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD] =
    if (isContained(center, radius)) {
      //println("Leaf.isContained: "+point);
      Seq(point)
    } else {
      //println("Leaf.isContained: "+false);
      Nil
    }

  def isContained(center: V4DD, radius: Double) =
    center.dist(point) < radius

  def isIntersected(center: V4DD, radius: Double) = isContained(center, radius)

  val toSeq = Seq(point)
}

object KdTree {
  def getAxisGetter(depth: Int) = {
    val axis = depth % 4 // 4 == number of axes
    val axisGetters: Seq[Function1[V4DD, Double]] = Seq(_.a, _.b, _.c, _.d)
    axisGetters(axis)
  }

  private def approxMedian[T](f: T => Double, s: Seq[T]) = {
    val n = 100
    val sorted = s.take(n).sortBy(f)
    sorted(sorted.size/2)
  }

  // to keep points in general position
  def purturb(a: Seq[V4DD]) =
    a.map{case V4DD(a, b, c, d, n) => V4DD(a, b, c, d + 0.00000001 * Math.random(), n)}

  def apply(points: Seq[V4DD]) = buildKdTree(points, 0)

  def shortcutEquality[T](a: Seq[T], b: Seq[T]) = a.zip(b).forall(t => t._1 == t._2)

  def undup(a: Seq[V4DD]): Seq[V4DD] = a.groupBy(identity).map{case (k, v) => V4DD(k.a, k.b, k.c, k.d, v.size)}.toSeq

  private def buildKdTree(points: Seq[V4DD], depth: Int): KdTree = {
//    List(V4DD(602.0,551.0,602.0,551.0), V4DD(602.0,551.0,602.0,551.0), V4DD(607.0,553.0,607.0,553.0), V4DD(602.0,551.0,602.0,551.0))
//    println(points.toList)

    if (points.tail.isEmpty) {
      Leaf(points.head)
    } else {
      val axisGetter = getAxisGetter(depth)

      lazy val median = approxMedian(axisGetter, points)

      //val (lowerDups, upperDups) =
      val (lower, upper) =
        points.partition(p => axisGetter(p) < axisGetter(median))

      //val lower = if (shortcutEquality(points, lowerDups)) undup(lowerDups) else lowerDups
      //val upper = if (shortcutEquality(points, upperDups)) undup(upperDups) else upperDups

      val left = if (lower.nonEmpty) Some(buildKdTree(lower, depth + 1)) else None
      val right = if (upper.nonEmpty) Some(buildKdTree(upper, depth + 1)) else None

      Node(depth, median, left, right)
    }
  }

  def test = {
    val kd = KdTree(Seq(V4DD(1, 2, 3, 4), V4DD(1, 1, 1, 1), V4DD(2, 2, 2, 2), V4DD(1, 1, 2, 2)))

    Seq(
      kd.radiusQuery(V4DD(1,1,1,1), 1.0) == List(V4DD(1.0,1.0,1.0,1.0,1)),
      kd.radiusQuery(V4DD(1,1,1,1), 4.0) == List(V4DD(1.0,1.0,1.0,1.0,1), V4DD(1.0,1.0,2.0,2.0,1), V4DD(2.0,2.0,2.0,2.0,1), V4DD(1.0,2.0,3.0,4.0,1)),
      kd.radiusQuery(V4DD(1,1,1,1), 3.0) == List(V4DD(1.0,1.0,1.0,1.0,1), V4DD(1.0,1.0,2.0,2.0,1), V4DD(2.0,2.0,2.0,2.0,1)),
      kd.radiusQuery(V4DD(1,1,1,1), 2.0) == List(V4DD(1.0,1.0,1.0,1.0,1), V4DD(1.0,1.0,2.0,2.0,1))
    ).foreach(println)

    kd
  }
}
