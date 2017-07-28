package traffic

import akka.actor.{ActorRef, ActorSystem, Props}
import org.joda.time.DateTime
import traffic.counting.{Countable, CounterTreeLeaf, CounterTreeMessage, CounterTreeNode}

import scala.util.Random


case class CountableString(s: String) extends Countable {
  val timestamp = new DateTime
}

class ThirdLastCharCounterTreeNode(name: String) extends CounterTreeNode[CountableString](name) {
  def childNodeLabel(value: CountableString) = "" +  value.s.charAt( value.s.length-3)
  def childActorName(value: CountableString) = name + "-" + "node" +  value.s.charAt( value.s.length-3)
  def childClass = classOf[SecondLastCharCounterTreeNode]
}


class SecondLastCharCounterTreeNode(name: String) extends CounterTreeNode[CountableString](name) {
  def childNodeLabel(value: CountableString) = "" +  value.s.charAt( value.s.length-2)
  def childActorName(value: CountableString) = name + "-" + "node" +  value.s.charAt( value.s.length-2)
  def childClass = classOf[LastCharCounterTreeNode]
}


class LastCharCounterTreeNode(name: String) extends CounterTreeNode[CountableString](name) {
  def childNodeLabel(value: CountableString) = "" +  value.s.charAt( value.s.length-1)
  def childActorName(value: CountableString) = name + "-" + "leaf" +  value.s.last.toString
  def childClass = classOf[MyCounterTreeLeaf]
}

class MyCounterTreeLeaf(name: String) extends CounterTreeLeaf[CountableString](name) {
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
      counter ! CounterTreeMessage.UpdateCountFor(CountableString(v.toString))
    }

    // at this point the othe cascading is still happening, we've only for sure sent the top-level message

    println("emitting...")
    counter ! CounterTreeMessage.EmitCount(emitter)


    // so it's likely the update messages *and* the emit messages are cascading simultaneously...
    //  that sounds like a race condition :(


    // how do we wait until the messages to emitter have cascade through all the actors?
    Thread.sleep(1000)

    emitter ! Emitter.Stop
  }
}
