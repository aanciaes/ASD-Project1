package replication

import akka.actor.Actor
import app._

class Accepter extends Actor{

  var allNodes: List[String] = List.empty
  var seqNumP: Int = 0
  var seqNumA: Int = 0
  var valueA: String = ""

  override def receive = {

    case init: InitPaxos => {

      val process = context.actorSelection(s"${sender.path.address.toString}/user/globalView")
      process ! ShowGV

    }

    /*case reply: ReplyShowView => {
      for(n <- reply.nodes){
        allNodes += n
      }
    }*/

    case prepare: Prepare => {

      if(prepare.seqNum > seqNumP){
        seqNumP = prepare.seqNum

        sender ! Prepare_OK(prepare.seqNum, prepare.value)
      }

    }

    case accept: Accept => {
      if(accept.seqNum >= seqNumP){
        seqNumA = accept.seqNum
        valueA = accept.value

        sender ! Accept_OK(accept.seqNum, "")

        for(n <- allNodes){
          val process = context.actorSelection(s"${sender.path.address.toString}/user/learner")
          process ! Accept_OK(seqNumA, valueA)
        }
      }

    }


  }
}
