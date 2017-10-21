package layers

import java.util.NoSuchElementException

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
        contactNode ! Join()

        addNodeActiveView(message.contactNode)
        log.debug("Init message - Sending join to: " + contactNode)

        val process = context.actorSelection(s"${myself}/user/informationDissemination")
        process ! BroadcastMessage(myself)

      }
    }


    case receiveJoin: Join => {
      log.debug("receiving join from: " + sender)
      addNodeActiveView(sender.path.address.toString)

      activeView.filter(node => !node.equals(sender.path.address.toString)).foreach(node => {
        val process = context.actorSelection(s"${node}/user/partialView")
        process ! ForwardJoin(sender.path.address.toString, ARWL, myself)
        log.debug("Forwarding join to: " + process)
      })
    }


    case receiveForward: ForwardJoin => {

      log.debug("Receiving FowardJoin from: " + sender + " with awrl: " + receiveForward.arwl)

      if (receiveForward.arwl == 0 || activeView.size == 1) {
        addAndNotify(receiveForward.newNode)
      }
      else {
        if (receiveForward.arwl == PRWL) {
          addNodePassiveView(receiveForward.newNode)
        }

        log.debug("receiving Foward join (Not added directly)")

        try {
          val node: String = Random.shuffle(activeView.filter(node =>
            !node.equals(sender.path.address.toString)
              && !(node.equals(receiveForward.contactNode)))).head

          log.debug("node shuffled: " + node)

          val process = context.actorSelection(s"${node}/user/partialView")
          process ! ForwardJoin(receiveForward.newNode, receiveForward.arwl - 1, receiveForward.contactNode)
          log.debug("FowardJoin with shuffle to: " + process)

        } catch {
          case ex: NoSuchElementException => {
            addAndNotify(receiveForward.newNode)
          }
        }
      }
    }

    case receiveNotify: Notify => {
      log.debug("Receiving notify from: " + sender.path.address.toString)
      addNodeActiveView(sender.path.address.toString)
    }

    case disconnectRandomNode: Disconnect => {
      log.debug ("Receiving disconnect")
      if (activeView.contains(disconnectRandomNode.nodeToDisconnect)) {
        activeView = activeView.filter(!_.equals(disconnectRandomNode.nodeToDisconnect))
        addNodePassiveView(disconnectRandomNode.nodeToDisconnect)
        log.debug("Disconnecting: " + disconnectRandomNode.nodeToDisconnect)
      }
    }

    case ShowPV => {
      sender ! ReplyShowView("Partial View", myself, activeView)
    }
  }

  def dropRandomNodeFromActiveView() = {
    val node: String = Random.shuffle(activeView).head

    activeView = activeView.filter(!_.equals(node))
    addNodePassiveView(node)
    log.debug("Disconnecting: " + node)

    log.debug("Sending disconnect message: " + node)
    val process2 = context.actorSelection(s"${node}/user/partialView")
    process2 ! Disconnect(myself)
  }

  def addNodeActiveView(node: String) = {
    if (!node.equals(myself) && !activeView.contains(node)) {

      if (activeView.size >= aViewSize) {
        dropRandomNodeFromActiveView()
      }

      activeView = activeView :+ node
    }
    log.info("Node added to activeView: " + node)
  }

  def addNodePassiveView(node: String) = {
    if (!node.equals(myself) && !activeView.contains(node) && !passiveView.contains(node)) {

      if (passiveView.size >= pViewSize) {
        val n: String = Random.shuffle(passiveView).head
        passiveView = passiveView.filter(!_.equals(n))
      }

      passiveView = passiveView :+ node
    }
    log.info("Node added to passive view: " + node)
  }

  def addAndNotify (newNode: String) = {
    addNodeActiveView(newNode)
    val process = context.actorSelection(s"${newNode}/user/partialView")
    if (!activeView.contains(newNode) || !((newNode).equals(myself)))
      process ! Notify()
    log.debug("Added Node directly - Notifying: " + process)
  }
}
