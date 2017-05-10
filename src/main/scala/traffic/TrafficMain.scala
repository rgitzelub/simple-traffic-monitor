package traffic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}

import scala.util.Random



object TrafficMain {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("traffic")

    val N = 10

    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[NodeCount], "counter"), "counter")

    println("updating...")
    1.to(N).foreach { _ =>
      val v = Random.nextInt(1000).toString
      counter ! UpdateCountFor(v)
    }

    // at this point the othe cascading is still happening, we've only for sure sent the top-level message

    println("emitting...")
    counter ! EmitCount(emitter)


    // so it's likely the update messages *and* the emit messages are cascading simultaneously...
    //  that sounds like a race condition :(


    // how do we wait until the messages to emitter have cascade through all the actors?
    Thread.sleep(1000)

    emitter ! Stop
  }

  class Terminator(ref: ActorRef) extends Actor with ActorLogging {
    context watch ref

    def receive = {
      case Terminated(_) =>
        log.info(s"${ref} has terminated, shutting down system")
        context.system.terminate()
    }
  }

}
