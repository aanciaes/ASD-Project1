package app

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import layers.{GlobalView, InformationDissemination, PartialView}


object Process extends App {

  val config = configureRemote()

  val sys = ActorSystem("AkkaSystem", config)
  val partialView = sys.actorOf(Props[PartialView], "partialView")
  val globalView = sys.actorOf(Props[GlobalView], "globalView")
  //val informationDessimination = sys.actorOf(Props[InformationDissemination], "informationDessimination")

  var partialViewNeighs : List [String] = List.empty
  if(args.length > 0){
    for (arg <- args)
      partialViewNeighs = partialViewNeighs :+ arg
  }

  for (address <- partialViewNeighs) {
    var process = sys.actorSelection(s"akka.tcp://AkkaSystem@${address}/user/partialView")
    process ! "GAYYY"
  }





  def configureRemote(): Config = {
    var port = 0
    if (args.length == 0) {
      port = 2552

    }
    ConfigFactory.load.getConfig("Process").withValue("akka.remote.netty.tcp.port",
      ConfigValueFactory.fromAnyRef(port))
  }

}