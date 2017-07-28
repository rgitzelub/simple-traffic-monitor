package traffic

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import traffic.by_ip_address.CountRandom.log
import traffic.counting.{CounterTreeMessage, CountsTree}
import traffic.impl.ip.{HitTreeCounter, IpAddress, PageHit}
import traffic.impl.page.{ConvertableEvent, ConvertableEventTreeCounter}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}


object CountPageEventsFromFile {

  val system = ActorSystem("page_events_counter")

  val log = Logging.getLogger(system, this)


  val format = DateTimeFormat.forPattern("dd/MMM/yyyy:kk:mm:ss Z")


  def main(args: Array[String]): Unit = {

    val counter = system.actorOf(Props(classOf[ConvertableEventTreeCounter], "counter"), "counter")

    val seance = system.actorOf(Props(classOf[DeadLetterListener]))
    system.eventStream.subscribe(seance, classOf[DeadLetter])

    val file = new File("src/test/resources/event-receiver.txt")

    val lines = io.Source.fromFile(file).getLines

    log.info("reading...")
    var i = 0
    lines.foreach{ line =>
      val splits = line.split("\\\"").toVector
      if(splits.size == 28) {
        val values = splits.map(_.replaceAll("\\\\", "").replace(",", "").replace(":", ""))
        val event = ConvertableEvent(
            new DateTime(values(11).toLong),
            values(16).substring(0, 8),
            values(16),
            values(20),
            values(26)
        )
//        println(event)
        counter ! CounterTreeMessage.UpdateCountFor(event)
      }
      i += 1
      if(i % 100000 == 0) println(i)
    }
    log.info(s"done. read ${i} events")

    log.info("extracting counts...")

    implicit val timeout = Timeout(30 minutes)

    try {
      val ct = Await.result(
        ask(counter, CounterTreeMessage.AskForCounts),
        Duration(30, TimeUnit.MINUTES)
      ).asInstanceOf[CountsTree]

      log.info("done")

      log.info("Final: " + ct.count.toString)
//      log.info(ct.toString)

      ct.print(0){ x => println(x.toString) }

      ct.children.toList.sortBy(_.count.total).foreach(x => println(x.count))

      // wait for logger to catch up
      Thread.sleep(1000 * 5)
    }
    catch {
      case e: Exception =>
        log.error(e.toString)
    }
    finally {
      system.terminate()
    }
  }
}
