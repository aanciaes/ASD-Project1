package layers

import akka.actor.{Actor, ActorRef}
import app._
import com.typesafe.scalalogging.Logger

import scala.util.control.Breaks._



class GlobalView extends Actor {

  val log = Logger("scala.slick")
  val hashID_2551 : Int = 474

  var globalView: List[String] = List.empty
  var myself: String = ""


  var id: Int = 0

  //var storage = scala.collection.mutable.HashMap[String, List[Byte]]()
  var storage = scala.collection.mutable.HashMap[String, String]()
  var defaultData: List[Byte] = List.empty

  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself


      id = math.abs(init.selfAddress.reverse.hashCode%1000)
      println ("Unique Identifier: " + id)

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
      for (n <- reply.nodes.filter(!_.equals(myself))){
        globalView = globalView :+ n

      }
    }




    // - - - - - - - - STORAGE - - - - - - - -

    case write: Write => {

      var idWrite = math.abs(write.id.reverse.hashCode%1000)
      log.debug("Received write with key: " + idWrite)

      var hashedProcesses = scala.collection.mutable.HashMap[Int, String]()
        for(n<-globalView){
        hashedProcesses.put(math.abs((n.reverse.hashCode%1000)), n)
      }
      hashedProcesses.toSeq.sortBy(_._1)

      if(hashedProcesses.contains(idWrite)) {
        log.debug("HashID " + idWrite + " exists")

        if(!idWrite.equals(myself.reverse.hashCode%1000)){
          log.debug("Its not me tho...")
          log.debug("Forwarding to HashID " + idWrite)
          val process = context.actorSelection(s"${hashedProcesses.get(idWrite).get}/user/globalView")
          process ! ForwardWrite(idWrite, write.data, sender)
        }
        else {
          log.debug("And its me!!")
          log.debug("Storing HashID " + idWrite.toString + " with the data: " + write.data)
          storage.put(idWrite.toString, write.data)

          //Send back to Application
          sender ! ReplyStoreAction("Write", myself, write.data)
        }

      }
      else
        findProcessForWrite(idWrite, hashedProcesses, write.data, sender)
    }


    case read: Read => {

      print("Received Read from application")
      var idRead = math.abs(read.id.reverse.hashCode%1000)
      var hashedProcesses = scala.collection.mutable.HashMap[Int, String]()
      for(n<-globalView){
        hashedProcesses.put(math.abs((n.reverse.hashCode%1000)), n)
      }
      hashedProcesses.toSeq.sortBy(_._1)

      if(hashedProcesses.contains(idRead)) {
        log.debug("HashID " + idRead + " exists")
        if(!idRead.equals(myself.reverse.hashCode%1000)) {
          log.debug("Its not me tho...")

          if (storage.exists(_ == idRead)) {
            log.debug("But I have it stored!!")
            storage.get(idRead.toString)
            log.debug("Read completed! Data: " + storage.get(idRead.toString))
            sender ! ReplyStoreAction("Read", myself, storage.get(idRead.toString).get)
          }
          else{
            findProcessForRead(idRead, hashedProcesses, sender)
          }

        }
        else { //ITS MEEE
          log.debug("I have the read! Data: " + storage.get(idRead.toString))
          storage.get(idRead.toString)
          sender ! ReplyStoreAction("Read", myself, storage.get(idRead.toString).get)
        }

      }
      else
        defaultData
    }

    case forwardWrite: ForwardWrite => {
      log.debug("Process hashID: " + math.abs(myself.reverse.hashCode%1000) + " STORED ID: " + forwardWrite.id + " with the data: " + forwardWrite.data)
      storage.put((forwardWrite.id).toString, forwardWrite.data)

      //Send back to Application
      val process = context.actorSelection(s"${forwardWrite.appID.path}")
      process ! ReplyStoreAction("Write", myself, forwardWrite.data)
    }

    case forwardRead: ForwardRead => {
      log.debug("Process hashID: " + math.abs(myself.reverse.hashCode%1000) + " Got stored HashID: " + forwardRead.id + " with the data: " + storage.get(forwardRead.id.toString))
      storage.get(forwardRead.id.toString)

      //Send back to Application
      val process = context.actorSelection(s"${forwardRead.appID.path}")
      process ! ReplyStoreAction("Read", myself, storage.get(forwardRead.id.toString).get)
    }
  }


  // - - - - - - - - - - - - - - - - - - - - - - -



  def findProcessForWrite(idProcess: Int, hashedProcesses: scala.collection.mutable.HashMap[Int, String], data: String, appID: ActorRef) ={

    log.debug("Process " + idProcess + " does NOT EXIST in the System")

    var previousN  = hashID_2551

    for (n <- hashedProcesses) {
      log.debug("hashProcess: " + n)
      log.debug("processID: " + n._1)
      if (n._1 > idProcess) {
        log.debug("Process " + n + "is too high")
        log.debug("Forwarding WRITE to: " + previousN + " with the following address: " + hashedProcesses.get(previousN).get)

        val process = context.actorSelection(s"${hashedProcesses.get(previousN).get}/user/globalView")
        process ! ForwardWrite(idProcess, data, appID)
        break
      }
      else
        previousN = n._1
    }

  }

  def findProcessForRead(idProcess: Int, hashedProcesses: scala.collection.mutable.HashMap[Int, String], appID: ActorRef) ={

    log.debug("Process " + idProcess + " does NOT EXIST in the System")

    var previousN  = hashID_2551

    for (n <- hashedProcesses) {
      log.debug("hashProcess: " + n)
      log.debug("processID: " + n._1)
      if (n._1 > idProcess) {
        log.debug("Process " + n + "is too high")
        log.debug("Forwarding READ to: " + previousN + " with the following address: " + hashedProcesses.get(previousN))

        val process = context.actorSelection(s"${hashedProcesses.get(previousN).get}/user/globalView")
        process ! ForwardRead(idProcess, appID)
        break
      }
      else
        previousN = n._1
    }

  }
}