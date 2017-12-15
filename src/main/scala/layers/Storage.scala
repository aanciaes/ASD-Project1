package layers

import akka.actor.{Actor}
import app._
import replication.StateMachine

import scala.collection.mutable._

class Storage extends Actor {

  var storage = HashMap[String, String]()
  var pending = Queue[Operation]()
  var replicasFront = TreeMap[Int, String]()
  var replicasBack = TreeMap[Int, String]()
  var stateMachines = TreeMap[Int, StateMachine]()
  var myself: String = ""
  var myselfHashed: Int = 0


  override def receive = {

    case init: InitReplication => {
      println ("Init replication on storage")
      myself = init.selfAddress
      myselfHashed = init.myselfHashed

      val newProcess = init.node
      val newProcessHashed = init.nodeHashed

      println("New process id: " + newProcess)
      replicasFront = init.replicasFront
      replicasBack = init.replicasBack

      for (st <- stateMachines) {
        if (!replicasFront.contains(st._1)) {
          transferData(st, newProcessHashed, newProcess)
        }
      }

      for (r <- replicasFront) {
        if (!stateMachines.contains(r._1)) {
          stateMachines.put(r._1, new StateMachine(myself, r._1, replicasBack, context.system))
        }
      }

      if (myself.equals(newProcess)) {
        for (r <- replicasFront) {
          if (r._1 != myselfHashed) {
            println("asking state machine to: " + r._2)
            val process = context.actorSelection(s"${r._2}/user/storage")
            process ! GetStateMachine
          }
        }
      }

      for (st <- stateMachines)
        st._2.setNewReplicas(replicasBack)

      println("I Have replicas of: ")
      for (r <- replicasFront) {
        println(r)
      }
      println("- - - - - - - - - - - -")

      println("The processes have replicas of me: ")
      for (r <- replicasBack) {
        println(r)
      }
      println("- - - - - - - - - - - -")
    }

    case write: ForwardWrite => {
      println("Forward Write received")
      println("myself hashed: " + math.abs(myself.reverse.hashCode % 1000) + " STORED ID: " + write.hashedDataId + " with the data: " + write.data.toString)

      val op = Operation("write", write.hashedDataId, write.data)

      pending.enqueue(op)

      val leader = stateMachines.get(myselfHashed).get
      leader.initPaxos(op, myselfHashed)
    }

    case read: ForwardRead => {
      println("---ForwardRead---")
      if (storage.contains(read.hashedDataId.toString)) {
        println("Process hashID: " + math.abs(myself.reverse.hashCode % 1000) + " Got stored HashID: " + read.hashedDataId + " with the data: " + storage.get(read.hashedDataId.toString).get.toString)
        storage.get(read.hashedDataId.toString)

        println("Read completed! Data: " + storage.get(read.hashedDataId.toString))

        //Send back to Application
        val process = context.actorSelection(s"${Utils.applicationAddress}")
        process ! ReplyStoreAction("Read", myself, storage.get(read.hashedDataId.toString).get)
      }
      else {
        println("The read with the id: " + read.hashedDataId + " does not exist in the System.")

        //Send back to Application
        val process = context.actorSelection(s"${Utils.applicationAddress}")
        process ! ReplyStoreAction("Read", myself, "Read not found in the System!")
      }
    }


    case executeOp: ExecuteOP => {
      println("Write Op received")
      if (myselfHashed == executeOp.leaderHash) {
        try {
          if (executeOp.opType.equals("delete")) {
            storage -= executeOp.hashDataId.toString
          }
          if (executeOp.opType.equals("write")) {
            storage.put(executeOp.hashDataId.toString, executeOp.data)
          }
          pending.dequeue()
        } catch {
          case ioe: NoSuchElementException => //
          case e: Exception => //
        }
      }

      var hashToMatch = executeOp.hashDataId
      if (executeOp.opType.equals("delete")) {
        //On delete writes on state machine of leader and not on dataId
        //example: given a node [688, 474[ and a new node with hash 300, when transfering data will delete data
        //with id 450 in bucket 688 and not on new node 300
        hashToMatch = executeOp.leaderHash
      }
      val stateHash = Utils.matchKeys(hashToMatch, stateMachines)
      stateMachines.get(stateHash).get.writeOp(executeOp.opType, executeOp.opCounter, executeOp.hashDataId, executeOp.data)
    }

    case ShowBuckets => {
      var toPrint = ""

      for ((hash, st) <- stateMachines) {
        toPrint += "State Machine of bucket: " + hash + "\n"
        for ((op, value) <- st.stateMachine)
          toPrint += " - Operation number: " + op + " Operation: " + value + "\n"
      }

      sender ! ReplyShowBuckets(toPrint)
    }

    case ShowReplicas => {
      var prtFrontRep = ""
      var prtBackRep = ""

      for ((hash, address) <- replicasFront) {
        prtFrontRep += "My Front Replicas: " + hash + "\n"
      }

      for ((hash, address) <- replicasBack) {
        prtBackRep += "Back Replicas: " + hash + "\n"
      }

      sender ! ReplyShowReplicas(prtFrontRep, prtBackRep)
    }

    case transferData: TransferData => {
      println("receiving data transfer")
      for (op <- transferData.ops) {
        val leader = stateMachines.get(myselfHashed).get
        leader.initPaxos(op, myselfHashed)
      }
    }

    case GetStateMachine => {
      val myStateMachine = stateMachines.get(myselfHashed).get
      sender ! ReplyGetStateMachine(myselfHashed, myStateMachine.getCounter(), myStateMachine.getOperations())
    }

    case message: ReplyGetStateMachine => {
      val st = stateMachines.get(message.bucket).get
      st.setCounter(message.counter)
      st.setOperations(message.ops)
    }

    case updateReplicas: UpdateReplicas => {
      var aux: StateMachine = null

      if(stateMachines.contains(updateReplicas.nodeRemoved)){
        //Saving state machine before deletion
        aux = stateMachines.get(updateReplicas.nodeRemoved).get
      }


      for (st <- stateMachines){
        if(!updateReplicas.newFrontReplicas.contains(st._1)){
          stateMachines.get(st._1).get.stopActors()
          stateMachines -= st._1
        }
      }

      for (r <- updateReplicas.newFrontReplicas){
        if(!stateMachines.contains(r._1)){
          stateMachines.put(r._1, new StateMachine(myself, r._1, updateReplicas.newBackReplicas, context.system))
        }
      }
      for (st <- stateMachines)
        st._2.setNewReplicas(updateReplicas.newBackReplicas)

      if(aux != null && myselfHashed==updateReplicas.previousNode)
        executeStateMachine(updateReplicas.nodeRemoved, aux)

      replicasFront=updateReplicas.newFrontReplicas
      replicasBack=updateReplicas.newBackReplicas
    }
  }

  def transferData(tuple: (Int, StateMachine), newProcessHashed: Int, newProcessAddr: String) = {
    println("Transfer data -> bucket: " + tuple._1 + "state machine: " + tuple._2)

    //Remove state machine from this.process
    stateMachines.get(tuple._1).get.stopActors()
    stateMachines -= tuple._1

    var opList: List[Operation] = List.empty
    //Remove all data that is now handled by the new process
    for ((key, value) <- storage) {
      if (key.toInt >= newProcessHashed) {
        stateMachines.get(myselfHashed).get.initPaxos(new Operation("delete", key.toInt, value), myselfHashed)

        opList = opList :+ (Operation("write", key.toInt, value))
      }
    }

    if (opList.size > 0) {
      println("transfering data")
      val process = context.actorSelection(s"${newProcessAddr}/user/storage")
      process ! TransferData(opList)
    }
  }

  def executeStateMachine(removeReplicaHash: Int, stateMachine: StateMachine) = {
    val stateMachineOPs = stateMachine.getOperations()

    for (op <- stateMachineOPs) {
      val opToExecute = op._2
      val leader = stateMachines.get(myselfHashed).get

      leader.initPaxos(opToExecute, myselfHashed)
    }
  }

}
