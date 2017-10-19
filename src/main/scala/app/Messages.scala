package app

//pView
case class InitMessage (selfAddress : String, contactNode: String)

case class Join(newNodeAddress: String)

case class ForwardJoin(newNode: String, arwl: Int, senderAddress: String)

case class Notify (senderAddress : String)

case class Disconnect (nodeToDisconnect: String)


//gView
case class InitGlobView (selfAddress : String)

case class NotifyGlobalView (address: String)


//Application Messages

case class ShowGV(address: String)

case class ShowPV(message: String)

case class ReplyAppRequest(replyType: String, myself: String,  nodes: List[String])

// Information Dissemination

case class InitGossip(selfAddress : String)