package traffic

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout


import scala.concurrent.Future
import scala.concurrent.duration._


case class CountTreeNode(label: String, count: Int, children: Iterable[CountTreeNode]) {
  def print(indent: Int): Unit = {
//    println(children)
    println(s">${"  " * indent}${label}: ${count}")
    children.foreach(c => c.print(indent + 1))
  }
}

case object AskForCountTree


trait Counting {
  this: Actor =>

  var count = 0

  // a human name for what the actor is counting
  def label: String
}


trait ChildStrategy[T] {
  this: Actor =>

  // what is under us?
  def childClass: Class[_]

  // human-grokkable descriptor for this particular actor, usually based solely on the value
  //  it was created by
  def childNodeLabel(value: T): String

  // a unique name for the actor itself, ideally based on the value
  def childActorName(value: T): String


  // we can use anything for the name, and if we use the 'key' in the name uniquely,
  //  we don't need to explicitly keep a map of keys to actors (so long as we don't
  //  create any other children)

  def newChild(value: T) = {
    context.actorOf(Props(childClass, childNodeLabel(value)), childActorName(value))
  }

  // if we don't already have a child for the key, create one
  def childFor(value: T): ActorRef = {
    context.child(childActorName(value)).getOrElse(newChild(value))
  }
}



abstract class NodeCount[T](val label: String) extends Actor with Counting with ChildStrategy[T] {
  def receive = {
    case UpdateCountFor(value: T) =>
      childFor(value) ! UpdateCountFor(value)
      count += 1

    case AskForCountTree =>
      // TODO: really need these?
      implicit val timeout = Timeout(1 seconds)
      import context.dispatcher

      val s = sender // http://stackoverflow.com/a/25402857

      val fs = context.children.map{ ask(_, AskForCountTree).mapTo[CountTreeNode] }
      Future.sequence(fs).map{ counts =>
        s ! CountTreeNode(label, count, counts)
      }

    case msg :EmitCount =>
      context.children.foreach(_ ! msg)
      msg.emitter ! CountToEmit(label, count)
  }
}

abstract class LeafCount[T](val label: String) extends Actor with Counting {
  def receive = {
    case UpdateCountFor(_) =>
      count += 1

    case AskForCountTree =>
      sender ! CountTreeNode(label, count, List())

    case EmitCount(emitter) =>
      emitter ! CountToEmit(label, count)
  }
}
