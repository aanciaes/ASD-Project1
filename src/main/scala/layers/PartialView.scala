package layers

import akka.actor.Actor
import app._
import com.typesafe.scalalogging.Logger

import scala.util.Random

class PartialView extends Actor {

  val log = Logger("scala.slick")

  var activeView: List[String] = List.empty
  var passiveView: List[String] = List.empty
  var myself: String = ""
  val ARWL = 3
  val PRWL = 3
  val aViewSize = 3
  val pViewSize = 30


  override def receive = {

    case message: InitMessage => {
      myself = message.selfAddress

      if (!message.contactNode.equals("")) {

        val contactNode = context.actorSelection(s"${message.contactNode}/user/partialView")
        contactNode ! Join(myself)

        addNodeActiveView(message.contactNode)
        println("Init message - Sending join to: " + contactNode)
      }
    }


    case receiveJoin: Join => {

      println("receiving join from: " + sender)
      addNodeActiveView(receiveJoin.newNodeAddress)

      activeView.filter(node => !node.equals(receiveJoin.newNodeAddress)).foreach(node => {
        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(receiveJoin.newNodeAddress, ARWL)
        println("Fowarding join to: " + process)
      })
    }


    case receiveForward: ForwardJoin => {

      println("Receiving FowardJoin from: " + sender + " with awrl: " + receiveForward.arwl)

      if (receiveForward.arwl == 0 || activeView.size == 1) {

        addNodeActiveView(receiveForward.newNode)
        val process = context.actorSelection(s"${receiveForward.newNode}/user/partialView")
        if (!activeView.contains(receiveForward.newNode) || !((receiveForward.newNode).equals(myself)))
          process ! Notify()
        println("Added Node directly - Notifying: " + process)
      }
      else {

        if (receiveForward.arwl == PRWL) {
          addNodePassiveView(receiveForward.newNode)
        }
        println("reciving Foward join (Not added directly)")
        val node: String = Random.shuffle(activeView.filter(node => !node.equals(sender.path.address.toString))).head
        println("node shuffled: " + node)
        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(receiveForward.newNode, receiveForward.arwl - 1)
        println("FowardJoin with shuffle to: " + process)
      }
    }

    case receiveNotify: Notify => {
      println("Reciving notify from: " + sender.path.address.toString)
      addNodeActiveView(sender.path.address.toString)
    }

    case disconnectRandomNode: Disconnect => {

      if (activeView.contains(disconnectRandomNode.nodeToDisconnect)) {
        activeView = activeView.filter(!_.equals(disconnectRandomNode.nodeToDisconnect))
        addNodePassiveView(disconnectRandomNode.nodeToDisconnect)
      }
    }

    case ShowPV => {
      sender ! ReplyAppRequest("Partial View", myself, activeView)
    }
  }

  def dropRandomNodeFromActiveView() = {
    val node: String = Random.shuffle(activeView).head

    val process = context.actorSelection(s"${myself}/user/partialView")
    process ! Disconnect(node)

    activeView = activeView.filter(!_.equals(node))
    addNodePassiveView(node)

    println("disconnected node: " + node)
  }

  def addNodeActiveView(node: String) = {
    if (!node.equals(myself) && !activeView.contains(node)) {

      if (activeView.size >= aViewSize) {
        dropRandomNodeFromActiveView()
      }

      activeView = activeView :+ node
    }
  }

  def addNodePassiveView(node: String) = {
    if (!node.equals(myself) && !activeView.contains(node) && !passiveView.contains(node)) {

      if (passiveView.size >= pViewSize) {
        val n: String = Random.shuffle(passiveView).head
        passiveView = passiveView.filter(!_.equals(n))
      }

      passiveView = passiveView :+ node
    }
  }
}
