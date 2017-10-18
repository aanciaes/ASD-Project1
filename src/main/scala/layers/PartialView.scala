package layers

import akka.actor.Actor
import app._

import scala.util.Random

class PartialView extends Actor{

  var activeView : List [String] = List.empty
  var passiveView : List [String] = List.empty
  var myself : String = ""
  val ARWL = 5
  val PRWL = 5
  val aViewSize = 5
  val pViewSize = 30


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

        addNodeActiveView(receiveForward.newNode)
        val process = context.actorSelection(s"${receiveForward.newNode}/user/partialView")
        process ! Notify(myself)
      }
      else{

        if(receiveForward.arwl == PRWL){
          addNodePassiveView(receiveForward.newNode)
        }

        var node : String = Random.shuffle(activeView.filter(node => !node.eq(receiveForward.senderAddress))).head

        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(node, ARWL - 1, myself)
      }
      activeView.foreach(p => println("1: " + p.toString))
    }

    case receiveNotify : Notify => {
      addNodeActiveView(receiveNotify.senderAddress)
      activeView.foreach(p => println("2: " + p.toString))
    }

    case disconnectRandomNode : Disconnect => {
      if(activeView.contains(disconnectRandomNode.nodeToDisconnect)){

        activeView = activeView.filter(!_.equals(disconnectRandomNode.nodeToDisconnect))
        addNodePassiveView(disconnectRandomNode.nodeToDisconnect)
      }
    }
  }

  def dropRandomNodeFromActiveView() = {
    var node : String = Random.shuffle(activeView).head
    val process = context.actorSelection(s"${myself}/user/partialView")
    process ! Disconnect(node)
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
        var n : String = Random.shuffle(passiveView).head
        passiveView = passiveView.filter(!_.equals(n))
      }
      passiveView = passiveView :+ node
    }
  }
}
