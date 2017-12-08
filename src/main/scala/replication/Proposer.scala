package replication

import akka.actor.{Actor, ActorRef}
import app._

import scala.collection.mutable.TreeMap

class Proposer extends Actor{

  var n : Int = 0
  var biggestNseen = 0
  var prepared : Boolean = false
  var nPreparedOk: Int = 0
  var replicas: TreeMap[Int, String] = TreeMap.empty
  var op = Operation("", 0, "")
  var myself: String = ""
  var myselfHashed = 0
  var smCounter = 0
  var aSet: List[String] = List.empty
  var appID: ActorRef = ActorRef.noSender

  override def receive = {

    case init: InitPaxos => {
      replicas = init.replicas
      op = init.op
      myself = init.myself
      myselfHashed = init.myselfHashed
      smCounter = init.smCounter
      appID = init.appID
      n = biggestNseen + 1

      if(!prepared){
        for(r <- init.replicas){
          val process = context.actorSelection(s"${r._2}/user/accepter")
          process ! PrepareAccepter(n, op)
        }
      }
    }

    case prepOk: Prepare_OK => {
      nPreparedOk += 1

      // Update N
      if(biggestNseen < prepOk.n) {
        biggestNseen = prepOk.n
      }


      if(nPreparedOk > replicas.size/2){

        prepared = true

        for(r <- replicas){
          val process = context.actorSelection(s"${r._2}/user/accepter")
          process ! Accept(n, prepOk.op, replicas, myselfHashed, smCounter)
        }
      }
    }

    case acceptOK: Accept_OK_P => {
      aSet :+ sender.path.address.toString
      if(aSet.size > replicas.size/2){
        val process = context.actorSelection(s"${appID.path}")
        process ! ReplyStoreAction("Write", myself, acceptOK.op.data)
      }
    }
  }
}