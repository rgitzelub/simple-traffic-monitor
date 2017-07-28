package traffic.impl.ip

import org.joda.time.DateTime
import traffic.counting.Countable


case class PageHit(timestamp: DateTime, address: IpAddress) extends Countable

