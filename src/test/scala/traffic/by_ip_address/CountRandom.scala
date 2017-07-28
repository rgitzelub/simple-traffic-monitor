package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import traffic._
import traffic.counting.{CounterTreeMessage, CountsTree}
import traffic.impl.ip.{HitTreeCounter, IpAddress, PageHit, SimpleThresholdListener}

import scala.concurrent.Await
import scala.concurrent.duration._




object CountRandom {

  val system = ActorSystem("ip")

  val log = Logging.getLogger(system, this)

  def main(args: Array[String]): Unit = {

    val notifier = system.actorOf(Props[Notifier], "notifier")

    val counter = system.actorOf(Props(classOf[HitTreeCounter], "Address Counter"), "counter")
    counter ! CounterTreeMessage.SetListener(new SimpleThresholdListener(notifier, 10, 4, 3, 2))

    log.info("counting...")

    // we want to submit `N` addresses over `totalTime` ms
    val N = 1000
    val totalTime = 2000 // milliseconds

    val updateProgressEvery = N / 10
    val delayMs = totalTime / N

    1.to(N)
      .foreach{ i =>
        counter ! CounterTreeMessage.UpdateCountFor(PageHit(new DateTime, IpAddress.random4))
        if(i % updateProgressEvery == 0) log.info(s"$i addresses submitted")
        Thread.sleep(delayMs)
      }


    log.info("done counting")

    implicit val timeout = Timeout(1 seconds)
    val askTimeout = Duration(1, TimeUnit.SECONDS)

    def printCurrentTree = {
      log.info("====================")
      val finalCount = Await.result(ask(counter, CounterTreeMessage.AskForCounts), askTimeout).asInstanceOf[CountsTree]
      log.info(finalCount.count.toString)
      finalCount.print(0){ x => log.info(x.toString) }
    }

    printCurrentTree

    Thread.sleep(100)
    counter ! CounterTreeMessage.ForgetOldCounts(1)
    printCurrentTree

    Thread.sleep(500)
    counter ! CounterTreeMessage.ForgetOldCounts(1)
    printCurrentTree

    Thread.sleep(800)
    counter ! CounterTreeMessage.ForgetOldCounts(1)
    printCurrentTree

    system.terminate()
  }
}
