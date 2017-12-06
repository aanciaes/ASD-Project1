package layers

import akka.actor.Actor
import app._

class Storage extends Actor{

  val hashID_2551: Int = math.abs(("akka.tcp://AkkaSystem@127.0.0.1:2551").reverse.hashCode % 1000) //474 in localhost

  var storage = scala.collection.mutable.HashMap[String, String]()
  var pending = scala.collection.mutable.Queue[String]()
  //var storage = scala.collection.mutable.HashMap[String, List[Byte]]()
  //var defaultData: List[Byte] = List.empty

  var myself: String = ""

  override def receive = {

    case init: InitStorage => {
      myself = init.selfAddress
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
