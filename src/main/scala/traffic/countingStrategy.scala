package traffic

import akka.actor.Actor



trait CountingStrategy {
  def count: Long
  def incrementCount: Unit
  def forgetOlderThan(seconds: Int)
}

trait PlainOldCountingStrategy extends CountingStrategy {
  var count = 0
  def incrementCount = count += 1
  def forgetOlderThan(seconds: Int) =
    count = 0
}

trait SimpleListCountingStrategy extends CountingStrategy {
  private var timestamps: List[Long] = List()
  private var cachedSize = 0L

  private def now = System.currentTimeMillis

  def count: Long = cachedSize

  def incrementCount = {
    timestamps = (now +: timestamps)
    cachedSize = timestamps.size
  }

  def forgetOlderThan(seconds: Int) = {
    val threshold = now - (seconds * 1000)
    timestamps = timestamps.takeWhile(_ > threshold)
    cachedSize = timestamps.size
  }
}

trait CountListener {
  def notify(counter: Counter, count: Long)
}

trait Counter {
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

  def increment = {
    incrementCount
    countListener.foreach{ cl =>
      //      println(label, count)
      cl.notify(this, count)
    }
  }
}

