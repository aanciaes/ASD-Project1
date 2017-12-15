package app

import akka.actor.ActorRef
import replication.StateMachine

import scala.collection.mutable._

//pView
case class InitMessage(selfAddress: String, contactNode: String)

case class Join()

case class ForwardJoin(newNode: String, arwl: Int, contactNode: String)

case class Notify()

case class Disconnect(nodeToDisconnect: String)

case class ShowPV(address: String)

case class AskPassiveView(priority: String)


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

case class Write(dataId: String, data: String)

case class Read(dataId: String)

case class ForwardWrite(hashedDataId: Int, data: String)

case class ForwardRead(hashedDataId: Int)


//Replication

case class InitReplication(replicasFront: TreeMap[Int, String], replicasBack: TreeMap[Int, String], selfAddress: String,
                           myselfHashed: Int, node: String, nodeHashed: Int, isNew: Boolean)

case class GetStateMachine()

case class ReplyGetStateMachine (bucket: Int, counter: Int, ops: TreeMap[Int, Operation])

case class AskSeqNum()

case class ReplySeqNum(seqNum: Int)

case class ExecuteOP(opType: String, opCounter: Int, hashDataId: Int, data: String, leaderHash: Int)

case class TransferData (ops: List[Operation])


// Paxos

case class InitPaxos(op: Operation, myselfHashed: Int, replicas: TreeMap[Int, String], smCounter: Int)

case class PrepareAccepter(n: Int, op: Operation)

case class PrepareLearner()

case class Prepare_OK(n: Int, op: Operation)

case class Accept(n: Int, op: Operation, replicas: TreeMap[Int, String], leaderHash: Int, smCounter: Int)

case class Accept_OK_L(n: Int, op: Operation, replicas: TreeMap[Int, String], leaderHash: Int, smCounter: Int)

case class Accept_OK_P(n: Int, op: Operation)


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

case class ShowBuckets(reply: String)

case class ShowReplicas(reply: String)

case class ReplyShowBuckets (print : String)

case class ReplyShowReplicas(printFrontR: String, printBackR: String)

case class RemoveDeadReplica(deadReplicaHashed: Int, newReplicas: TreeMap[Int, String])




