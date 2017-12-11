package replication

import app._
import akka.actor.{ActorRef, ActorSystem, Props}
import app.Process.configureRemote

import scala.collection.mutable.TreeMap

class StateMachine (myself: String, bucket : Int, setReplicas: TreeMap[Int, String], sys: ActorSystem) {

  val proposer = sys.actorOf(Props(new Proposer (myself, bucket)), "proposer" + bucket)
  val accepter = sys.actorOf(Props(new Accepter(myself, bucket)), "accepter" + bucket)
  val learner = sys.actorOf(Props(new Learner(myself, bucket)), "learner" + bucket)

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

  def initPaxos(op: Operation, myselfHashed: Int, appID: ActorRef) = {
    proposer ! InitPaxos(op, myselfHashed, replicas, counter, appID)
  }

  def getSize : Int = {
    stateMachine.size
  }
}