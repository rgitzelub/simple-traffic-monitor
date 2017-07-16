package traffic

import akka.actor.{Actor, DeadLetter}
import akka.event.Logging

class DeadLetterListener extends Actor {

  val log = Logging.getLogger(context.system, this)

  def receive = {
    case DeadLetter(msg, from, to) =>
      log.error(from.path + " failed to " + to.path)
  }
}
