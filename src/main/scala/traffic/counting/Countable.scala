package traffic.counting

import org.joda.time.DateTime

/*
 * the basics for anything we want to count
 */
trait Countable {
  val timestamp: DateTime
}
