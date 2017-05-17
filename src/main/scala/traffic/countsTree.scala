package traffic


case class CountsTree(label: String, count: Int, children: Iterable[CountsTree]) {
  def print(indent: Int): Unit = {
    println(s">${"  " * indent}${label}: ${count}")
    children.foreach{ _.print(indent + 1) }
  }
}

case object AskForCountsTree

