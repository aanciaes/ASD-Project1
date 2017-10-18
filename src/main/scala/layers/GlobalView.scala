package layers

import akka.actor.Actor
import app.{NotifyGlobalView, ReplyAppRequest, ShowGB, ShowPV}

class GlobalView extends Actor {

  var globalView: List[String] = List.empty


  override def receive = {

    case NotifyGlobalView(address) => {
      globalView = globalView :+ address
    }


    /**
      * case ShowGB => {
      * sender ! ReplyAppRequest("Global View", myself, globalView)
      * }*/
  }
}
