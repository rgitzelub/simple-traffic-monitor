package traffic.impl.page

import org.joda.time.DateTime
import traffic.counting.Countable


case class ConvertableEvent(timestamp: DateTime, accountId: String, clientId: String, activationRuleId: String, eventType: String) extends Countable



