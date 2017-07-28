package traffic.impl.ip

import akka.actor.ActorRef
import traffic._
import traffic.counting._


class SimpleThresholdListener[T <: Countable](notifier :ActorRef, aThreshold: Long, bThreshold: Long, cThreshold: Long, dThreshold: Long) extends CountListener {
  override def notify(counter: Counter[_], count: Long) = {
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

