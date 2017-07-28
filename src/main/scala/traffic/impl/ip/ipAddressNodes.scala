package traffic.impl.ip

import traffic.counting.{CounterTreeLeaf, CounterTreeNode}


class HitTreeCounter(name: String) extends CounterTreeNode[PageHit](name) {
  def childNodeLabel(value: PageHit) = s"${value.address.a}.x.x.x"
  def childActorName(value: PageHit) = context.self.path.name + "-" + value.address.a
  val childClass = classOf[ACounterTreeNode]
}

class ACounterTreeNode(name: String) extends CounterTreeNode[PageHit](name) {
  def childNodeLabel(value: PageHit) = s"${value.address.a}.${value.address.b}.x.x"
  def childActorName(value: PageHit) = context.self.path.name + "-" + value.address.b
  val childClass = classOf[BCounterTreeNode]
}

class BCounterTreeNode(name: String) extends CounterTreeNode[PageHit](name) {
  def childNodeLabel(value: PageHit) = s"${value.address.a}.${value.address.b}.${value.address.c}.x"
  def childActorName(value: PageHit) = context.self.path.name + "-" + value.address.c
  val childClass = classOf[CCounterTreeNode]
}

class CCounterTreeNode(name: String) extends CounterTreeNode[PageHit](name) {
  def childNodeLabel(value: PageHit) = s"${value.address.a}.${value.address.b}.${value.address.c}.${value.address.d}"
  def childActorName(value: PageHit) = context.self.path.name + "-" + value.address.d
  val childClass = classOf[DCounterTreeLeaf]
}

class DCounterTreeLeaf(name: String) extends CounterTreeLeaf[PageHit](name) {
}


