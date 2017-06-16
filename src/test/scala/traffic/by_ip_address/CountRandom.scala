package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import traffic._

import scala.concurrent.Await
import scala.concurrent.duration._




object CountRandom {

  val system = ActorSystem("ip")

  def main(args: Array[String]): Unit = {

    val log = Logging.getLogger(system, this)

    val emitter = system.actorOf(Props[Emitter], "emitter")
   // system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val notifier = system.actorOf(Props[Notifier], "notifier")

    val counter = system.actorOf(Props(classOf[IpAddressCountTree], "Address Counter", notifier), "counter")

    log.info("updating...")

    val N = 20

    1.to(N)
      .foreach{ _ =>
        counter ! UpdateCountFor(IpAddress.randomSimplistic)
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
