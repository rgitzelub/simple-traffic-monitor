package traffic

import akka.actor.ActorRef


case class UpdateCountFor[T](value: T)

// drop the counts for anything more than `seconds` old
case class ForgetOldCounts(seconds: Int)

case class EmitCount(emitter: ActorRef)

case class SetListener(cl: CountListener)

case object Stop
