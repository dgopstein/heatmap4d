package heatmap4d

import KdTree._

trait KdTree {
  //val axisPoint = axisGetter(point)

  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD]

  def isContained(center: V4DD, radius: Double): Boolean
  def isIntersected(center: V4DD, radius: Double): Boolean

  def toSeq: Seq[V4DD]
}


case class Node(depth: Int, left: Option[KdTree], right: Option[KdTree]) extends KdTree {
  val axisGetter = getAxisGetter(depth)

  def isContained(center: V4DD, radius: Double): Boolean =
    left.map(_.isContained(center, radius)).getOrElse(true) &&
    right.map(_.isContained(center, radius)).getOrElse(true)

  def isIntersected(center: V4DD, radius: Double): Boolean =
    left.exists(_.isIntersected(center, radius)) ||
    right.exists(_.isIntersected(center, radius))

  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD] = {
    val newLeft =
      if ( left.exists(_.isContained(center, radius))) left.toSeq.flatMap(_.toSeq) else left.toSeq.flatMap(_.radiusQuery(center, radius))
    val newRight =
      if (right.exists(_.isContained(center, radius))) right.toSeq.flatMap(_.toSeq) else right.toSeq.flatMap(_.radiusQuery(center, radius))

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

case class Leaf(point: V4DD) extends KdTree {
  def radiusQuery(center: V4DD, radius: Double): Seq[V4DD] =
    if (isContained(center, radius)) Seq(center) else Nil

  def isContained(center: V4DD, radius: Double) =
    center.dist(point) < radius && center.dist(point) < radius

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

  private def buildKdTree(points: Seq[V4DD], depth: Int = 0): KdTree = {
    if (points.size == 1) {
      Leaf(points(0))
    } else {
      val axisGetter = getAxisGetter(depth)

      val median = approxMedian(axisGetter, points)

      val (lower, upper) =
        points.partition(p => axisGetter(p) < axisGetter(median))
      val left = if (lower.nonEmpty) Some(buildKdTree(lower, depth + 1)) else None
      val right = if (upper.nonEmpty) Some(buildKdTree(upper, depth + 1)) else None

      Node(depth, left, right)
    }
  }

  def test = {
    KdTree.buildKdTree(Seq(V4DD(1, 2, 3, 4), V4DD(1, 1, 1, 1), V4DD(2, 2, 2, 2), V4DD(1, 1, 2, 2)))
  }
}
