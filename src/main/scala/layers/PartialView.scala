package layers

import akka.actor.Actor
import app._

import scala.util.Random

class PartialView extends Actor{

  var activeView : List [String] = List.empty
  var passiveView : List [String] = List.empty
  var myself : String = ""
  val ARWL = 3
  val PRWL = 3
  val aViewSize = 3
  val pViewSize = 30


  override def receive = {

    case message : InitMessage => {
      myself = message.selfAddress

      if(!message.contactNode.equals("")) {

        val contactNode = context.actorSelection(s"${message.contactNode}/user/partialView")
        contactNode ! Join(myself)

        addNodeActiveView(message.contactNode)

      }
    }


    case receiveJoin : Join => {

      addNodeActiveView(receiveJoin.newNodeAddress)

      activeView.filter(node => !node.eq(receiveJoin.newNodeAddress)).foreach(node => {
        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(receiveJoin.newNodeAddress, ARWL, myself)
      })


      activeView.foreach(p => println("aView post Join: " + p.toString))
    }


    case receiveForward : ForwardJoin => {

      if(receiveForward.arwl == 0 || activeView.size == 1){

        addNodeActiveView(receiveForward.newNode)
        val process = context.actorSelection(s"${receiveForward.newNode}/user/partialView")
        process ! Notify(myself)
      }
      else{

        if(receiveForward.arwl == PRWL){
          addNodePassiveView(receiveForward.newNode)
        }

        val node : String = Random.shuffle(activeView.filter(node => !node.eq(receiveForward.senderAddress))).head

        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(node, receiveForward.arwl - 1, myself)
      }


      activeView.foreach(p => println("aView post ForwardJoin: " + p.toString))
    }

    case receiveNotify : Notify => {
      addNodeActiveView(receiveNotify.senderAddress)

    }

    case disconnectRandomNode : Disconnect => {

      if(activeView.contains(disconnectRandomNode.nodeToDisconnect)){
        activeView = activeView.filter(!_.equals(disconnectRandomNode.nodeToDisconnect))
        addNodePassiveView(disconnectRandomNode.nodeToDisconnect)
      }
    }
  }

  def dropRandomNodeFromActiveView() = {
    val node : String = Random.shuffle(activeView).head

    val process = context.actorSelection(s"${myself}/user/partialView")
    process ! Disconnect(node)

    activeView = activeView.filter(!_.equals(node))
    addNodePassiveView(node)

    println("disconnected node: " + node)
  }

  def addNodeActiveView(node : String) = {
    if(!node.equals(myself) && !activeView.contains(node)) {

      if(activeView.size >= aViewSize) {
        dropRandomNodeFromActiveView()
      }

      activeView = activeView :+ node
    }
  }

  def addNodePassiveView(node : String) = {
    if(!node.equals(myself) && !activeView.contains(node) && !passiveView.contains(node)){

      if(passiveView.size >= pViewSize) {
        val n : String = Random.shuffle(passiveView).head
        passiveView = passiveView.filter(!_.equals(n))
      }

      passiveView = passiveView :+ node
    }
  }
}
