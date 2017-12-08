package replication

import app._
import akka.actor.{ActorSystem, Props}
import app.Process.configureRemote

import scala.collection.mutable.TreeMap

class StateMachine (bucket : Int, setReplicas: TreeMap[Int, String]) {

  val config = configureRemote()
  val sys = ActorSystem("AkkaSystem", config)

  val proposer = sys.actorOf(Props[Proposer], "proposer")
  val accepter = sys.actorOf(Props[Accepter], "accepter")
  val learner = sys.actorOf(Props[Learner], "learner")

  var counter = 0
  var stateMachine = TreeMap[Int, Operation]()
  var replicas: TreeMap[Int, String] = setReplicas

  def write (index: Int, key: Int, data: String) = {

    stateMachine.put(index, Operation("write", key, data))
    println ("Writing on state machine bucket:" + bucket + " index:" + index + " with key-> " + key, " and data -> " + data)
    counter += 1
  }

  def getCounter(): Int ={
    counter
  }

  def initPaxos(op: Operation) = {
    proposer ! InitPaxos(op, replicas)
  }
}