package app

//pView
case class InitMessage (selfAddress : String, contactNode: String)

case class Join()

case class ForwardJoin(newNode: String, arwl: Int, contactNode: String)

case class Notify ()

case class Disconnect (nodeToDisconnect: String)


//gView
case class InitGlobView (selfAddress : String)

case class NotifyGlobalView (address: String)


//Application Messages

case class ShowGV(address: String)

case class ShowPV(address: String)

case class ReplyShowView(replyType: String, myself: String,  nodes: List[String])

// Information Dissemination

case class InitGossip(selfAddress : String)

case class ShowNeighbours(neigh: List[String])



case class BroadcastMessage(newNode : String)

case class ForwardBcast(mid: Int, m: String, hop: Int)

case class PendingMsg(forwardBcastMsg: ForwardBcast, senderAddress: String)

case class GossipAnnouncement(mid: Int)

case class GossipMessage(forwardBcastMsg: ForwardBcast)