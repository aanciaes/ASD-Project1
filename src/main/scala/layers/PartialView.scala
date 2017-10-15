package layers

import akka.actor.Actor
import app.{InitMessage, Notify, NotifyGlobalView}

class PartialView extends Actor{

  var listNeighs : List [String] = List.empty

  override def receive = {

    case message : InitMessage => {
      listNeighs = message.neighs

      for (neigh <- listNeighs) {
        val process = context.actorSelection(s"${neigh}/user/partialView")
        println ("Sending to: " + process.toString())
        process ! Notify
      }

      //Notify self.global view with self.address
      val gb = context.actorSelection("/user/globalView")
      println ("Partial View sending to local process: " + gb)
      gb ! NotifyGlobalView(message.selfAddress)

    }

    case Notify => {
      println("Receiving notify from: " + sender.path.address.toString)
      listNeighs = listNeighs :+ (sender.path.address.toString)
      for (neigh <- listNeighs)
        println (neigh)
    }
  }
}
