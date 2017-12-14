package app

import scala.collection.mutable._

object Utils {

  val applicationAddress = "akka.tcp://akkaSystem@127.0.0.1:8080/user/appActor"

  def matchKeys(hashedDataId: Int, map: TreeMap[Int, _]): Int = {

    val hashID_2551: Int = math.abs(("akka.tcp://AkkaSystem@127.0.0.1:2551").reverse.hashCode % 1000) //474 in localhost
    var previousN = hashID_2551

    var count = 1

    for ((hash, _) <- map) {
      if (hash > hashedDataId) {

        if (hash == map.firstKey) {
          return map.lastKey
        } else {
          return previousN
        }
      } else {
        if (count == map.size) {
          return hash
        }
        count = count + 1
        previousN = hash
      }
    }
    return 0
  }
}

case class Operation (op : String, key : Int, data : String)