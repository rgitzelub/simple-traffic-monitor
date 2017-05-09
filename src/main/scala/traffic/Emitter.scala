package traffic

import akka.actor.Actor


case class CountToEmit(name: String, value: Int)


class Emitter extends Actor {
  var countMessagesProcessed = 0
  var countSum = 0

  def receive = {
    case CountToEmit(name, value) =>
      println(s"${name}: ${value}")
      countMessagesProcessed += 1
      countSum += value

    case Stop =>
      println(s"processed ${countMessagesProcessed} count messages with total sum ${countSum}")
      context.stop(self)
  }
}
