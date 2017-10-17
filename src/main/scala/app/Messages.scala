package app

case class InitMessage (selfAddress : String, contactNode: String)

case class Join (newNodeAddress: String)

case class ForwardJoin(newNode: String, arwl: Int, senderAddress: String)

case class NotifyGlobalView (address: String)

case class Notify (senderAddress : String)
