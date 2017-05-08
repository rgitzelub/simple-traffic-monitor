package traffic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}

import scala.util.Random

object TrafficMain {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("traffic")

    val N = 1000000

    val actors = 0.until(N).toArray.map { i =>
      val name = "a" + i
      system.actorOf(Props(classOf[IpAddressPart1], name), name)
    }

    val emitter = system.actorOf(Props[Emitter], "emitter")

    system.actorOf(Props(classOf[Terminator], List(emitter)), "terminator")

    1.to(N/2).foreach { _ =>
      actors(Random.nextInt(N)) ! UpdateCount
    }

    actors.foreach(_ ! EmitCount(emitter))

    1.to(N/2).foreach { _ =>
      actors(Random.nextInt(N)) ! UpdateCount
    }

    println("emitting...")

    actors.foreach(_ ! EmitCount(emitter))

    // this will be at the end of the Emitter's queue, so it won't be processed until after all the messages are output
    emitter ! Stop
  }

  class Terminator(refs: List[ActorRef]) extends Actor with ActorLogging {
    refs.foreach( context watch _ )

    def receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system")
        context.system.terminate()
    }
  }

}
