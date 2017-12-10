package replication

import akka.actor.Actor
import app._
import scala.collection.mutable._

class Learner (myself: String, bucket: Int) extends Actor {

  var decision = Operation("", 0, "")
  var na: Int = 0
  var va = Operation("", 0, "")
  var nAcceptOK = 0
  var majority: Boolean = false

  override def receive = {

    case InitPaxos => {
      resetPaxos()
    }

    case accept: Accept_OK_L => {
      println ("Receiving accept_OK")

      if (accept.n >= na) {
        na = accept.n
        va = accept.op

        nAcceptOK += 1
        if (nAcceptOK > accept.replicas.size / 2 && !majority) {
          majority = true

          decision = va

          println ("Sending decide to storage")
          val process = context.actorSelection(s"${myself}/user/storage")
          process ! WriteOP(accept.smCounter, va.key, va.data, accept.leaderHash)
        }
      }
    }
  }

  private def resetPaxos() = {
    majority = false
    nAcceptOK = 0
    println ("Reseting learner...")
  }
}

