import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import layers.{GlobalView, PartialView}


object Process extends App {

  val config = configure()

  println (config)


  val sys = ActorSystem("AkkaSystem", config)
  val partialView = sys.actorOf(Props[PartialView], "partialView")

  println(s"Work actor path is ${partialView}")

  def configure(): Config = {
    if (args.length == 0)
      ConfigFactory.load.getConfig("InitialProcess")
    else
      ConfigFactory.load.getConfig("Process")
  }

}

object Process2 extends App {

  val config = ConfigFactory.load.getConfig("Process")

  val sys = ActorSystem("AkkaSystem", config)
  val partialView2 = sys.actorSelection("akka.tcp://AkkaSystem@127.0.0.1:2552/user/partialView")
  println(partialView2.pathString)
  partialView2 ! "Fuck you"

}
