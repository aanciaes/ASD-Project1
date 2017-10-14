package layers

import akka.actor.Actor

class PartialView extends Actor{

  override def receive = {
    case x => {
      println (x)
    }
  }

}
