package traffic.counting

import akka.actor.Actor



trait CountingStrategy {
  def currentCount: Long
  def incrementCount(value: Countable): Unit
  def forgetOlderThan(seconds: Int)
}

trait PlainOldCountingStrategy extends CountingStrategy {
  var currentCount = 0
  def incrementCount(value: Countable) = currentCount += 1
  def forgetOlderThan(seconds: Int) =
    currentCount = 0
}

trait SimpleListCountingStrategy extends CountingStrategy {
  private var timestamps: List[Long] = List()
  private var cachedSize = 0L

  private def now = System.currentTimeMillis

  def currentCount: Long = cachedSize

  def incrementCount(value: Countable) = {
    timestamps = (value.timestamp.getMillis +: timestamps)
    cachedSize = timestamps.size
  }

  def forgetOlderThan(seconds: Int) = {
    val threshold = now - (seconds * 1000)
    timestamps = timestamps.takeWhile(_ > threshold)
    cachedSize = timestamps.size
  }
}

trait CountListener {
  def notify(counter: Counter[_], count: Long)
}

trait Counter[T <: Countable] {
  this: Actor with CountingStrategy =>

  // a human name for what the actor is counting
  def label: String

  // TODO: it doesn't seem right to expose this, but you can't pass your
  //  own listener to a child without it being exposed
  var countListener: Option[CountListener] = None

  // this *could* be a constructor parameter, then it couldn't
  //  be changed on the fly
  def setListener(cl: Option[CountListener]) = {
    countListener = cl
  }

  def count(value: Countable) = {
    incrementCount(value)
    countListener.foreach{ cl =>
      //      println(label, count)
      cl.notify(this, currentCount)
    }
  }
}

