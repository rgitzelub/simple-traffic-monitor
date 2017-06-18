package traffic

import akka.actor.{Actor, ActorLogging}


case class ThresholdReached(name: String, count: Long)


class Notifier extends Actor with ActorLogging {
  def receive = {
    case t: ThresholdReached =>
      log.info(s"${t.name} reached ${t.count}")
  }
}
