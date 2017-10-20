package app

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger

object Application extends App {

  val log = Logger("scala.slick")

  val config = ConfigFactory.load.getConfig("ApplicationConfig")
  val sys = ActorSystem("akkaSystem", config)
  val appActor = sys.actorOf(Props[appActor], "appActor")

  while (true) {
    val line = scala.io.StdIn.readLine()
    var words: Array[String] = line.split("\\s")

    words(0) match {
      case "gv" if (words.length == 2) => showGV(words(1))
      case "pv" if (words.length == 2) => showPV(words(1))
      case _ => println("Wrong command")
    }
  }

  def showGV(process: String) = {
    appActor ! ShowGV(process)
  }

  def showPV(process: String) = {
    appActor ! ShowPV(process)
  }

  class appActor extends Actor {

    override def receive = {

      case ShowGV(x) => {
        var process = sys.actorSelection(s"${x}/user/globalView")
        process ! ShowGV
      }

      case ShowPV(x) => {
        var process = sys.actorSelection(s"${x}/user/partialView")
        process ! ShowPV
      }

      case reply: ReplyAppRequest => {
        println ("-------------------------------------------------------------")
        println(s"${reply.replyType} nodes from ${reply.myself}")
        for (process <- reply.nodes)
          println("\t - " + process)
        println ("-------------------------------------------------------------")
      }
    }
  }

}
