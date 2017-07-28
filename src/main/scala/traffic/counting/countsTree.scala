package traffic.counting

/*
 * a tree of counts extracted from a tree of counters
 */


case class CountsTree(label: String, count: Count, children: Iterable[CountsTree]) {
  def print(indent: Int)(printer: Any => Any): Unit = {
    if(count.total > 0) {
      //    println(s">${"  " * indent}${label}: ${count.total} (${count.actors - count.obsolete} unique actors, ${count.obsolete} zero)")
      printer(s">${"  " * indent}${label}: ${count}")
    }
    children.foreach{ _.print(indent + 1)(printer) }
  }
}


case class Count(total: Long, actors: Long, obsolete: Long) { // 'obsolete' isn't the right word... expired? aquiesced?
  val active = actors - obsolete

  def +(that: Count) =
    Count(
      total + that.total,
      actors + that.actors,
      obsolete + that.obsolete
    )

  override def toString = s"${total} ${active} ${obsolete}"
}
object Count {
  def sum(counts: Iterable[Count]) = counts.foldLeft(Count(0,0,0))(_+_)
}

