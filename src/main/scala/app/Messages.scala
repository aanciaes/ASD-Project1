package app

case class InitMessage(selfAddress: String, contactNode: String)

case class Join(newNodeAddress: String)

case class ForwardJoin(newNode: String, arwl: Int, senderAddress: String)

case class NotifyGlobalView(address: String)

case class Notify(senderAddress: String)

case class Disconnect(nodeToDisconnect: String)

//Application Messages

case class ShowGB(address: String)

case class ShowPV(message: String)

case class ReplyAppRequest(replyType: String, myself: String,  nodes: List[String])