package app

import akka.actor.ActorRef

//pView
case class InitMessage(selfAddress: String, contactNode: String)

case class Join()

case class ForwardJoin(newNode: String, arwl: Int, contactNode: String)

case class Notify()

case class Disconnect(nodeToDisconnect: String)

case class ShowPV(address: String)

case class AskPassiveView (priority : String)




//gView
case class InitGlobView(selfAddress: String, contactNode: String)

case class NotifyGlobalView(address: String)

case class ShowGV(address: String)



//Other

case class ReplyShowView(replyType: String, myself: String, nodes: List[String])

case class ReplyStoreAction(replyType: String, myself: String, data: String)


// Information Dissemination

case class InitGossip(selfAddress: String)

case class ShowNeighbours(neigh: List[String])

case class BroadcastMessage(messageType: String, node: String)

case class ForwardBcast(mid: Int, bCastMessage: BroadcastMessage, hop: Int)

case class PendingMsg(forwardBcastMsg: ForwardBcast, senderAddress: String)

case class GossipAnnouncement(mid: Int)

case class GossipMessage(forwardBcastMsg: ForwardBcast)

case class AntiEntropy(knownMessages: List[Int])

case class GossipRequest(mid: Int)



//Storage

case class InitStorage(selfAddress: String)

case class Write(dataId: String, data: String)

case class Read(dataId: String)

case class ForwardWrite(hashedDataId: Int, data: String, appID: ActorRef)

case class ForwardRead(hashedDataId: Int, appID: ActorRef)

//Replication

case class InitPaxos()

case class AskSeqNum()

case class ReplySeqNum(seqNum: Int)

case class Propose(value: String)

case class Prepare(seqNum: Int, value: String)

case class Prepare_OK(seqNum: Int, value: String)

case class Accept(seqNum: Int, value: String)

case class Accept_OK(seqNum: Int, value: String)

case class Decided(value: String)




// Heartbeat

case class Heartbeat()

// Verify PseudoDead processes
case class IsAlive(p: String)

case class Check(from: String)

case class ReplyIsAlive(from: String)

case class AliveMessage(p: String)




//Application

case class MessagesStats(address: String)

case class ReplyMessagesStats(
                               totalSentMessages: Int,
                               totalReceivedMessages: Int,

                               gossipMessagesReceived: Int,
                               gossipMessagesSent: Int,

                               gossipAnnouncementReceived: Int,
                               gossipAnnouncementSent: Int,

                               gossipRequestReceived: Int,
                               gossipRequestSent: Int,

                               antiEntropyReceived: Int,
                               antiEntropySent: Int
                             )






