package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random


class InformationDissemination extends Actor {

  val log = Logger("scala.slick")

  var neigh : List[String] = List.empty
  var delivered : List[ForwardBcast] = List.empty
  var pending : List[PendingMsg] = List.empty
  var requested : List[String] = List.empty
  var currentNeighbours : List[String] = List.empty
  var fanout = 3
  var r = 3
  var myself: String = ""

  override def receive: Receive = {

    case init: InitGossip => {
      myself = init.selfAddress
    }

    case bcastMessage : BroadcastMessage => {

      val mid = bcastMessage.newNode.hashCode

      delivered = delivered :+ ForwardBcast(mid, bcastMessage.newNode, 0)

      pending = pending :+ PendingMsg( ForwardBcast(mid, bcastMessage.newNode, 0) , myself)

      val process = context.actorSelection(s"${myself}/user/partialView")
      process ! ShowNeighbours
    }


    case showNeigh : ShowNeighbours => {

      var gossipTargets : List[String] = List.empty

      currentNeighbours = List.empty

      for(n <- showNeigh.neigh){
        currentNeighbours = currentNeighbours :+ n
        var i = 0
        for(msg <- pending){

          for(gT <- Random.shuffle(currentNeighbours)){
            if(i < fanout){
              gossipTargets = gossipTargets :+ gT
              i = i+1
            }

          }

          for(p <- gossipTargets){
            val process = context.actorSelection(s"${p}/user/partialView")
            if(msg.forwardBcastMsg.hop <= r){
              process ! GossipMessage(ForwardBcast(msg.forwardBcastMsg.mid, msg.forwardBcastMsg.m, msg.forwardBcastMsg.hop + 1))

            }
            process ! GossipAnnouncement(msg.forwardBcastMsg.mid)
          }

        }
      }
      pending = List.empty
    }


    /*case gossipMessage : GossipMessage => {
      var filterDelivered : List[ForwardBcast] = delivered.filter(_.equals(gossipMessage.forwardBcastMsg.mid))

      if(filterDelivered.filter(_.equals(gossipMessage.forwardBcastMsg.m)).contains(gossipMessage)){
        delivered = delivered :+ ForwardBcast(gossipMessage.forwardBcastMsg.mid, gossipMessage.forwardBcastMsg.m, gossipMessage.forwardBcastMsg.hop)


        var filterRequested : List[PendingMsg] = requested.filter(_.equals(gossipMessage.forwardBcastMsg.mid))
        requested = requested.filter(!_.equals(gossipMessage))
      }

    }*/
  }

  def getNeighbours(self : String) ={
    //var listNeigh = self.activeView
  }

}