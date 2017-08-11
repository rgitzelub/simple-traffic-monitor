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

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}


object CountPageEventsFromFile {

  val system = ActorSystem("page_events_counter")

  val log = Logging.getLogger(system, this)


  val format = DateTimeFormat.forPattern("dd/MMM/yyyy:kk:mm:ss Z")


  def eventFromLogLine(line: String) = {
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
      Some(event)
    }
    else {
      None
    }
  }

  def main(args: Array[String]): Unit = {

    val counter = system.actorOf(Props(classOf[ConvertableEventTreeCounter], "counter"), "counter")

    val seance = system.actorOf(Props(classOf[DeadLetterListener]))
    system.eventStream.subscribe(seance, classOf[DeadLetter])

    val file = new File("src/test/resources/event-receiver.txt")

    val lines = io.Source.fromFile(file).getLines

    val sendForgetMessageIntervalMillis = Duration(5, TimeUnit.SECONDS).toMillis
    val forgetMessageValueSeconds = Duration(1, TimeUnit.MINUTES).toSeconds.toInt

    log.info("reading...")
    val linesToProcess = lines.take(100000).toList
    log.info(linesToProcess.head)
    log.info(linesToProcess.last)

    var i = 0
    val validEvents = linesToProcess.flatMap{ line =>
      i += 1
      if(i % 100000 == 0) println(i)
      eventFromLogLine(line) match {
        case Some(event) =>
          Some(event)
        case _ =>
          log.warning("ignoring line " + line)
          None
      }
    }

    log.info(s"read ${i} lines as ${validEvents.size} events")

    // need to break the single list into many lists of lists where the
    //  inner lists are all in the same time frame

    // one way to do it, with mutable values...
    var lastCleanMillis = validEvents.head.timestamp.getMillis
    val currentList = mutable.MutableList[ConvertableEvent]()
    val list =
      validEvents.foldLeft(List[List[ConvertableEvent]]()){ case(acc, event) =>
        if(event.timestamp.getMillis - lastCleanMillis > sendForgetMessageIntervalMillis) {
          lastCleanMillis += sendForgetMessageIntervalMillis
          log.info("start new list at " + event.timestamp.getMillis)
          val newAcc = currentList.toList +: acc
          currentList.clear
          currentList += event
          newAcc
        }
        else
        {
          currentList += event
          acc
        }
      }
    val eventsGroupedByTime1 = (currentList.toList +: list).reverse

    log.info(s"made ${eventsGroupedByTime1.size} groups")
    eventsGroupedByTime1.foreach{ group =>
      log.info(s"  ${group.size} from ${group.head.timestamp.getMillis} to ${group.last.timestamp.getMillis}")
    }


    // recursion and 'partition' are a better choice, but unfortunately wouldn't take into account
    //  that the list is already sorted, so it will take considerably longer

    // 'takeWhile' + 'drop' would be more efficient than 'partition'...

    def splitWithinNextInterval(events: List[ConvertableEvent], since: Long, interval: Long) = {
      val t = events.takeWhile{ event =>
        event.timestamp.getMillis - since < interval
      }
      (t, events.drop(t.size))
    }

    def groupByTime(events: List[ConvertableEvent], startingAt: Long, interval: Long) = {
      def r(groups: List[List[ConvertableEvent]], remainingEvents: List[ConvertableEvent], since: Long): List[List[ConvertableEvent]] = {
        val (first, rest) = splitWithinNextInterval(remainingEvents, since, interval)
        if(first.size == 0 && rest.size > 0) {
          // none in that interval, don't bother keeping an empty list
          //  (or should we leave them in, and leave it up to the caller to filter out the empty ones?
          r(groups, rest, since + interval)
        }
        else {
          if(rest.size == 0) {
            groups :+ first
          }
          else {
            r(groups :+ first, rest, since + interval)
          }
        }
      }
      r(List(), events, startingAt)
    }

    val eventsGroupedByTime2 = groupByTime(validEvents, validEvents.head.timestamp.getMillis, sendForgetMessageIntervalMillis)

    log.info(s"made ${eventsGroupedByTime2.size} groups")
    eventsGroupedByTime2.foreach{ group =>
      log.info(s"  ${group.size} from ${group.head.timestamp.getMillis} to ${group.last.timestamp.getMillis}")
    }

    var n = 0
    eventsGroupedByTime2.foreach{ events =>
//      println(msg)
      events.foreach { event =>
        counter ! CounterTreeMessage.UpdateCountFor(event)
        n += 1
      }
      counter ! CounterTreeMessage.ForgetOldCounts(forgetMessageValueSeconds)
      n += 1
    }

    log.info(s"sent $n messages")

//    Thread.sleep(1000 * 5)

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

//      ct.print(0){ x => println(x.toString) }
//
//      ct.children.toList.sortBy(_.count.total).foreach(x => println(x.count))

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
