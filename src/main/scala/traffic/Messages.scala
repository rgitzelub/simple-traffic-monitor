package traffic

import akka.actor.ActorRef


case class UpdateCountFor(value: String)


case class EmitCount(emitter: ActorRef)


case object Stop
