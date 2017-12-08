package replication

import akka.actor.Actor
import app._

import scala.collection.mutable.TreeMap

class Proposer extends Actor{

  var n : Int = 0
  var biggestNseen = 0
  var prepared : Boolean = false
  var nPreparedOk: Int = 0
  var replicas: TreeMap[Int, String] = TreeMap.empty

  override def receive = {

    case init: InitPaxos => {

      replicas = init.replicas
      n = biggestNseen + 1

      if(!prepared){
        for(r <- replicas){
          val process = context.actorSelection(s"${r._2}/user/accepter")
          process ! PrepareAccepter(n, init.op, replicas)
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
          process ! Accept(n, prepOk.op)
        }
      }
    }
  }
}