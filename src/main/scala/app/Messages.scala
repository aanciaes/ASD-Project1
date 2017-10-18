package app

case class InitMessage (selfAddress : String, contactNode: String) //pView

case class Join (newNodeAddress: String)

case class ForwardJoin(newNode: String, arwl: Int, senderAddress: String)

case class Notify (senderAddress : String)



case class InitGlobView (selfAddress : String) //gView

case class NotifyGlobalView (address: String)


