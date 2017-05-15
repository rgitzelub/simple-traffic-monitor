package traffic

import akka.actor._



class Terminator(ref: ActorRef) extends Actor with ActorLogging {
  context watch ref

  def receive = {
    case Terminated(_) =>
      log.info(s"${ref} has terminated, shutting down system")
      context.system.terminate()
  }
}
