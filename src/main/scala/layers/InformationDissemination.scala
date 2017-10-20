package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random


class InformationDissemination extends Actor {

  val log = Logger("scala.slick")

  var neigh : List [String] = List.empty
  var delivered : List [String] = List.empty
  var fanout = 5
  var LazyPush : Map [String, String] = Map.empty
  var myself: String = ""

  override def receive: Receive = {

    case init: InitGossip => {
      myself = init.selfAddress
      //neigh = getNeighbours(myself)
      for( p <- neigh){
        //LazyPush[p] = List.empty
      }
    }
  }

  def getNeighbours(self : String) ={
    //var listNeigh = self.activeView
  }

}