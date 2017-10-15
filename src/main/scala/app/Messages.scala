package app

case class InitMessage (selfAddress : String, neighs: List[String])

case class Notify ()

case class NotifyGlobalView (address: String)
