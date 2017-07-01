package traffic.impl

import akka.actor.ActorRef
import traffic._


class SimpleThresholdListener(notifier :ActorRef, aThreshold: Long, bThreshold: Long, cThreshold: Long, dThreshold: Long) extends CountListener {
  override def notify(counter: Counter, count: Long) = {
    counter match {
      case a: ACounterTreeNode if (count == aThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case b: BCounterTreeNode if (count == bThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case c: CCounterTreeNode if (count == cThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case d: DCounterTreeLeaf if (count == dThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case _ =>
        // do nothing
    }
  }
}

class IpAddressTreeCounter(name: String) extends CounterTreeNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACounterTreeNode]
}

class ACounterTreeNode(name: String) extends CounterTreeNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCounterTreeNode]
}

class BCounterTreeNode(name: String) extends CounterTreeNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCounterTreeNode]
}

class CCounterTreeNode(name: String) extends CounterTreeNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCounterTreeLeaf]
}

class DCounterTreeLeaf(name: String) extends CounterTreeLeaf[IpAddress](name) {
}


