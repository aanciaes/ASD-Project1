package replication

import akka.actor.Actor
import app._

class Learner extends Actor{

  var decision: String = ""
  var seqNumA: Int = 0
  var valueA: String = ""
  //var aset

  override def receive = {

    case acceptOk: Accept_OK => {
      if(acceptOk.seqNum > seqNumA){
        seqNumA = acceptOk.seqNum
        valueA = acceptOk.value

        //aset.reset()

      }
      else if(acceptOk.seqNum < seqNumA){

      }
      //aset.add(sender.path.address.toString)

      /*if( aset.size > allNodes.size/2){
        decision = valueA
      */
    }

  }

}
