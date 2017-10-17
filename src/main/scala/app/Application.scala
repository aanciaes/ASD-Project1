package app

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Application extends App{

  val config = ConfigFactory.load.getConfig("AppConf")
  val sys = ActorSystem("akkaSystem", config)
  val appActor = sys.actorOf(Props[appActor], "appActor")

  while (true){
    val line = scala.io.StdIn.readLine()
    var words : Array [String] = line.split("\\s")

    words(0) match {
      case "print" => print(words(1))
      case "show"  => println ("Commane Show")
      case _ => println ("Wrong command")
    }
  }

  def print (actor : String) = {
    appActor ! Send (actor)
  }

  class appActor extends Actor {


    override def receive = {

      case Send(x) => {
        var process = sys.actorSelection(s"${x}/user/partialView")
        process ! "Hello"
      }

      case Receive(x) => {
        println (x)
      }
    }
  }


  case class Send (address : String)
  case class Receive (message : String)
}
