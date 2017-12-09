package replication

import app._
import akka.actor.{ActorRef, ActorSystem, Props}
import app.Process.configureRemote

import scala.collection.mutable.TreeMap

class StateMachine (myself: String, bucket : Int, setReplicas: TreeMap[Int, String], sys: ActorSystem) {

  val proposer = sys.actorOf(Props[Proposer], "proposer" + bucket)
  val accepter = sys.actorOf(Props(new Accepter(myself)), "accepter" + bucket)
  val learner = sys.actorOf(Props(new Learner(myself)), "learner" + bucket)

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

  def initPaxos(op: Operation, myselfHashed: Int, appID: ActorRef, myself: String) = {
    proposer ! InitPaxos(op, myselfHashed, replicas, counter, appID, myself)
  }
}