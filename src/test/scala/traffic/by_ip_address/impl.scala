package traffic.by_ip_address

import akka.actor.ActorRef
import traffic.{LeafCount, NodeCount, Notifier, ThresholdReached}


class IpAddressCountTree(name: String, notifier: ActorRef) extends NodeCount[IpAddress](name, notifier) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACountNode]
  override def notifyAndPossiblySend(count: Int, notifier: ActorRef) = 
    if(count == 100) {
      notifier ! ThresholdReached(name, count)
    }
}

class ACountNode(name: String, notifier: ActorRef) extends NodeCount[IpAddress](name, notifier) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCountNode]
  override def notifyAndPossiblySend(count: Int, notifier: ActorRef) =
    if(count == 10) {
      notifier ! ThresholdReached(name, count)
    }
}

class BCountNode(name: String, notifier: ActorRef) extends NodeCount[IpAddress](name, notifier) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCount]
  override def notifyAndPossiblySend(count: Int, notifier: ActorRef) =
    if(count == 5) {
      notifier ! ThresholdReached(name, count)
    }
}

class CCount(name: String, notifier: ActorRef) extends NodeCount[IpAddress](name, notifier) {
  def childNodeLabel(value: IpAddress) = s"${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCountLeaf]
  override def notifyAndPossiblySend(count: Int, notifier: ActorRef) =
    if(count == 4) {
      notifier ! ThresholdReached(name, count)
    }
}

class DCountLeaf(name: String, notifier: ActorRef) extends LeafCount[IpAddress](name, notifier) {
  override def notifyAndPossiblySend(count: Int, notifier: ActorRef) =
    if(count == 2) {
      notifier ! ThresholdReached(name, count)
    }
}


