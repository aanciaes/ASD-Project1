/*
package replication

import akka.actor.Actor
import app._


class Proposer extends Actor{

  var allNodes: List[String] = List.empty
  var seqNum: Int = 0;
  var value: String = "";
  var client: String = "";

  override def receive = {

    case init: InitPaxos => {

      val process = context.actorSelection(s"${sender.path.address.toString}/user/globalView")
      process ! ShowGV

      val process2 = context.actorSelection(s"${sender.path.address.toString}/user/proposer")
      process2 ! AskSeqNum

    }

    /*case reply: ReplyShowView => {
      for(n <- reply.nodes){
        allNodes += n
      }
    }*/

    /*
    case askSeqNum: AskSeqNum => {
      sender ! ReplySeqNum(seqNum)
    }

    case replySeqNum: ReplySeqNum => {
      seqNum = replySeqNum.seqNum
    }
    */

    case propose: Propose => {

      client = sender.path.address.toString

      for(n <- allNodes){
        val process = context.actorSelection(s"${n}/user/accepter")
        process ! Prepare(seqNum, propose.value)
      }

    }

    case prepareOk: Prepare_OK => {
      value = prepareOk.value

      for(n <- allNodes){
        val process = context.actorSelection(s"${n}/user/accepter")
        process ! Accept(seqNum, value)
      }

    }

    case acceptOk: Accept_OK => {
      //val process = context.actorSelection(s"${client}/user/accepter")
      //process ! Decided(value)
    }

  }


}
*/
