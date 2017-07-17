package traffic.by_ip_address

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, JodaTimePermission}
import traffic.by_ip_address.CountRandom.system
import traffic.impl.{IpAddress, IpAddressTreeCounter}
import traffic.{CounterTreeMessage, CountsTree, DeadLetterListener, Emitter, Terminator, UpdateCountFor}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}


object CountFromFile {

  val system = ActorSystem("ip")

  val log = Logging.getLogger(system, this)


  val format = DateTimeFormat.forPattern("dd/MMM/yyyy:kk:mm:ss Z")


  def main(args: Array[String]): Unit = {


    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[IpAddressTreeCounter], "counter"), "counter")

    val seance = system.actorOf(Props(classOf[DeadLetterListener]))
    system.eventStream.subscribe(seance, classOf[DeadLetter])

    log.info("reading...")

//    val file = new File("/Users/rodneygitzel/Downloads/search-results-2017-06-30T14-55-22.422-0700.csv")
    val file = new File("/Users/rodneygitzel/Downloads/elb/t2")

    val N = 40 * 100000

    var i = 0
    io.Source.fromFile(file).getLines.take(N).foreach{ line =>
      val splits = line.split(" ").toVector.map(s => s.replace("\"", ""))
      if(splits.size == 2) {
        val ts = DateTime.parse(splits(0))
        val address = IpAddress.fromString(splits(1))
        //      println(address, ts)
        counter ! UpdateCountFor(address)
      }
      i += 1
      if(i % 100000 == 0) println(i)
    }
    log.info("done reading")

    log.info("extracting counts")

    implicit val timeout = Timeout(300 seconds)

    try {
      val ct = Await.result(
        ask(counter, CounterTreeMessage.AskForCounts),
        Duration(30, TimeUnit.SECONDS)
      ).asInstanceOf[CountsTree]

      log.info("done")

      //    log.info("Final: " + ct.count.toString)
    }
    catch {
      case e: Exception =>
        log.error(e.toString)
    }
    finally {
      emitter ! Stop
      system.terminate()
    }
  }
}
