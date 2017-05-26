package traffic.by_ip_address

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import traffic.{AskForCountsTree, CountsTree, Emitter, Terminator, UpdateCountFor}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}


object CountFromFile {

  val system = ActorSystem("ip")

  def main(args: Array[String]): Unit = {

    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[IpAddressCount], "counter"), "counter")

    println("updating...")

    io.Source.fromFile(new File("src/test/resources/big.csv")).getLines.drop(1).foreach{ line =>
      val splits = line.split(",").toVector.map(s => s.replace("\"", ""))
      val address = IpAddress.fromString(splits.last)
      //println(address)
      counter ! UpdateCountFor(address)
    }


    println("building tree")

    implicit val timeout = Timeout(3 seconds)

    val r = Await.result(
      ask(counter, AskForCountsTree),
      Duration(6, TimeUnit.SECONDS)
    )

    r.asInstanceOf[CountsTree].print(0)

    emitter ! Stop
  }
}
