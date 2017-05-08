package traffic

import akka.actor.Actor


case class CountMessage(name: String, value: Int)

class Emitter extends Actor {
  def receive = {
    case CountMessage(name, value) =>
     // println(s"${name}: ${value}")

    case Stop =>
      context.stop(self)
  }
}
