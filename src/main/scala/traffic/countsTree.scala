package traffic


case class CountsTree(label: String, count: Long, children: Iterable[CountsTree]) {
  def print(indent: Int): Unit = {
    println(s">${"  " * indent}${label}: ${count}")
    children.foreach{ _.print(indent + 1) }
  }

  def toHtml(indent: Int): String = {
    s"${"&nbsp;&nbsp;" * indent}${label}: ${count}</br>\n" + children.map(_.toHtml(indent+1)).mkString("</br>\n")
  }
}

case object AskForCountsTree

