package layers

import akka.actor.Actor
import app._
import replication.StateMachine

import scala.collection.mutable._

class Storage extends Actor{

  var storage = HashMap[String, String]()
  var pending = Queue[String]()
  var replicas = TreeMap[Int, String]()
  var stateMachines = TreeMap [Int, StateMachine]()
  var myself: String = ""
  var myselfHashed: Int = 0

  override def receive = {

    case init: InitReplication => {
      myself = init.selfAddress
      myselfHashed = init.myselfHashed
      replicas = init.replicas

      for((hash, addr) <- replicas){
        stateMachines.put(hash, new StateMachine(hash, replicas))
      }

      println("My replicas are: ")
      for(r <- stateMachines){
        println(r)
      }
      println("- - - - - - - - - - - -")
    }

    case write: ForwardWrite => {

      println("myself hashed: " + math.abs(myself.reverse.hashCode % 1000) + " STORED ID: " + write.hashedDataId + " with the data: " + write.data.toString)

      val stateCounter = stateMachines.get(myselfHashed).get.getCounter()
      for(r <- replicas){
        val process = context.actorSelection(s"${r._2}/user/storage")
        process ! WriteOP(stateCounter, write.hashedDataId, write.data, myselfHashed)
      }

      storage.put((write.hashedDataId).toString, write.data)

      //Send back to Application
      val process = context.actorSelection(s"${write.appID.path}")
      process ! ReplyStoreAction("Write", myself, write.data)
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
      if(myselfHashed == writeOp.leaderHash){
        storage.put(writeOp.hashDataId.toString, writeOp.data)
      }
      stateMachines.get(myselfHashed).get.write(writeOp.opCounter, writeOp.hashDataId, writeOp.data)
    }
  }
}
