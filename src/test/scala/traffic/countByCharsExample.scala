package traffic

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.util.Random


class ThirdLastCharCounterTreeNode(name: String) extends CounterTreeNode[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-3)
  def childActorName(value: String) = name + "-" + "node" + value.charAt(value.length-3)
  def childClass = classOf[SecondLastCharCounterTreeNode]
}


class SecondLastCharCounterTreeNode(name: String) extends CounterTreeNode[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-2)
  def childActorName(value: String) = name + "-" + "node" + value.charAt(value.length-2)
  def childClass = classOf[LastCharCounterTreeNode]
}


class LastCharCounterTreeNode(name: String) extends CounterTreeNode[String](name) {
  def childNodeLabel(value: String) = "" + value.charAt(value.length-1)
  def childActorName(value: String) = name + "-" + "leaf" + value.last.toString
  def childClass = classOf[MyCounterTreeLeaf]
}

class MyCounterTreeLeaf(name: String) extends CounterTreeLeaf[String](name) {
}


// ----------------------------------------------


object TrafficMain {

  val system = ActorSystem("traffic")

  def main(args: Array[String]): Unit = {

    val emitter = system.actorOf(Props[Emitter], "emitter")
    system.actorOf(Props(classOf[Terminator], emitter), "terminator")

    val counter = system.actorOf(Props(classOf[ThirdLastCharCounterTreeNode], "counter"), "counter")

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
