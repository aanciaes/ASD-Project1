package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

class GlobalView extends Actor {

  val log = Logger("scala.slick")

  var globalView: List[String] = List.empty
  var myself: String = ""

  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself

      val process = context.actorSelection(s"${init.contactNode}/user/globalView")
      process ! ShowGV
    }

    case message: BroadcastMessage => {
      log.debug("Global view receive Broacast Message from: " + sender.path.address.toString)
      message.messageType match {
        case "add" => {
          if (!message.node.equals(myself))
            log.debug ("adding: " + message.node + " to global view")
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

    case reply: ReplyShowView => {
      for (n <- reply.nodes.filter(!_.equals(myself)))
        globalView = globalView :+ n
    }
  }
}