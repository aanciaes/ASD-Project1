package layers

import akka.actor.{Actor, ActorRef}
import app._
import com.typesafe.scalalogging.Logger


class GlobalView extends Actor {

  val log = Logger("phase1")
  val log2 = Logger("phase2")

  val hashID_2551: Int = math.abs(("akka.tcp://AkkaSystem@127.0.0.1:2551").reverse.hashCode % 1000) //474 in localhost

  var globalView: List[String] = List.empty
  var myself: String = ""
  var id: Int = 0
  //var storage = scala.collection.mutable.HashMap[String, String]()
  //var pending = scala.collection.mutable.Queue[]

  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself

      id = math.abs(init.selfAddress.reverse.hashCode % 1000)
      println("Unique Identifier: " + id)

      val process = context.actorSelection(s"${init.contactNode}/user/globalView")
      process ! ShowGV
    }

    case message: BroadcastMessage => {
      //log.debug("Global view receive Broacast Message from: " + sender.path.address.toString)
      message.messageType match {
        case "add" => {
          if (!message.node.equals(myself))
          //log.debug("adding: " + message.node + " to global view")
            globalView = globalView :+ message.node
        }
        case "del" => {
          if (!message.node.equals(myself))
            globalView = globalView.filter(!_.equals(message.node))
        }
        case _ => log.error("Error, wrong message type")
      }
    }

    case ShowGV => {
      sender ! ReplyShowView("Global View", myself, globalView)
    }

    //Since all global views are up to date, on init
    //Gets contact node global view and copies it to is own
    case reply: ReplyShowView => {
      for (n <- reply.nodes.filter(!_.equals(myself))) {
        globalView = globalView :+ n

      }
    }




    // - - - - - - - - STORAGE - - - - - - - -

    case write: Write => {

      var hashedDataId = math.abs(write.dataId.reverse.hashCode % 1000)
      log2.debug("Received write with key: " + hashedDataId)

      var hashedProcesses = scala.collection.mutable.TreeMap[Int, String]()
      for (n <- globalView) {
        hashedProcesses.put(math.abs((n.reverse.hashCode % 1000)), n)
      }

      if (hashedProcesses.contains(hashedDataId)) {
        log2.debug("HashID " + hashedDataId + " exists")

        if (!hashedDataId.equals(myself.reverse.hashCode % 1000)) {
          log2.debug("Its not me tho...")
          log2.debug("Forwarding to HashID " + hashedDataId)
          val process = context.actorSelection(s"${hashedProcesses.get(hashedDataId).get}/user/storage")
          process ! ForwardWrite(hashedDataId, write.data, sender)
        }
        else {
          log2.debug("And its me!!")
          log2.debug("Storing HashID " + hashedDataId.toString + " with the data: " + write.data)

          val process = context.actorSelection(s"${myself}/user/storage")
          process ! ForwardWrite(hashedDataId, write.data, sender)
        }

      }
      else
        findProcessForWrite(hashedDataId, hashedProcesses, write.data, sender)
    }


    case read: Read => {

      print("Received Read from application")
      var hashedDataId = math.abs(read.dataId.reverse.hashCode % 1000)
      var hashedProcesses = scala.collection.mutable.TreeMap[Int, String]()
      for (n <- globalView) {
        hashedProcesses.put(math.abs((n.reverse.hashCode % 1000)), n)
      }


      if (hashedProcesses.contains(hashedDataId)) {
        log2.debug("HashID " + hashedDataId + " exists")
        if (!hashedDataId.equals(myself.reverse.hashCode % 1000)) {
          log2.debug("Its not me tho...")
          findProcessForRead(hashedDataId, hashedProcesses, sender)
        }
        else { //ITS MEEE

          val process = context.actorSelection(s"${myself}/user/storage")
          process ! ForwardRead(hashedDataId, sender)
        }
      }
      else
        findProcessForRead(hashedDataId, hashedProcesses, sender)
    }

  }


  // - - - - - - - - - - - - - - - - - - - - - - -


  def findProcessForWrite(hashedDataId: Int, hashedProcesses: scala.collection.mutable.TreeMap[Int, String], data: String, appID: ActorRef) = {
    log2.debug("Process " + hashedDataId + " does NOT EXIST in the System")

    var previousN = hashID_2551
    var count = 1
    var break = false

    for ((hash, process) <- hashedProcesses) {

      if (!break) {
        log2.debug("hashProcess: " + hash)
        log2.debug("process: " + process)

        if (hash > hashedDataId) {
          log2.debug("Process " + hash + " is too high")

          //write 300 qdo hashedProcesses = {450, 750, 900} tem que ir po 900
          if (hash == hashedProcesses.firstKey) {
            log2.debug("Forward Write to: " + hashedProcesses.last._2)
            val process = context.actorSelection(s"${hashedProcesses.last._2}/user/storage")
            process ! ForwardWrite(hashedDataId, data, appID)
            break = true
          }
          else {
            log2.debug("Forwarding WRITE to: " + previousN + " with the following address: " + hashedProcesses.get(previousN).get)

            val process = context.actorSelection(s"${hashedProcesses.get(previousN).get}/user/storage")
            process ! ForwardWrite(hashedDataId, data, appID)
            break = true
          }
        }

        else {
          if (count == hashedProcesses.size) {
            val process = context.actorSelection(s"${hashedProcesses.get(hash).get}/user/storage")
            process ! ForwardWrite(hashedDataId, data, appID)
            break = true
          }
          count = count + 1
          previousN = hash
        }
      }
    }
  }


  def findProcessForRead(hashedDataId: Int, hashedProcesses: scala.collection.mutable.TreeMap[Int, String], appID: ActorRef) = {
    log2.debug("Process " + hashedDataId + " does NOT EXIST in the System")

    var previousN = hashID_2551
    var count = 1
    var break = false

    for ((hash, process) <- hashedProcesses) {

      if (!break) {
        log2.debug("hashProcess: " + hash)
        log2.debug("processID: " + process)
        if (hash > hashedDataId) {
          log2.debug("Process " + hash + " is too high")

          //read 300 qdo hashedProcesses = {450, 750, 900} tem que ir po 900
          if (hash == hashedProcesses.firstKey) {
            val process = context.actorSelection(s"${hashedProcesses.last._2}/user/storage")
            process ! ForwardRead(hashedDataId, appID)
            break = true
          }
          else {
            log2.debug("Forwarding READ to: " + previousN + " with the following address: " + hashedProcesses.get(previousN).get)

            val process = context.actorSelection(s"${hashedProcesses.get(previousN).get}/user/storage")
            process ! ForwardRead(hashedDataId, appID)
            break = true
          }
        }

        else {
          if (count == hashedProcesses.size) {
            val process = context.actorSelection(s"${hashedProcesses.get(hash).get}/user/storage")
            process ! ForwardRead(hashedDataId, appID)
            break = true
          }
          count = count + 1
          previousN = hash
        }
      }
    }
  }
}