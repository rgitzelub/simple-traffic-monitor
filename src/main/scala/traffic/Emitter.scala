package traffic

import akka.actor.{Actor, ActorLogging}



object Emitter {
  case object Stop
  case class CountToEmit(name: String, value: Long)
}

class Emitter extends Actor with ActorLogging {
  var countMessagesProcessed: Long = 0L
  var countSum: Long = 0L

  def receive = {
    case Emitter.CountToEmit(name, value) =>
      log.debug(s"${name}: ${value}")
      countMessagesProcessed += 1
      countSum += value

    case Emitter.Stop =>
      log.info(s"processed ${countMessagesProcessed} count messages with total sum ${countSum}")
      context.stop(self)
  }
}
