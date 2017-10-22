package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random


class InformationDissemination extends Actor {

  val log = Logger("scala.slick")

  var neigh: List[String] = List.empty
  var delivered: List[ForwardBcast] = List.empty
  var pending: List[PendingMsg] = List.empty
  //var requested: List[Int] = List.empty
  var currentNeighbours: List[String] = List.empty
  var fanout = 3
  //var r = 3
  var myself: String = ""

  override def receive: Receive = {

    case init: InitGossip => {
      myself = init.selfAddress

    }

    case bcastMessage: BroadcastMessage => {

      log.debug("Initializing bCast")

      val mid = bcastMessage.newNode.hashCode

      delivered = delivered :+ ForwardBcast(mid, bcastMessage.newNode, 0)

      pending = pending :+ PendingMsg(ForwardBcast(mid, bcastMessage.newNode, 0), myself)

      getNeighbours()
    }


    case view: ReplyShowView => {
      log.debug("Got self active view")

      var gossipTargets: List[String] = List.empty
      currentNeighbours = view.nodes

      for (n <- currentNeighbours)
        log.debug("Neigh: " + n)

      for (msg <- pending) {
        gossipTargets = randomSelection()

        for (n <- gossipTargets)
          log.debug("Random: " + n)

        for (p <- gossipTargets) {
          val process = context.actorSelection(s"${p}/user/informationDissemination")
          log.debug("Sending gossip message to: " + p)
          //if (msg.forwardBcastMsg.hop <= r) {
            process ! GossipMessage(ForwardBcast(msg.forwardBcastMsg.mid, msg.forwardBcastMsg.m, msg.forwardBcastMsg.hop + 1))
          //} else {
            //process ! GossipAnnouncement(msg.forwardBcastMsg.mid)
          //}
        }

      }
      pending = List.empty
    }


    case gossipMessage: GossipMessage => {
      log.debug("Receiving gossip message from: " + sender.path.address.toString)
      var filterDelivered: List[ForwardBcast] = delivered.filter(_.mid.equals(gossipMessage.forwardBcastMsg.mid))

      if (filterDelivered.size == 0) {
        delivered = delivered :+ gossipMessage.forwardBcastMsg
        //requested = requested.filter(_.equals(gossipMessage.forwardBcastMsg.mid))
        BcastDeliver(gossipMessage.forwardBcastMsg.m)
        pending = pending :+ PendingMsg(gossipMessage.forwardBcastMsg, sender.path.address.toString)
        getNeighbours()
      }
    }
  }

  def randomSelection(): List[String] = {
    var gossipTargets: List[String] = List.empty

    var i = 0
    for (gT <- Random.shuffle(currentNeighbours)) {
      if (i < fanout && gT != null) {
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

  def BcastDeliver (message : String) = {
    val process = context.actorSelection(s"${myself}/user/globalView")
    process ! message
  }

}