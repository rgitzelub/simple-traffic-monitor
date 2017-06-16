package traffic.by_ip_address

import scala.util.Random



case class IpAddress(a: Int, b: Int, c: Int, d: Int)

object IpAddress {
  def random = IpAddress(
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1,
    Random.nextInt(256) + 1
  )
  // this gives us a much smaller problem space to test with
  def randomSimplistic = IpAddress(
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

