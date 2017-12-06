package replication

import scala.collection.mutable.TreeMap

class StateMachine (bucket : Int) {

  var counter = 0
  var stateMachine = TreeMap[Int, Operation]()
  stateMachine.put(1, Operation("", 1, ""))

}



case class Operation (op : String, key : Int, data : String)