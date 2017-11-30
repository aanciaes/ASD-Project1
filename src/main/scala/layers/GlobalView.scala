package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random

class GlobalView extends Actor {

  val log = Logger("scala.slick")

  var globalView: List[String] = List.empty
  var myself: String = ""

  var id: Int = 0

  //var storage = scala.collection.mutable.HashMap[String, List[Byte]]()
  var storage = scala.collection.mutable.HashMap[String, String]()
  var defaultData: List[Byte] = List.empty

  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself

      val random = new Random()
      id = math.abs(init.selfAddress.hashCode%1000)
      println ("Unique Identifier: " + id)

      val process = context.actorSelection(s"${init.contactNode}/user/globalView")
      process ! ShowGV
    }

    case message: BroadcastMessage => {
      log.debug("Global view receive Broacast Message from: " + sender.path.address.toString)
      message.messageType match {
        case "add" => {
          if (!message.node.equals(myself))
            log.debug("adding: " + message.node + " to global view")
          globalView = globalView :+ message.node
        }
        case "del" => {
          if (!message.node.equals(myself))
            globalView = globalView.filter(!_.equals(message.node))
        }
        case _ => log.error("Error, wrong message type")
      }
    }

    case ShowGV => {
      sender ! ReplyShowView("Global View", myself, globalView)
    }

    //Since all global views are up to date, on init
    //Gets contact node global view and copies it to is own
    case reply: ReplyShowView => {
      for (n <- reply.nodes.filter(!_.equals(myself)))
        globalView = globalView :+ n
    }




    // - - - - - - - - STORAGE - - - - - - - -

    case write: Write => {
      log.debug("Received write with key: " + (write.id.hashCode%1000).toString)
      log.debug("Data: " + write.data)
      storage.put((write.id.hashCode%1000).toString, write.data)

    }

    case read: Read => {

      if (storage.exists(_ == read.id)) {
        storage.get((read.id.hashCode%1000).toString)
      }
      else
        defaultData

    }
  }
}