package traffic

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.util.Random


class ThirdLastCharNodeCount(name: String) extends NodeCount[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-3)
  def childActorName(value: String) = name + "-" + "node" + value.charAt(value.length-3)
  def childClass = classOf[SecondLastCharNodeCount]
}


class SecondLastCharNodeCount(name: String) extends NodeCount[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-2)
  def childActorName(value: String) = name + "-" + "node" + value.charAt(value.length-2)
  def childClass = classOf[LastCharNodeCount]
}


class LastCharNodeCount(name: String) extends NodeCount[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-1)
  def childActorName(value: String) = name + "-" + "leaf" + value.last.toString
  def childClass = classOf[MyLeafCount]
}

class MyLeafCount(name: String) extends LeafCount[String](name) {
}


// ----------------------------------------------


object TrafficMain {

  val system = ActorSystem("traffic")

  def main(args: Array[String]): Unit = {

    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[ThirdLastCharNodeCount], "counter"), "counter")

    println("updating...")
    val list = 1.to(1000).map { _ => Random.nextInt(1000) + 1000 }
//    val list = List("10", "12", "32", "30", "12", "13")
    list.foreach{ v =>
      println(v)
      counter ! UpdateCountFor(v.toString)
    }

    // at this point the othe cascading is still happening, we've only for sure sent the top-level message

    println("emitting...")
    counter ! EmitCount(emitter)


    // so it's likely the update messages *and* the emit messages are cascading simultaneously...
    //  that sounds like a race condition :(


    // how do we wait until the messages to emitter have cascade through all the actors?
    Thread.sleep(1000)

    emitter ! Stop
  }
}
