package app

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import layers.{GlobalView, InformationDissemination, PartialView}


object Process extends App {

  val config = configureRemote()
  val port = args(0)


  val sys = ActorSystem("AkkaSystem", config)

  val selfAddress = getSelfAddress(port.toInt)

  val partialView = sys.actorOf(Props[PartialView], "partialView")
  val globalView = sys.actorOf(Props[GlobalView], "globalView")
  //val informationDessimination = sys.actorOf(Props[InformationDissemination], "informationDessimination")

  var neighs : List [String] = List.empty
  if(args.length > 1){
    for ( index <- 1 to (args.length-1))
      neighs = neighs :+ args(index)
  }
  println(selfAddress)
  println(neighs.foreach(p => println(p)))
  println(neighs.size)

  for ( x <- neighs)
    println(x)

  /**
  for (address <- neighs) {

    partialView ! InitMessage(neighs)
  }
  */

  def configureRemote(): Config = {

    val config = ConfigFactory.load.getConfig("Process")

    if(args.length != 0) {
      config.withValue("akka.remote.netty.tcp.port",
        ConfigValueFactory.fromAnyRef(port))
    }
    config
  }


  def getSelfAddress(port : Int) = {
    val address = config.getAnyRef("akka.remote.netty.tcp.hostname")
    val port = config.getAnyRef("akka.remote.netty.tcp.port")

    s"akka.tcp://${sys.name}@${address}:${port}"
  }

}