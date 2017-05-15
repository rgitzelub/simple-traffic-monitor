package traffic

import akka.actor.ActorRef


case class UpdateCountFor[T](value: T)


case class EmitCount(emitter: ActorRef)


case object Stop
