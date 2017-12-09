package replication

import akka.actor.Actor
import app._

import scala.collection.mutable.TreeMap

class Accepter (myself: String, bucket: Int) extends Actor{

  var np: Int = 0
  var na: Int = 0
  var va = Operation("", 0, "")

  override def receive = {

    case prepare: PrepareAccepter => {
      println()
      println ("Receiving prepare from: " + sender.path.address + " with op: " + prepare.op)

      if(prepare.n > np){
        np = prepare.n

        println ("Sending prepare_ok to: " + sender.path.address)
        sender ! Prepare_OK(np, prepare.op)
      }
    }

    case accept: Accept => {
      println ()
      println ("Receiving accept op: " + accept.op + " and n: " + accept.n + " from: " + sender.path.address)

      if(accept.n >= np){
        na = accept.n
        va = accept.op

        sender ! Accept_OK_P(accept.n, va)

        for(r <- accept.replicas) {
          println ("Sending accept_OK to: learner " + r._1)
          val process = context.actorSelection(s"${r._2}/user/learner" + r._1)
          process ! Accept_OK_L(na, va, accept.replicas, accept.leaderHash, accept.smCounter)
        }
      }
    }
  }
}

