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
    }

    case ShowGV => {
      sender ! ReplyAppRequest("Global View", myself, globalView)
    }
  }
}
