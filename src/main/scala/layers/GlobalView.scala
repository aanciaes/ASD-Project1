package layers

import akka.actor.{Actor, ActorRef}
import app._
import com.typesafe.scalalogging.Logger
import scala.collection.mutable._


class GlobalView extends Actor {

  val log = Logger("phase1")
  val log2 = Logger("phase2")
  val N_REPLICAS = 3
  val hashID_2551: Int = math.abs(("akka.tcp://AkkaSystem@127.0.0.1:2551").reverse.hashCode % 1000) //474 in localhost

  var globalView: List[String] = List.empty
  var myself: String = ""
  var myHashedId: Int = 0
  var hashedProcesses = TreeMap[Int, String]()


  override def receive = {

    case init: InitGlobView => {
      myself = init.selfAddress
      globalView = globalView :+ myself

      myHashedId = math.abs(init.selfAddress.reverse.hashCode % 1000)
      println("Unique Identifier: " + myHashedId)

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

      if(globalView.size >= N_REPLICAS){
        for(p <- globalView){
          val process = context.actorSelection(s"${p}/user/globalView")
          process ! InitReplication
        }
      }
    }


    case InitReplication => {
      updateHashedProcesses(globalView)

      val replicas : TreeMap[Int, String] = findReplicas()

      val process = context.actorSelection(s"${myself}/user/storage")
      process ! InitReplication(replicas, myself, myHashedId)

    }





    // - - - - - - - - STORAGE - - - - - - - - //

    case write: Write => {

      val hashedDataId = math.abs(write.dataId.reverse.hashCode % 1000)
      log2.debug("Received write with key: " + hashedDataId)

      val processId = FindProcess.matchKeys(hashedDataId, hashedProcesses)


      val process = context.actorSelection(s"${hashedProcesses.get(processId).get}/user/storage")
      process ! ForwardWrite(hashedDataId, write.data, sender)
    }


    case read: Read => {

      print("Received Read from application")
      val hashedDataId = math.abs(read.dataId.reverse.hashCode % 1000)

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

  def updateHashedProcesses(globalView: List[String]) = {
    for (n <- globalView) {
      hashedProcesses.put(math.abs((n.reverse.hashCode % 1000)), n)
    }
  }

  def findReplicas() = {

    val replicas = TreeMap[Int, String]()

    var count = 0
    var it = hashedProcesses.iterator
    var break = false
    while(true && !break){
      var p = it.next()

      if(p._1 == myHashedId || count != 0){
        replicas.put(p._1, p._2)
        count = count + 1
      }

      if(count == 3){
        break = true
      }

      if(!it.hasNext){
        it = hashedProcesses.iterator
      }
    }
    replicas
  }


  def findProcessForRead(hashedDataId: Int, hashedProcesses: TreeMap[Int, String], appID: ActorRef) = {
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