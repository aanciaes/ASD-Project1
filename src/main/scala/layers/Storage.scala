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

      for (st <- stateMachines){
        if (!replicas.contains(st._1))
          //TODO: remove state machine and transfer all data and all storage
      }

      for (r <- replicas) {
        if(!stateMachines.contains(r._1))
          stateMachines.put(r._1, new StateMachine(myself, r._1, replicas, context.system))
      }

      println("My replicas are: ")
      for (r <- replicas) {
        println(r)
      }
      println("- - - - - - - - - - - -")

      println("My sate machines are: ")
      for (r <- stateMachines) {
        println(r)
      }
      println("- - - - - - - - - - - -")
    }

    case write: ForwardWrite => {
      println("Forward Write received")
      println("myself hashed: " + math.abs(myself.reverse.hashCode % 1000) + " STORED ID: " + write.hashedDataId + " with the data: " + write.data.toString)

      applicationAddr = write.appID

      val op = Operation("Write", write.hashedDataId, write.data)

      pending.enqueue(op)

      //TODO: something, don't know why this is where
      val stateCounter = stateMachines.get(myselfHashed).get.getCounter()

      val leader = stateMachines.get(myselfHashed).get
      leader.initPaxos(op, myselfHashed, write.appID)
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
        try {
          pending.dequeue()
          storage.put(writeOp.hashDataId.toString, writeOp.data)
        } catch {
          case ioe: NoSuchElementException => //
          case e: Exception => //
        }
      }

      val stateHash = Utils.matchKeys(writeOp.hashDataId, stateMachines)
      stateMachines.get(stateHash).get.write(writeOp.opCounter, writeOp.hashDataId, writeOp.data)
    }

    case ShowBuckets => {
      var toPrint = ""

      for ((hash,st) <- stateMachines) {
        toPrint +=  "State Machine of bucket: " + hash + "\n"
        for ((op, value) <- st.stateMachine)
          toPrint += " - Operation number: " + op + " Operation: " + value + "\n"
      }

      sender ! ReplyShowBuckets(toPrint)
    }
  }
}
