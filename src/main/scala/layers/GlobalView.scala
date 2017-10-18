package layers

import akka.actor.Actor
import app._

class GlobalView extends Actor {

  var globalView: List[String] = List.empty
  var myself: String = ""

  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself
    }

    case NotifyGlobalView(address) => {
      globalView = globalView :+ address
    }

    case ShowGV => {
      sender ! ReplyAppRequest("Global View", myself, globalView)
    }
  }
}
