package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import traffic._
import traffic.impl.{IpAddress, IpAddressCountTree, SimpleThresholdListener}

import scala.concurrent.Await
import scala.concurrent.duration._




object CountRandom {

  val system = ActorSystem("ip")

  val log = Logging.getLogger(system, this)

  def main(args: Array[String]): Unit = {

    val notifier = system.actorOf(Props[Notifier], "notifier")

    val counter = system.actorOf(Props(classOf[IpAddressCountTree], "Address Counter"), "counter")
    counter ! SetListener(new SimpleThresholdListener(notifier, 10, 4, 3, 2))

    log.info("updating...")

    val N = 100
    val printAt = N / 10
    val delayMs = 100

    1.to(N)
      .foreach{ i =>
        counter ! UpdateCountFor(IpAddress.random4)
        if(i % printAt == 0) log.info(s"$i sent")
        Thread.sleep(delayMs)
      }


    log.info("building tree")

    implicit val timeout = Timeout(3 seconds)

    val r = Await.result(
      ask(counter, AskForCountsTree),
      Duration(6, TimeUnit.SECONDS)
    )

    r.asInstanceOf[CountsTree].print(0)

    log.info("done printing")

//    system.stop(counter)
//    system.stop(emitter)

    system.terminate()
  }
}
