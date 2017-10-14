package layers

import akka.actor.Actor
import app.{InitMessage, Notify}

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
    }

    case Notify => {
      println("Receiving notify from: " + sender.path.address.toString)
      listNeighs = listNeighs :+ (sender.path.address.toString)
      for (neigh <- listNeighs)
        println (neigh)
    }
  }
}
