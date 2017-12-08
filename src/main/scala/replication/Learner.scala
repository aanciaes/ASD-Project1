package replication

import akka.actor.Actor
import app._
import scala.collection.mutable._

class Learner extends Actor {

  var decision = Operation("", 0, "")
  var na: Int = 0
  var va = Operation("", 0, "")
  var aSet: List[String] = List.empty

  override def receive = {

    case accept: Accept_OK_L => {


      if (accept.n > na) {
        na = accept.n
        va = accept.op
        aSet = List.empty
      }
      else if (accept.n < na) {
        //Return
      }
      aSet :+ sender.path.address.toString
      if (aSet.size > accept.replicas.size / 2) {
        decision = va

        for(r <- accept.replicas){
          val process = context.actorSelection(s"${r}/user/storage")
          process ! WriteOP(accept.smCounter, va.key, va.data, accept.leaderHash)
        }

      }

    }
  }
}

