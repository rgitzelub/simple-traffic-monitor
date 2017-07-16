package traffic


import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._


object CounterTreeMessage {
  case object AskForCounts

  val count = new AtomicInteger(0)
}


/*
 * this allows you to construct a counter of timestamped hierarchical data
 */

/*
 * this is where the actual counting happens
 */
abstract class CounterTreeLeaf[T](val label: String) extends Actor with Counter with SimpleListCountingStrategy {

  val log = Logging.getLogger(context.system, this)

  def receive = {
    case SetListener(cl) =>
      setListener(Some(cl))

    case UpdateCountFor(_) =>
      increment

    case CounterTreeMessage.AskForCounts =>
      log.info(s"asked: ${CounterTreeMessage.count.incrementAndGet} $label - $count")
      sender ! CountsTree(label, Count(count, 1, if(count == 0) 1 else 0), List())

    case EmitCount(emitter) =>
      emitter ! CountToEmit(label, count)

    case msg: ForgetOldCounts =>
      forgetOlderThan(msg.seconds)
  }
}


/*
 * no counting happens here, we instead manage a portion of the tree
 */
abstract class CounterTreeNode[T](val label: String) extends Actor with CounterTreeNodeChildFactory[T] {

  val log = Logging.getLogger(context.system, this)

  // need these to be able to use Future in `receive`... which seems a bit klunky
  // TODO: what set the timeout to?
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher

  def receive = {
    case update: UpdateCountFor[T] =>
      childFor(update.value) ! update

    case CounterTreeMessage.AskForCounts =>
      val childFutures = context.children.map{
        ask(_, CounterTreeMessage.AskForCounts).mapTo[CountsTree]
      }
      val copyOfSenderToUseFromTheFuture = sender // http://stackoverflow.com/a/25402857
      Future.sequence(childFutures).map{ childTrees =>
        val combinedCount = Count.sum(childTrees.map(_.count))
        copyOfSenderToUseFromTheFuture ! CountsTree(label, combinedCount, childTrees)
      }

    // the rest of these are just passed along
    case emit: EmitCount =>
      context.children.foreach(_ ! emit)

    case set: SetListener =>
      context.children.foreach(_ ! set)

    case forget: ForgetOldCounts =>
      context.children.foreach(_ ! forget)

    case Terminated(ref) =>
      log.error(s"uh oh, ${ref} died")
  }

// TODO: need to rethink this part
//  override def childCreatedHook(child: ActorRef) = {
//      countListener.map{ cl => child ! SetListener(cl) }
//  }
}


/*
 * breaking this out as a trait makes CounterTreeNode less cluttered
 */
protected trait CounterTreeNodeChildFactory[T] {
  this: CounterTreeNode[T] =>

  // what is under us?
  def childClass: Class[_]

  // human-grokkable descriptor for this particular actor, usually based solely on the value
  //  it was created by
  def childNodeLabel(value: T): String

  // a unique name for the actor itself, ideally based on the value
  def childActorName(value: T): String

  def childCreatedHook(child: ActorRef) = {}

  // -------------

  // we can use anything for the name, and if we use the 'key' in the name uniquely,
  //  we don't need to explicitly keep a map of keys to actors (so long as we don't
  //  create any other children)

  def newChild(value: T) = {
    val child = context.actorOf(Props(childClass, childNodeLabel(value)), childActorName(value))
    context.watch(child)
    childCreatedHook(child)
    child
  }

  // if we don't already have a child for the key, create one
  def childFor(value: T): ActorRef = {
    context.child(childActorName(value)).getOrElse(newChild(value))
  }
}
