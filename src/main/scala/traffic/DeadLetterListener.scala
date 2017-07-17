package traffic

import akka.actor.{Actor, ActorLogging, DeadLetter}


class DeadLetterListener extends Actor with ActorLogging {

  def receive = {
    case DeadLetter(msg, from, to) =>
      log.error(from.path + " failed to " + to.path + ": " + msg.toString.substring(0,100))
  }
}
