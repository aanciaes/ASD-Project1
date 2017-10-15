package layers

import akka.actor.Actor
import app.NotifyGlobalView

class GlobalView extends Actor{

  var globalView : List [String] = List.empty


  override def receive = {

    case NotifyGlobalView (address) => {
      globalView = globalView :+ address
    }

  }
}
