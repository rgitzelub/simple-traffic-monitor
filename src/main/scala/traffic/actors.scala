package traffic

import akka.actor.{Actor, ActorRef, Props}


trait Counting {
  this: Actor =>

  var count = 0

  def label: String
}



abstract class NodeCount[T](val label: String) extends Actor with Counting {
  private def newChild(value: T) = {
    context.actorOf(Props(childClass, childNodeLabel(value)), childActorName(value))
  }


  // TODO: protect these
  def childNodeLabel(value: T): String
  def childActorName(value: T): String
  def childClass: Class[_]

  // we can use anything for the name, and if we use the 'key' in the name uniquely,
  //  we don't need to explicitly keep a map of keys to actors (so long as we don't
  //  create any other children)
//  private def childName(value: T) = "leaf" + childKey(value)

  // if we don't already have a child for the key, create one
  private def childFor(value: T): ActorRef = {
    context.child(childActorName(value)).getOrElse(newChild(value))
  }


  def receive = {
    case UpdateCountFor(value: T) =>
      childFor(value) ! UpdateCountFor(value)
      count += 1

    case msg :EmitCount =>
      context.children.foreach(_ ! msg)
      msg.emitter ! CountToEmit(label, count)
  }
}

abstract class LeafCount[T](val label: String) extends Actor with Counting {
  def receive = {
    case UpdateCountFor(_) =>
      count += 1

    case EmitCount(emitter) =>
      emitter ! CountToEmit(label, count)
  }
}
