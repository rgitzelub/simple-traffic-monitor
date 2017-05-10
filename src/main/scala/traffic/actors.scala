package traffic

import akka.actor.{Actor, ActorRef, Props}


trait Counting {
  this: Actor =>

  var count = 0
}



class NodeCount(name: String) extends Actor with Counting {
  private def newChild(name: String) = {
    context.actorOf(Props(classOf[LeafCount], name), name)
  }

  // we can use anything for the name, and if we use the 'key' in the name uniquely,
  //  we don't need to explicitly keep a map of keys to actors (so long as we don't
  //  create any other children)
  private def childName(value: String) = "leaf" + value.substring(value.length-2)

  // if we don't already have a child for the key, create one
  private def childFor(value: String): ActorRef = {
    val name = childName(value)
    context.child(name).getOrElse(newChild(name))
  }


  def receive = {
    case UpdateCountFor(value) =>
      childFor(value) ! UpdateCountFor(value)
      count += 1

    case msg :EmitCount =>
      context.children.foreach(_ ! msg)
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
