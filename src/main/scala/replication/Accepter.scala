package replication

import akka.actor.Actor
import app._

import scala.collection.mutable.TreeMap

class Accepter extends Actor{

  var np: Int = 0
  var na: Int = 0
  var va = Operation("", 0, "")

  override def receive = {

    case prepare: PrepareAccepter => {
      if(prepare.n > np){
        np = prepare.n

        sender ! Prepare_OK(np, prepare.op)
      }
    }

    case accept: Accept => {

      if(accept.n > np){
        na = accept.n
        va = accept.op

        sender ! Accept_OK_P(accept.n, va)

        for(r <- accept.replicas) {
          val process = context.actorSelection(s"${r}/user/learner")
          process ! Accept_OK_L(na, va, accept.replicas, accept.leaderHash, accept.smCounter)
        }
      }
    }
  }
}

