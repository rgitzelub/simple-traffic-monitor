package traffic.by_ip_address

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import traffic.{AskForCountsTree, CountsTree, Emitter, LeafCount, NodeCount, Terminator, UpdateCountFor}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}




object CountRandom {

  val system = ActorSystem("ip")

  def main(args: Array[String]): Unit = {

    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[IpAddressCount], "counter"), "counter")

    println("updating...")

//    val list = List(
//      new IpAddress(1, 2, 3, 4),
//      new IpAddress(1, 2, 3, 4),
//      new IpAddress(1, 2, 3, 5),
//      new IpAddress(1, 2, 3, 6),
//      new IpAddress(1, 2, 13, 4),
//      new IpAddress(1, 2, 13, 4),
//      new IpAddress(11, 12, 13, 15)
//    )

    val list = 1.to(1000000).map(_ => IpAddress.random)

    list.foreach{ v =>
      //println(v)
      counter ! UpdateCountFor(v)
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
