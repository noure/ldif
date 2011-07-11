package ldif.entity

trait Entity
{
  def uri : String

  def graph: String

  def entityDescription : EntityDescription

  def factums(patternId : Int) : Traversable[IndexedSeq[Node]]
}



