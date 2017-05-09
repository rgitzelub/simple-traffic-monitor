package traffic

import akka.actor.{Actor, ActorRef, Props}

import scala.collection.mutable


trait Counting {
  this: Actor =>

  var count = 0

  val countingBehaviour: Receive = {
    case UpdateCountFor(_) =>
      count += 1
  }
}



class NodeCount(name: String) extends Actor with Counting {
  private val children = mutable.HashMap[Int,ActorRef]()

  private def newChild(c: Char) = {
    val leafName = s"${name}-${c}"
    context.system.actorOf(Props(classOf[LeafCount], leafName), leafName)
  }

  private def childFor(value: String) = children.getOrElseUpdate(value.last, newChild(value.last))


  def receive = {
    case UpdateCountFor(value) =>
      childFor(value) ! UpdateCountFor(value)
      count += 1

    case msg :EmitCount =>
      children.values.foreach(_ ! msg)
      msg.emitter ! CountToEmit(name, count)
  }
}

class LeafCount(name: String) extends Actor with Counting {
  def receive = {
    case UpdateCountFor(_) =>
      count += 1

    case EmitCount(emitter) =>
      emitter ! CountToEmit(name, count)
  }
}
