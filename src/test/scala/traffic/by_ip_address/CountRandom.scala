package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import traffic._
import traffic.impl.{IpAddress, IpAddressTreeCounter, SimpleThresholdListener}

import scala.concurrent.Await
import scala.concurrent.duration._




object CountRandom {

  val system = ActorSystem("ip")

  val log = Logging.getLogger(system, this)

  def main(args: Array[String]): Unit = {

    val notifier = system.actorOf(Props[Notifier], "notifier")

    val counter = system.actorOf(Props(classOf[IpAddressTreeCounter], "Address Counter"), "counter")
    counter ! SetListener(new SimpleThresholdListener(notifier, 10, 4, 3, 2))

    log.info("counting...")

    val N = 1000
    val totalTime = 2000 // milliseconds

    val printAt = N / 10

    val delayMs = totalTime / N

    1.to(N)
      .foreach{ i =>
        counter ! UpdateCountFor(IpAddress.random4)
        if(i % printAt == 0) log.info(s"$i sent")
        Thread.sleep(delayMs)
      }


    log.info("done counting")

    implicit val timeout = Timeout(3 seconds)

    def printCurrentTree = {
      log.info("====================")
      val t = Await.result(
        ask(counter, AskForCountsTree),
        Duration(6, TimeUnit.SECONDS)
      ).asInstanceOf[CountsTree]

      log.info(t.count.toString)
      t.print(0){ x => log.info(x.toString) }
    }
    printCurrentTree

    Thread.sleep(100)
    counter ! ForgetOldCounts(1)

    printCurrentTree

    Thread.sleep(500)
    counter ! ForgetOldCounts(1)

    printCurrentTree

    system.terminate()
  }
}
