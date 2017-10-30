package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class InformationDissemination extends Actor {

  val log = Logger("scala.slick")
  var sentMessages: Int = 0
  var receivedMessages: Int = 0

  //var neigh: List[String] = List.empty
  var delivered: List[ForwardBcast] = List.empty
  var pending: List[PendingMsg] = List.empty
  var requested: List[Int] = List.empty
  var currentNeighbours: List[String] = List.empty
  var fanout = 3
  var r = 3
  var myself: String = ""
  context.system.scheduler.schedule(0 seconds, 5 seconds)(entropy())

  override def receive: Receive = {

    case init: InitGossip => {
      myself = init.selfAddress
    }

    case bcastMessage: BroadcastMessage => {

      sentMessages = sentMessages + 1
      log.debug("Initializing bCast")

      val mid = (bcastMessage.node + bcastMessage.messageType).hashCode

      if (!bcastMessage.node.equals(myself))
        BcastDeliver(bcastMessage)

      delivered = delivered :+ ForwardBcast(mid, bcastMessage, 0)

      pending = pending :+ PendingMsg(ForwardBcast(mid, bcastMessage, 0), myself)

      getNeighbours()
    }


    case view: ReplyShowView => {
      log.debug("Got self active view")

      var gossipTargets: List[String] = List.empty
      currentNeighbours = view.nodes

      for (n <- currentNeighbours)
        log.debug("Neigh: " + n)

      for (msg <- pending) {
        gossipTargets = randomSelection(msg.senderAddress)

        for (n <- gossipTargets)
          log.debug("Random: " + n)

        for (p <- gossipTargets) {
          val process = context.actorSelection(s"${p}/user/informationDissemination")
          log.debug("Sending gossip message to: " + p)
          if (msg.forwardBcastMsg.hop <= r) {
            process ! GossipMessage(ForwardBcast(msg.forwardBcastMsg.mid, msg.forwardBcastMsg.bCastMessage, msg.forwardBcastMsg.hop + 1))
          } else {
            process ! GossipAnnouncement(msg.forwardBcastMsg.mid)
            log.warn("Sent gossip announencment to: " + process)
          }
        }

      }
      pending = List.empty
    }


    case gossipMessage: GossipMessage => {
      receivedMessages = receivedMessages + 1
      log.debug("Receiving gossip message from: " + sender.path.address.toString)
      //var filterDelivered: List[ForwardBcast] = delivered.filter(_.mid.equals(gossipMessage.forwardBcastMsg.mid))

      //if (filterDelivered.size == 0) {
      if(!delivered.exists(m => (m.mid.equals(gossipMessage.forwardBcastMsg.mid)))){
        delivered = delivered :+ gossipMessage.forwardBcastMsg

        //If same node comes back up again, it is not ignored
        if(gossipMessage.forwardBcastMsg.bCastMessage.messageType.equals("del")){
          val mid = (gossipMessage.forwardBcastMsg.bCastMessage.node+"add").hashCode
          delivered = delivered.filter(!_.mid.equals(mid))
          log.error("Wrong deletion")
        }

        requested = requested.filter(_.equals(gossipMessage.forwardBcastMsg.mid))
        BcastDeliver(gossipMessage.forwardBcastMsg.bCastMessage)
        pending = pending :+ PendingMsg(gossipMessage.forwardBcastMsg, sender.path.address.toString)
        getNeighbours()
      }
    }

    case gossipAnnouncement : GossipAnnouncement => {
      log.warn("Receiving gossip announcement from: " + sender.path.address.toString)
      if(!delivered.exists(p => p.mid==gossipAnnouncement.mid) && !requested.contains(gossipAnnouncement.mid)) {
        log.warn("Sent gossip request to : " + sender.path.address.toString)
        requested = requested :+ gossipAnnouncement.mid
        sender ! GossipRequest(gossipAnnouncement.mid)
      }
    }

    case gossipRequest : GossipRequest => {
      log.warn("Received gossip request from: " + sender.path.address.toString)
      for(m <- delivered){
        if (m.mid == gossipRequest.mid){
          sender ! GossipMessage (ForwardBcast(m.mid, m.bCastMessage, (m.hop+1)))
        }
      }
    }

    case antiEntropy: AntiEntropy => {
      for (msg <- delivered) {
        if (!antiEntropy.knownMessages.contains(msg.mid)) {
          val process = context.actorSelection(s"${sender}/user/informationDissemination")
          process ! GossipMessage(ForwardBcast(msg.mid, msg.bCastMessage, msg.hop))
        }
      }
    }

    case MessagesStats => {
      sender ! ReplyMessagesStats(receivedMessages, sentMessages)
    }
  }

  def randomSelection(sender : String): List[String] = {
    var gossipTargets: List[String] = List.empty
    val shuffled = Random.shuffle(currentNeighbours)

    var i = 0
    for (gT <- shuffled) {
      if (i < fanout && gT != null && !gT.equals(sender)) {
        gossipTargets = gossipTargets :+ gT
        i = i + 1
      }
    }
    return gossipTargets
  }

  def getNeighbours() = {
    log.debug("Getting self active view")
    val process = context.actorSelection(s"${myself}/user/partialView")
    process ! ShowPV
  }

  def BcastDeliver(broadcastMessage: BroadcastMessage) = {
    val process = context.actorSelection(s"${myself}/user/globalView")
    process ! broadcastMessage

    //Clean up delivered messages so if node comes up again is inserted with success
    if(broadcastMessage.messageType.equals("del")){
      delivered = delivered.filter(!_.bCastMessage.node.equals(broadcastMessage.node))
    }
  }

  def entropy() = {
    if (!currentNeighbours.isEmpty) {
      var p: String = Random.shuffle(currentNeighbours).head
      var knownMessages: List[Int] = List.empty
      for (msg <- delivered) {
        knownMessages = knownMessages :+ msg.mid
      }
      val process = context.actorSelection(s"${p}/user/informationDissemination")
      process ! AntiEntropy
    }
  }
}