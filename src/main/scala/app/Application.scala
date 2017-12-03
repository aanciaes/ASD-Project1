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
      case "ms" if (words.length == 2) => messagesStats(words(1))
      case "write" if(words.length == 3) => write(words(1), words(2))
      case "read" if(words.length == 2) => read(words(1))
      //case "msall" if (words.length == 2) => messagesStatsAll()
      case "clear" => {
        for (i <- 1 to 20)
          println()
      }
      case "" => println()
      case _ => println("Wrong command")
    }
  }

  def showGV(process: String) = {
    appActor ! ShowGV(process)
  }

  def showPV(process: String) = {
    appActor ! ShowPV(process)
  }

  def messagesStats(process: String) ={
    appActor ! MessagesStats(process)
  }

  def write(process: String, data: String) ={
    appActor ! Write(process, data)
  }

  def read(process: String) ={
    appActor ! Read(process)
  }
  //def messagesStatsAll() = {

  //}

  class appActor extends Actor {

    override def receive = {

      case ShowGV(x) => {
        val process = sys.actorSelection(s"${x}/user/globalView")
        process ! ShowGV
      }

      case ShowPV(x) => {
        val process = sys.actorSelection(s"${x}/user/partialView")
        process ! ShowPV
      }

      case MessagesStats(x) => {
        val process = sys.actorSelection(s"${x}/user/informationDissemination")
        process ! MessagesStats
      }

      case reply: ReplyShowView => {
        println ("-------------------------------------------------------------")
        println(s"${reply.replyType} nodes from ${reply.myself}")
        for (process <- reply.nodes)
          println("\t - " + process)
        println ("-------------------------------------------------------------")
      }

      case replyStore: ReplyStoreAction => {
        println ("-------------------------------------------------------------")
        println (s"${replyStore.replyType} from ${replyStore.myself} with the DATA: ${replyStore.data}")
        println ("-------------------------------------------------------------")
      }

      case stats : ReplyMessagesStats => {
        println ("-------------------------------------------------------------")
        println(s"Messages from ${sender.path.address.toString}")
        println ("\t - Received Messages: " + stats.totalReceivedMessages)
        println ("\t - Sent Messages: " + stats.totalSentMessages)
        println ()
        println ("\t - Received Gossip Messages: " + stats.gossipMessagesReceived)
        println ("\t - Sent Gossip Messages: " + stats.gossipMessagesSent)
        println ()
        println ("\t - Received Gossip Announcements: " + stats.gossipAnnouncementReceived)
        println ("\t - Sent Gossip Announcements: " + stats.gossipAnnouncementSent)
        println ()
        println ("\t - Received Gossip Requests: " + stats.gossipRequestReceived)
        println ("\t - Sent Gossip Requests: " + stats.gossipRequestSent)
        println ()
        println ("\t - Received Anti Entropy Messages: " + stats.antiEntropyReceived)
        println ("\t - Sent Anti Entropy Messages: " + stats.antiEntropySent)
        println ()
        println ("-------------------------------------------------------------")
      }

      case writeStorage : Write => {
        val process = sys.actorSelection(s"${"akka.tcp://AkkaSystem@127.0.0.1:2551"}/user/globalView")
        process ! Write(writeStorage.id, writeStorage.data)
      }

      case readStorage : Read => {
        val process = sys.actorSelection(s"${"akka.tcp://AkkaSystem@127.0.0.1:2551"}/user/globalView")
        process ! Read(readStorage.id)
      }
    }
  }
}