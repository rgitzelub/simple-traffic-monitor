package traffic.by_ip_address

import traffic.{LeafCount, NodeCount}


class IpAddressCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACount]
}

class ACount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCount]
}


class BCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCount]
}

class CCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCount]
}

class DCount(name: String) extends LeafCount[IpAddress](name) {
}


