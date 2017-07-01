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
import traffic.{CounterTree, CountsTree, Emitter, Terminator, UpdateCountFor}

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

    log.info("reading...")

//    val file = new File("/Users/rodneygitzel/Downloads/search-results-2017-06-30T14-55-22.422-0700.csv")
    val file = new File("/Users/rodneygitzel/Downloads/elb/t2")

    var i = 0
    io.Source.fromFile(file).getLines.take(20 * 100000).foreach{ line =>
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


    log.info("extracting counts")

    implicit val timeout = Timeout(300 seconds)

    val ct = Await.result(
      ask(counter, CounterTree.AskForCounts),
      Duration(300, TimeUnit.SECONDS)
    ).asInstanceOf[CountsTree]

    log.info(ct.count.toString)

    emitter ! Stop

    system.terminate()
  }
}
