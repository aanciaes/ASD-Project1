package replication

import scala.collection.mutable.TreeMap

class StateMachine (bucket : Int) {

  var counter = 0
  var stateMachine = TreeMap[Int, Operation]()


  def write (index: Int, key: Int, data: String) = {
    stateMachine.put(index, Operation("write", key, data))
  }

}

case class Operation (op : String, key : Int, data : String)