package layers

import akka.actor.Actor
import app.Message

class PartialView extends Actor{

  var listNeighs : List [String] = List.empty

  override def receive = {
    case x => {
      println (x)
    }
  }
}
