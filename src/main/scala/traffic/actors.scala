package traffic

import akka.actor.{Actor, ActorRef, Props}


trait Counting {
  this: Actor =>

  var count = 0
}



abstract class NodeCount[T](name: String) extends Actor with Counting {
  private def newChild(name: String) = {
    context.actorOf(Props(childClass, name), name)
  }


  // TODO: protect these
  def childName(value: T): String
  def childClass: Class[_]

  // we can use anything for the name, and if we use the 'key' in the name uniquely,
  //  we don't need to explicitly keep a map of keys to actors (so long as we don't
  //  create any other children)
//  private def childName(value: T) = "leaf" + childKey(value)

  // if we don't already have a child for the key, create one
  private def childFor(value: T): ActorRef = {
    val name = childName(value)
    context.child(name).getOrElse(newChild(name))
  }


  def receive = {
    case UpdateCountFor(value: T) =>
      childFor(value) ! UpdateCountFor(value)
      count += 1

    case msg :EmitCount =>
      context.children.foreach(_ ! msg)
      msg.emitter ! CountToEmit(name, count)
  }
}

abstract class LeafCount[T](name: String) extends Actor with Counting {
  def receive = {
    case UpdateCountFor(_) =>
      count += 1

    case EmitCount(emitter) =>
      emitter ! CountToEmit(name, count)
  }
}
