package traffic

import akka.actor.{Actor, ActorLogging}


case class CountToEmit(name: String, value: Int)


class Emitter extends Actor with ActorLogging {
  var countMessagesProcessed = 0
  var countSum = 0

  def receive = {
    case CountToEmit(name, value) =>
      log.info(s"${name}: ${value}")
      countMessagesProcessed += 1
      countSum += value

    case Stop =>
      log.info(s"processed ${countMessagesProcessed} count messages with total sum ${countSum}")
      context.stop(self)
  }
}
