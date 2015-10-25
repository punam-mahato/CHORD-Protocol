import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}

import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.collection.mutable.ArrayBuffer
import scala.math
import com.typesafe.config.ConfigFactory



object Chord {
	
	def main(args: Array[String]){
		

		if (args.length==2){
			val mastersystem = ActorSystem("MasterActorSystem", ConfigFactory.load().getConfig("masterSystem"))
			val numNodes = args(0).toInt
			val numRequests = args(1).toInt
			val master_actor = mastersystem.actorOf(Props(classOf[Application],mastersystem, numNodes, numRequests), name = "MasterActor")
			//val acref =  context.actorOf(Props(classOf[Node], nodeId, KEY_LENGTH, MAX_KEY))
			master_actor ! Start()	

					
		}

		else {
			println("Provide 2 arguments: numNodes, numRequests")
		}
	}
}