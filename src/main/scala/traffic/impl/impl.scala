package traffic.impl

import akka.actor.ActorRef
import traffic._


class SimpleThresholdListener(notifier :ActorRef, aThreshold: Long, bThreshold: Long, cThreshold: Long, dThreshold: Long) extends CountListener {
  override def notify(counter: Counter, count: Long) = {
    counter match {
      case a: ACountNode if (count == aThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case b: BCountNode if (count == bThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case c: CCountNode if (count == cThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case d: DCountLeaf if (count == dThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case _ =>
        // do nothing
    }
  }
}

class IpAddressCountTree(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACountNode]
}

class ACountNode(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCountNode]
}

class BCountNode(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCountNode]
}

class CCountNode(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCountLeaf]
}

class DCountLeaf(name: String) extends LeafCount[IpAddress](name) {
}


