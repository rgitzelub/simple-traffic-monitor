package traffic


import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

import scala.concurrent.duration._



case class IpAddress(a: Int, b: Int, c: Int, d: Int)

object IpAddress {
  def random = IpAddress(
    Random.nextInt(16) + 1,
    Random.nextInt(16) + 1,
    Random.nextInt(32) + 1,
    Random.nextInt(4) + 1
  )
}


class IpAddressCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}.x.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACount]
}

class ACount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}.${value.b}.x.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCount]
}


class BCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}.${value.b}.${value.c}.x"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.c
  val childClass = classOf[CCount]
}

class CCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"host ${value.a}.${value.b}.${value.c}.${value.d}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.d
  val childClass = classOf[DCount]
}

class DCount(name: String) extends LeafCount[IpAddress](name) {
}


// ----------------------------------------------


object IpAddessMain {

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

    Thread.sleep(200)

    // at this point the other cascading is still happening, we've only for sure sent the top-level message

//    println("emitting...")
//    counter ! EmitCount(emitter)


    // so it's likely the update messages *and* the emit messages are cascading simultaneously...
    //  that sounds like a race condition :(


    // how do we wait until the messages to emitter have cascade through all the actors?
//    Thread.sleep(2000)

    println("building tree")

    implicit val timeout = Timeout(3 seconds)

    val r = Await.result(
      ask(counter, AskForCountTree),
      Duration(6, TimeUnit.SECONDS)
    )

    r.asInstanceOf[CountTreeNode].print(0)

    emitter ! Stop
  }
}
