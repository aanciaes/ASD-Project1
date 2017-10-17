package layers

import akka.actor.Actor
import app.{ForwardJoin, InitMessage, Join, Notify}

import scala.util.Random

class PartialView extends Actor{

  var activeView : List [String] = List.empty
  var passiveView : List [String] = List.empty
  var myself : String = ""
  val ARWL = 3
  val PRWL = 3


  override def receive = {

    case message : InitMessage => {
      myself = message.selfAddress

      if(!message.contactNode.equals("")) {

        var contactNode = context.actorSelection(s"${message.contactNode}/user/partialView")
        contactNode ! Join(myself)

        addNodeActiveView(message.contactNode)
        println("receiving init")
        println(message.contactNode)
      }
    }


    case receiveJoin : Join => {

      addNodeActiveView(receiveJoin.newNodeAddress)
      println("receiving join")

      activeView.filter(node => !node.eq(receiveJoin.newNodeAddress)).foreach(node => {
        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(receiveJoin.newNodeAddress, ARWL, myself)
      })

      activeView.foreach(p => println(p.toString))
    }


    case receiveForward : ForwardJoin => {

      if(receiveForward.arwl == 0 || activeView.size == 1){
        println("received forward join 1")
        addNodeActiveView(receiveForward.newNode)
        val process = context.actorSelection(s"${receiveForward.newNode}/user/partialView")
        process ! Notify(myself)
      }
      else{
        println("received forward join 2")
        if(receiveForward.arwl == PRWL){
          addNodePassiveView(receiveForward.newNode)
        }

        var node : String = Random.shuffle(activeView.filter(node => !node.eq(receiveForward.senderAddress))).head

        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(node, ARWL - 1, myself)
      }
      activeView.foreach(p => println(p.toString))
    }

    case receiveNotify : Notify => {
      addNodeActiveView(receiveNotify.senderAddress)
      activeView.foreach(p => println(p.toString))
    }
  }

  def addNodeActiveView(node : String) = {
    activeView = activeView :+ node
  }

  def addNodePassiveView(node : String) = {
    passiveView = passiveView :+ node
  }
}
