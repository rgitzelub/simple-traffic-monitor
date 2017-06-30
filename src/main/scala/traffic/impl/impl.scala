package traffic.impl

import akka.actor.ActorRef
import traffic._


class SimpleThresholdListener(notifier :ActorRef, aThreshold: Long, bThreshold: Long, cThreshold: Long, dThreshold: Long) extends CountListener {
  override def notify(counter: Counter, count: Long) = {
    counter match {
      case a: ACounterNode if (count == aThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case b: BCounterNode if (count == bThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case c: CCounterNode if (count == cThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case d: DCounterLeaf if (count == dThreshold) =>
        notifier ! ThresholdReached(counter.label, count)

      case _ =>
        // do nothing
    }
  }
}

class IpAddressTreeCounter(name: String) extends CounterNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACounterNode]
}

class ACounterNode(name: String) extends CounterNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCounterNode]
}

class BCounterNode(name: String) extends CounterNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCounterNode]
}

class CCounterNode(name: String) extends CounterNode[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCounterLeaf]
}

class DCounterLeaf(name: String) extends CounterLeaf[IpAddress](name) {
}


