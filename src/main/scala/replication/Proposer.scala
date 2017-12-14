package replication

import akka.actor.{Actor, ActorRef}
import app._

import scala.collection.mutable.TreeMap

class Proposer (myself: String, bucket: Int) extends Actor {

  var n: Int = 0
  var biggestNseen = 0

  var prepared: Boolean = false
  var majority: Boolean = false

  var nPreparedOk: Int = 0
  var replicas: TreeMap[Int, String] = TreeMap.empty

  var op = Operation("", 0, "")

  var myselfHashed = 0

  var smCounter = 0
  var nAcceptOk = 0

  override def receive = {

    case init: InitPaxos => {
      println("Paxos Initalized")

      replicas = init.replicas
      println("Number of replicas: " + replicas.size)

      op = init.op
      println("Operation: " + op)

      println("Myself: " + myself)

      myselfHashed = init.myselfHashed
      println("Myself hashed: " + myselfHashed)

      smCounter = init.smCounter
      println("Sm counter: " + smCounter)

      n = biggestNseen + 1
      println("N: " + n)

      resetPaxos()

      if (!prepared) {
        println("Not prepared")
        for (r <- init.replicas) {
          println("Sending prepare to: acceptor " + r._1)
          val process = context.actorSelection(s"${r._2}/user/accepter" + bucket)
          val learner = context.actorSelection(s"${r._2}/user/learner" + bucket)
          process ! PrepareAccepter(n, op)
          learner ! InitPaxos
        }
      }
    }

    case prepOk: Prepare_OK => {
      println()
      println("Recieving prepare_OK from: acceptor " + sender.path.address)

      nPreparedOk += 1

      // Update N
      if (biggestNseen < prepOk.n) {
        biggestNseen = prepOk.n
      }

      if ((nPreparedOk > replicas.size / 2) && !prepared) {
        println("Got majority")
        prepared = true

        for (r <- replicas) {
          println ("Sending accept to: " + r._1 + " with op: " + prepOk.op)
          val process = context.actorSelection(s"${r._2}/user/accepter" + bucket)
          process ! Accept(n, prepOk.op, replicas, myselfHashed, smCounter)
        }
      }
    }

    case acceptOK: Accept_OK_P => {
      println()
      println ("Receiving accept_ok_P")

      nAcceptOk += 1

      if (nAcceptOk > replicas.size / 2 && !majority) {
        majority=true

        println ("Sending response to application")
        val process = context.actorSelection(s"${Utils.applicationAddress}")
        process ! ReplyStoreAction(acceptOK.op.op, myself, acceptOK.op.data)
      }
    }
  }

  private def resetPaxos() = {
    prepared=false
    majority=false
    nPreparedOk = 0
    nAcceptOk = 0
    println ("Reseting Proposer on Init...")
  }
}