package traffic

import akka.actor.ActorRef


case object UpdateCount


case class EmitCount(emitter: ActorRef)


case object Stop
