package traffic.impl.page

import traffic.counting.{CounterTreeLeaf, CounterTreeNode}


class ConvertableEventTreeCounter(name: String) extends CounterTreeNode[ConvertableEvent](name) {
  def childNodeLabel(value: ConvertableEvent) = s"account ${value.accountId}"
  def childActorName(value: ConvertableEvent) = context.self.path.name + "--" + value.accountId
  val childClass = classOf[AccountNode]
}

class AccountNode(id: String) extends CounterTreeNode[ConvertableEvent](id) {
  def childNodeLabel(value: ConvertableEvent) = s"client ${value.clientId}"
  def childActorName(value: ConvertableEvent) = context.self.path.name + "--" + value.clientId
  val childClass = classOf[ClientNode]
}

class ClientNode(id: String) extends CounterTreeNode[ConvertableEvent](id) {
  def childNodeLabel(value: ConvertableEvent) = s"activation rule ${value.activationRuleId}"
  def childActorName(value: ConvertableEvent) = context.self.path.name + "--" + value.activationRuleId
  val childClass = classOf[ActivationRuleNode]
}

class ActivationRuleNode(id: String) extends CounterTreeNode[ConvertableEvent](id) {
  def childNodeLabel(value: ConvertableEvent) = s"${value.eventType}"
  def childActorName(value: ConvertableEvent) = context.self.path.name + "--" + value.eventType
  val childClass = classOf[EventTypeTreeLeaf]
}

class EventTypeTreeLeaf(name: String) extends CounterTreeLeaf[ConvertableEvent](name) {
}


