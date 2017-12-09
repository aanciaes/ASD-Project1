package replication

import akka.actor.{Actor, ActorRef}
import app._

import scala.collection.mutable.TreeMap

class Proposer extends Actor {

  var n: Int = 0
  var biggestNseen = 0

  var prepared: Boolean = false
  var majority: Boolean = false

  var nPreparedOk: Int = 0
  var replicas: TreeMap[Int, String] = TreeMap.empty

  var op = Operation("", 0, "")

  var myself: String = ""
  var myselfHashed = 0

  var smCounter = 0
  var nAcceptOk = 0
  var appID: ActorRef = ActorRef.noSender

  override def receive = {

    case init: InitPaxos => {
      println("Paxos Initalized")

      replicas = init.replicas
      println("Number of replicas: " + replicas.size)

      op = init.op
      println("Operation: " + op)

      myself = init.myself
      println("Myself: " + myself)

      myselfHashed = init.myselfHashed
      println("Myself hashed: " + myselfHashed)

      smCounter = init.smCounter
      println("Sm counter: " + smCounter)

      appID = init.appID
      println("App Id: " + appID)

      n = biggestNseen + 1
      println("N: " + n)

      if (!prepared) {
        println("Not prepared")
        for (r <- init.replicas) {
          println("Sending prepare to: acceptor " + r._1)
          val process = context.actorSelection(s"${r._2}/user/accepter" + r._1)
          process ! PrepareAccepter(n, op)
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
          val process = context.actorSelection(s"${r._2}/user/accepter" + r._1)
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
        println ("Sending reponse to application")
        val process = context.actorSelection(s"${appID.path}")
        process ! ReplyStoreAction("Write", myself, acceptOK.op.data)
      }
    }
  }
}