package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger
import scala.util.control.Breaks._



class GlobalView extends Actor {

  val log = Logger("scala.slick")

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
      log.debug("Global view receive Broacast Message from: " + sender.path.address.toString)
      message.messageType match {
        case "add" => {
          if (!message.node.equals(myself))
            log.debug("adding: " + message.node + " to global view")
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
      log.debug("Received write with key: " + (write.id.hashCode%1000))
      log.debug("With the data: " + write.data)

      var idWrite = write.id.hashCode%1000
      var hashedProcesses = scala.collection.mutable.HashMap[Int, String]()
      for(n<-globalView){
        hashedProcesses.put((n.hashCode%1000), n)
      }
      hashedProcesses.toSeq.sortBy(_._1)

      if(hashedProcesses.contains(idWrite)) {
        storage.put((write.id.hashCode % 1000).toString, write.data)
        log.debug("Process id " + write.id.hashCode % 1000 + "EXISTS and ADDED the data " + write.data)
      }
      else
        findProcessForWrite(write.id.hashCode%1000, hashedProcesses, write.data)
    }

    case read: Read => {

      if (storage.exists(_ == read.id)) {
        storage.get((read.id.hashCode%1000).toString)
      }
      else
        defaultData

    }

    case forwardWrite: ForwardWrite => {
      log.debug("Process: " + forwardWrite.id.hashCode()%1000 + " STORED the data: " + forwardWrite.data)
      storage.put((forwardWrite.id.hashCode()%1000).toString, forwardWrite.data)
    }
  }

  def findProcessForWrite(idProcess: Int, hashedProcesses: scala.collection.mutable.HashMap[Int, String], data: String) ={

    log.debug("Process " + idProcess + " does NOT EXIST in the System")

    var previousN  = 0


      for (n <- hashedProcesses) {
        if (n._1 > idProcess) {
          log.debug("Process " + n + "is too high")
          log.debug("Forwarding WRITE to: " + previousN)

          val process = context.actorSelection(s"${hashedProcesses.get(previousN)}/user/globalView")
          process ! ForwardWrite(idProcess, data)
          break
        }
        previousN = n._1
      }

  }
}