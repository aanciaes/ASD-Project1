package replication

import scala.collection.mutable.TreeMap

class StateMachine (bucket : Int, setReplicas: TreeMap[Int, String]) {

  var counter = 0
  var stateMachine = TreeMap[Int, Operation]()
  var replicas: TreeMap[Int, String] = setReplicas

  def write (index: Int, key: Int, data: String) = {

    stateMachine.put(index, Operation("write", key, data))

  }

  def getCounter(): Int ={
    counter
  }

  def setCounter(newCounter: Int) = {
    counter = newCounter
  }

}

case class Operation (op : String, key : Int, data : String)