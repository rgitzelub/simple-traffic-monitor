package traffic

import akka.actor.Actor


trait Counting {
  this: Actor =>

  var count = 0

  val countingBehaviour: Receive = {
    case UpdateCount =>
      count += 1
  }
}



class IpAddressPart1(name: String) extends Actor with Counting {
  def receive = countingBehaviour.orElse({
    case EmitCount(emitter) =>
      emitter ! CountMessage(name, count)
  })
}
