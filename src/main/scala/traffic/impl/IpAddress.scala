package traffic.impl

import scala.util.Random



case class IpAddress(a: Int, b: Int, c: Int, d: Int)

object IpAddress {
  def random = IpAddress(
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1
  )

  // these give us a much smaller problem spaces to test with
  def random4 = IpAddress(
    Random.nextInt(4) + 1,
    Random.nextInt(4) + 1,
    Random.nextInt(4) + 1,
    Random.nextInt(4) + 1
  )

  // these give us a much smaller problem spaces to test with
  def random2 = IpAddress(
    Random.nextInt(2) + 1,
    Random.nextInt(2) + 1,
    Random.nextInt(2) + 1,
    Random.nextInt(2) + 1
  )


  def fromString(s: String): IpAddress = {
    s.split("\\.").toList match {
      case List(a, b, c, d) =>
        IpAddress(a.toInt, b.toInt, c.toInt, d.toInt)
      case x =>
        throw new RuntimeException(s"invalid ip address '${s}'")
    }
  }
}

