package app

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Application extends App {

  val config = ConfigFactory.load.getConfig("ApplicationConfig")
  val sys = ActorSystem("akkaSystem", config)
  val appActor = sys.actorOf(Props[appActor], "appActor")

  while (true) {
    val line = scala.io.StdIn.readLine()
    var words: Array[String] = line.split("\\s")

    words(0) match {
      case "gb" if (words.length == 2) => showGB(words(1))
      case "pv" if (words.length == 2) => showPV(words(1))
      case _ => println("Wrong command")
    }
  }

  def showGB(process: String) = {
    appActor ! ShowGB(process)
  }

  def showPV(process: String) = {
    appActor ! ShowPV(process)
  }

  class appActor extends Actor {

    override def receive = {

      case ShowGB(x) => {
        var process = sys.actorSelection(s"${x}/user/globalView")
        process ! ShowGB
      }

      case ShowPV(x) => {
        var process = sys.actorSelection(s"${x}/user/partialView")
        process ! ShowPV
      }

      case reply : ReplyAppRequest => {
        println (s"${reply.replyType} nodes from ${reply.myself}")
        for(process <- reply.nodes)
          println (process)
      }
    }
  }
}
