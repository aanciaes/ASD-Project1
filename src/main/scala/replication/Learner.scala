package replication

import akka.actor.Actor
import app._

class Learner extends Actor{

  var decision = Operation("", 0, "")
  var na: Int = 0
  var va = Operation("", 0, "")


  override def receive = {

    case accept: Accept_OK_L => {
      if(accept.n > na){
        na = accept.n
        va = accept.op
        //aset.reset()
      }
      else if(accept.n < na){
          //Return
      }
      //aset.add(sender.path.address.toString)
      //if(aset is a majority quorum){
      //  decision = va
    }

  }
}

