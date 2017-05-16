package traffic


import akka.actor._



case class IpAddress(a: Int, b: Int, c: Int, d: Int)



class IpAddressCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.a
  val childClass = classOf[ACount]
}

class ACount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}.${value.b}"
  def childActorName(value: IpAddress) = context.self.path.name + "-" + value.b
  val childClass = classOf[BCount]
}


class BCount(name: String) extends NodeCount[IpAddress](name) {
  def childNodeLabel(value: IpAddress) = s"subnet ${value.a}.${value.b}.${value.c}"
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
    val list = List(
      new IpAddress(1, 2, 3, 4),
      new IpAddress(1, 2, 3, 4),
      new IpAddress(1, 2, 3, 5),
      new IpAddress(1, 2, 3, 6),
      new IpAddress(1, 2, 13, 4),
      new IpAddress(1, 2, 13, 4),
      new IpAddress(11, 12, 13, 15)
    )

    list.foreach{ v =>
      //println(v)
      counter ! UpdateCountFor(v)
    }

    // at this point the other cascading is still happening, we've only for sure sent the top-level message

    println("emitting...")
    counter ! EmitCount(emitter)


    // so it's likely the update messages *and* the emit messages are cascading simultaneously...
    //  that sounds like a race condition :(


    // how do we wait until the messages to emitter have cascade through all the actors?
    Thread.sleep(1000)

    emitter ! Stop
  }
}
