package layers

import akka.actor.Actor
import app._
import scala.collection.mutable._

class Storage extends Actor{

  var storage = HashMap[String, String]()
  var pending = Queue[String]()
  var replicas = TreeMap[Int, String]()
  var myself: String = ""

  override def receive = {

    case init: InitReplication => {
      myself = init.selfAddress

      replicas = init.replicas

      println("My replicas are: ")
      for(r <- replicas){
        println(r)
      }
      println("- - - - - - - - - - - -")
    }

    case write: ForwardWrite => {

      println("myself hashed: " + math.abs(myself.reverse.hashCode % 1000) + " STORED ID: " + write.hashedDataId + " with the data: " + write.data.toString)
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
  }
}
