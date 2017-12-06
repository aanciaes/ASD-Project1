package app

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.Logger
import layers.{GlobalView, InformationDissemination, PartialView, Storage}
//import replication._


object Process extends App {

  val log = Logger("scala.slick")

  var port = 2551
  if (args.length != 0) {
    port = args(0).toInt
  }

  val config = configureRemote()
  val sys = ActorSystem("AkkaSystem", config)
  val selfAddress = getSelfAddress(port)

  val globalView = sys.actorOf(Props[GlobalView], "globalView")
  val partialView = sys.actorOf(Props[PartialView], "partialView")
  val informationDissemination = sys.actorOf(Props[InformationDissemination], "informationDissemination")
  val storage = sys.actorOf(Props[Storage], "storage")

  /*val proposer = sys.actorOf(Props[Proposer], "proposer")
  val accepter = sys.actorOf(Props[Accepter], "accepter")
  val learner = sys.actorOf(Props[Learner], "learner")*/

  var contactNode = ""
  if (args.length > 1) {
    contactNode = args(1)
  }

  //Inits
  globalView ! InitGlobView(selfAddress, contactNode)
  partialView ! InitMessage(selfAddress, contactNode)
  informationDissemination ! InitGossip(selfAddress)
  storage ! InitStorage(selfAddress)

  def configureRemote(): Config = {

    ConfigFactory.load.getConfig("Process").withValue("akka.remote.netty.tcp.port",
      ConfigValueFactory.fromAnyRef(port))
  }


  def getSelfAddress(port: Int) = {
    val address = config.getAnyRef("akka.remote.netty.tcp.hostname")
    val port = config.getAnyRef("akka.remote.netty.tcp.port")

    s"akka.tcp://${sys.name}@${address}:${port}"
  }

}