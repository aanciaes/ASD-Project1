/*
package layers

import akka.actor.Actor
import app._

class Storage extends Actor{

  var storage = scala.collection.mutable.HashMap[String, List[Byte]]()
  var defaultData: List[Byte] = List.empty


  override def receive = {

    case init: InitStorage => {
      myself = init.selfAddress
      globalView = globalView :+ myself

      val process = context.actorSelection(s"${init.contactNode}/user/globalView")
      process ! ShowGV
    }

    case write: Write => {





      storage.put(write.id, write.data)
    }

    case read: Read => {

      if(storage.exists(_ == read.id)) {
        storage.get(read.id)
      }
      else
        defaultData

    }
  }
}
*/
