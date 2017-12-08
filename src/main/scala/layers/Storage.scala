package layers

import akka.actor.{Actor, ActorRef}
import app._
import replication.StateMachine

import scala.collection.mutable._

class Storage extends Actor {

  var storage = HashMap[String, String]()
  var pending = Queue[Operation]()
  var replicas = TreeMap[Int, String]()
  var stateMachines = TreeMap[Int, StateMachine]()
  var myself: String = ""
  var myselfHashed: Int = 0

  var applicationAddr: ActorRef = ActorRef.noSender

  override def receive = {

    case init: InitReplication => {
      myself = init.selfAddress
      myselfHashed = init.myselfHashed
      replicas = init.replicas

      for (r <- replicas) {
        stateMachines.put(r._1, new StateMachine(r._1, replicas, context.system))
      }

      println("My replicas are: ")
      for (r <- replicas) {
        println(r)
      }
      println("- - - - - - - - - - - -")
    }

    case write: ForwardWrite => {
      println("Forward Write received")
      println("myself hashed: " + math.abs(myself.reverse.hashCode % 1000) + " STORED ID: " + write.hashedDataId + " with the data: " + write.data.toString)

      applicationAddr = write.appID

      val op = Operation("Write", write.hashedDataId, write.data)
      val stateCounter = stateMachines.get(myselfHashed).get.getCounter()

      pending.enqueue(op)

      val leader = stateMachines.get(myselfHashed).get
      leader.initPaxos(op, myselfHashed, write.appID, myself)
    }

    case read: ForwardRead => {
      println("---ForwardRead---")
      if (storage.contains(read.hashedDataId.toString)) {
        println("Process hashID: " + math.abs(myself.reverse.hashCode % 1000) + " Got stored HashID: " + read.hashedDataId + " with the data: " + storage.get(read.hashedDataId.toString).get.toString)
        storage.get(read.hashedDataId.toString)

        println("Read completed! Data: " + storage.get(read.hashedDataId.toString))

        //Send back to Application
        val process = context.actorSelection(s"${read.appID.path}")
        process ! ReplyStoreAction("Read", myself, storage.get(read.hashedDataId.toString).get)
      }
      else {
        println("The read with the id: " + read.hashedDataId + " does not exist in the System.")

        //Send back to Application
        val process = context.actorSelection(s"${read.appID.path}")
        process ! ReplyStoreAction("Read", myself, "Read not found in the System!")
      }
    }


    case writeOp: WriteOP => {
      println("Write Op received")
      if (myselfHashed == writeOp.leaderHash) {
        storage.put(writeOp.hashDataId.toString, writeOp.data)

        //Send back to Application
        applicationAddr ! ReplyStoreAction("Write", myself, writeOp.data)
      }
      val stateHash = FindProcess.matchKeys(writeOp.hashDataId, stateMachines)
      stateMachines.get(stateHash).get.write(writeOp.opCounter, writeOp.hashDataId, writeOp.data)
    }
  }
}
