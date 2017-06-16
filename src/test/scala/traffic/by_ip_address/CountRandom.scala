package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import traffic.{AskForCountsTree, CountsTree, Emitter, UpdateCountFor}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}




object CountRandom {

  val system = ActorSystem("ip")

  def main(args: Array[String]): Unit = {

    val log = Logging.getLogger(system, this)

    val emitter = system.actorOf(Props[Emitter], "emitter")
   // system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[IpAddressCount], "Address Counter"), "counter")

    log.info("updating...")

    val N = 3

    1.to(N)
      .map(_ => IpAddress.random)
      .foreach{ v =>
        counter ! UpdateCountFor(v)
      }


    log.info("building tree")

    implicit val timeout = Timeout(3 seconds)

    val r = Await.result(
      ask(counter, AskForCountsTree),
      Duration(6, TimeUnit.SECONDS)
    )

    r.asInstanceOf[CountsTree].print(0)

    log.info("done printing")

    system.stop(counter)
    system.stop(emitter)

//    emitter ! Stop
//    println("stop sent")

    system.terminate()

//    Thread.sleep(1000)
  }
}
