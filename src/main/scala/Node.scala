import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

/**
Search/Lookup:
find_successor(id)
find_predecessor(id)
closest_preceding_finger(id)


Node join:
join()
init_finger_table()
update_others()
update_finger_table()


Concurrent join:
join()
stabilize()
notify()
fix_fingers()

*/


case class GetNodeID()
case class GetSuccessor()
case class GetPredecessor()
case class SetPredecessor(node:ActorRef)
case class SetFinger(i: Int, value:ActorRef, key: Int )

case class ConcurrentJoin(randomNode: ActorRef)
case class Stabilize()
case class Notify(randomNode: ActorRef)
case class Fix_Fingers()
case class Find_Successor(id: Int)
case class Find_Predecessor(id: Int)
case class Closest_Preceding_Finger(id: Int)
case class Join(randomNode:ActorRef)
case class Update_Finger_Table(actorref: ActorRef, s:Int, i:Int)


class Node(nodeId:Int, KEY_LENGTH:Int, MAX_KEY:Int) extends Actor{

	implicit val timeout = Timeout(1 seconds)


	var fingerTable: ArrayBuffer[Finger] = new ArrayBuffer[Finger]
	
	//println(KEY_LENGTH)
	for (i <- 0 until KEY_LENGTH){
		
		var start = ((nodeId + Math.pow(2,(i))) % Math.pow(2, KEY_LENGTH)).toInt
		//println("for node "+ nodeId+ "----i: "+i+" ----start: "+ start)
		var end = ((nodeId + Math.pow(2,(i + 1))) % Math.pow(2, KEY_LENGTH)).toInt
		//println("for node "+ nodeId+ "----i: "+i+" ----end: "+ end)
		var fingerObject = new Finger(start, end, self, nodeId);
		
		fingerTable += fingerObject
		
	}
	var successorNode: ActorRef = fingerTable(0).getSuccessor()
	//println("for node "+ nodeId+ "----successorNode : " + successorNode)
	var predecessorNode: ActorRef= null

	def receive = {
		//
		case GetNodeID() => sender ! nodeId
		case GetSuccessor() => sender ! (fingerTable(0).successor, fingerTable(0).successorId)
		case GetPredecessor() => sender ! predecessorNode
		case SetPredecessor(node) => predecessorNode = node

		case SetFinger(i, value, key)=> //println(i+"----"+value+"------"+key)
								fingerTable(i).successor = value
								fingerTable(i).successorId = key
								//println("for node "+self+ "nodeid" + nodeId+ "--i: "+i+" ---start: "+ fingerTable(i).getStart()+ "--successorId: " + fingerTable(i).successorId+"--successor: "+ fingerTable(i).successor)

		//lookup
		case Find_Successor(id) => println("finding successor")
								val (succ, succId) = find_successor(nodeId, id)
								sender ! (succ,succId)
		case Find_Predecessor(id) => 
								val (predecessor,predecessorId) = find_predecessor(nodeId, id)
								sender ! (predecessor, predecessorId)


		case Closest_Preceding_Finger(id) => println("cpf")
								val cpfSuccessor =closest_preceding_finger(nodeId,id)
								sender ! cpfSuccessor

		//node join
		case Join(randomNode) =>
			//if (randomNode){
				init_finger_table(randomNode) 
				update_others(self, nodeId)
			/*}
			else{
				for (i<-0 until KEY_LENGTH){
					fingerTable(i).successor = self	
					fingerTable(i).successorId = nodeId				
				}
				predecessorNode = self

			}*/
		 
		case Update_Finger_Table(actorref, s, i) =>
								update_finger_table(self, nodeId, actorref, s, i)

		//concurrent join
		/*case ConcurrentJoin(randomNode) =>
						println ("hello")
						predecessorNode=null
						val future3= randomNode ? Find_Successor(nodeId)
						val n = Await.result(future3, timeout.duration).asInstanceOf[ActorRef]
						fingerTable(0).setSuccessor(n)
		case Stabilize() => stabilize()
		case Notify(randomNode) => notify(randomNode)
		case Fix_Fingers() => fix_fingers()
				*/
	}


		def find_successor(nodeId:Int, id: Int):(ActorRef, Int)={

			val (predecessor, predecessorId) = find_predecessor(nodeId ,id)
			
			var successor = (self, nodeId)
			if (predecessor !=self){
				val future4 = predecessor ? GetSuccessor()	
				
				val successor = Await.result(future4, timeout.duration).asInstanceOf[(ActorRef, Int)]
				return (successor._1, successor._2)
			}
			return (successor._1, successor._2)

		}
		def find_predecessor(nodeId:Int, id:Int):(ActorRef, Int)={
			println("FP: Node "+self+ "---nodeid: " + nodeId+ "--successorId: "+ fingerTable(0).successorId +"---id: "+id)

			if (inRange_rightIncluded(id, nodeId, fingerTable(0).successorId)){
				println("FP: At nodeId: "+nodeId+ " --returning nodeId: "+ nodeId)
				return (self, nodeId)
			}
			else{
				var cpfSuccessor= closest_preceding_finger(nodeId, id)
				//if (cpfSuccessor != self){
					var future5 = cpfSuccessor ? Find_Predecessor(id)
					var predecessor = Await.result(future5, timeout.duration).asInstanceOf[(ActorRef, Int)]
					println("FP2: At nodeId: "+nodeId+ "--returning nodeId: "+ predecessor._2)
					return (predecessor._1, predecessor._2)
				//}
			}



/*
			var temp = self
			var tempId = nodeId			
			
			//var future5 = temp ? GetSuccessor()
			var tempSuccessor = fingerTable(0).successor
			//var  future6= tempSuccessor ? GetNodeID
			var tempSuccessorId = fingerTable(0).successorId
			println("In find_predecessor, at nodeId:" + nodeId+ "--ActorRef: "+self+"--tempSuccessor: "+ tempSuccessor+"---tempSuccessorId: "+ tempSuccessorId+"---id: "+id)
			println("--"+ tempSuccessor+"---"+ tempSuccessorId+"---"+id)
			if(inRange_rightIncluded(id, tempId, tempSuccessorId)==false){
				var (temp,tempId) = closest_preceding_finger(nodeId, id)
				 
			}
			println("**predecessor** "+temp)
			/*/*while ((inRange_rightIncluded(id, tempId, tempSuccessorId )==false) && (temp != self)){
				val future5  : Future[Any]= temp ? GetSuccessor() 
				val successr = Await.result(future5, timeout.duration).asInstanceOf[(ActorRef, Int)]
				var (temp, tempId) = (successr._1, successr._2)
			
			
				
			}*/*/



			return (temp, tempId) 
*/
		}


		def closest_preceding_finger(nodeId:Int, id:Int): ActorRef={
			
			for (i<- (KEY_LENGTH-1) to 0 by -1){
				println("CPF: Node "+self+ "--nodeid: " + nodeId+ "--i: "+i+" ---start: "+ fingerTable(i).getStart()+ "--successorId: " + fingerTable(i).successorId+"--successor: "+ fingerTable(i).successor)
				
				var closestNodeId = fingerTable(i).successorId
				if (inRange(closestNodeId, nodeId, id)){
					return fingerTable(i).successor
				}
			}
			return self

		}




		def init_finger_table(randomNode:ActorRef){
			println("ift0--check: " +fingerTable(0).successor)
			var future11 = randomNode ? Find_Successor(fingerTable(0).getStart())
			var temp1 = Await.result(future11, timeout.duration).asInstanceOf[(ActorRef, Int)]
			fingerTable(0).successor= temp1._1
			fingerTable(0).successorId = temp1._2
			println("ift1---check: "+ fingerTable(0).successor)
			var future12 = fingerTable(0).successor ? GetPredecessor()
			predecessorNode = Await.result(future12, timeout.duration).asInstanceOf[ActorRef]
			println("ift2")
			temp1._1 ! SetPredecessor(self)
			println("ift3")
			for (i<-0 until (KEY_LENGTH-2)){
				if (inRange_leftIncluded(fingerTable(i+1).successorId , nodeId, fingerTable(i).successorId)){
					println("ift4")
					fingerTable(i+1).successor=fingerTable(i).successor
					fingerTable(i+1).successorId=fingerTable(i).successorId
				}
				else{
					println("ift5")
					var future13 = randomNode ? Find_Successor(fingerTable(i+1).getStart())
					var temp2 = Await.result(future13, timeout.duration).asInstanceOf[(ActorRef, Int)]
					fingerTable(i+1).successor= temp2._1
					fingerTable(i+1).successorId= temp2._2
				}
			}

		}

		def update_others(acref:ActorRef, nodeId: Int){
			for (i<- 0 until KEY_LENGTH){
				var (pred, predId) = find_predecessor(nodeId, (nodeId - Math.pow(2, (i))).toInt)
				pred ! Update_Finger_Table(acref, nodeId, i)
			}

		}

		def update_finger_table(acref: ActorRef, nodeId:Int,actorref:ActorRef, s:Int,i:Int){
			if (inRange_leftIncluded(s, nodeId, fingerTable(i).successorId)){
				fingerTable(i).successor = actorref
				fingerTable(i).successorId = s		
				var p = predecessorNode
				//var x = p ? GetNodeID()
				println("uft-- nodeId: " +nodeId)
				p ! Update_Finger_Table(actorref, s,i)
			}
		}

		/*
		def stabilize(){
			var future17 = successorNode ? GetPredecessor
			var temp = Await.result(future17, timeout.duration).asInstanceOf[ActorRef]
			var future14 = temp ? GetNodeID
			var tempId = Await.result(future14, timeout.duration).asInstanceOf[Int]
			var future15 = successorNode ? GetNodeID
			var successorNodeId = Await.result(future15, timeout.duration).asInstanceOf[Int]
			if (inRange(tempId, nodeId, successorNodeId)){
				fingerTable(0).setSuccessor(temp)
			}
			successorNode ! notify(self)
		}

		def notify(randomNode:ActorRef){
			var future18= randomNode ? GetNodeID()	
			var y = Await.result(future18, timeout.duration).asInstanceOf[Int]
			val future19 = predecessorNode ? GetNodeID()
			var predecessorNodeId = Await.result(future19, timeout.duration).asInstanceOf[Int]
			if ((predecessorNode == null) || (inRange(y, predecessorNodeId, nodeId ))){
				predecessorNode = randomNode
			}
		}

		def fix_fingers(){
			for (i<- 0 until KEY_LENGTH){
				val z = fingerTable(i).getStart()
				fingerTable(i).setSuccessor(find_successor(nodeId, z))
			}

		}*/

		def inRange(id:Int, left:Int, right:Int):Boolean= {
			if (right>left){
				if ((id>left) && (id < right)){
					return true
				}
				else {return false}
			}
			else{
				if (((id>left)&& (id <= MAX_KEY)) || ((id >=0) && (id < right))){
					return true
				}
				else {return false}
			}

		}

		def inRange_rightIncluded(id:Int, left:Int, right:Int):Boolean= {
			if (right>left){
				if ((id>left) && (id <=right)){
					return true
				}
				else {return false}
			}
			else{
				if (((id>left) && (id <=MAX_KEY)) || ((id >=0) && (id <=right))){
					return true
				}
				else {return false}
			}

		}

		def inRange_leftIncluded(id:Int, left:Int, right:Int):Boolean= {
			if (right>left){
				if ((id >= left) && (id < right)){
					return true
				}
				else {return false}
			}
			else{
				if (((id >= left) && (id <=MAX_KEY)) || ((id >=0) && (id < right))){
					return true
				}
				else {return false}
			}

		}
		}

class Finger(start:Int, end:Int, acref: ActorRef, nodeId: Int) {
	implicit val timeout = Timeout(1 seconds)
	//var start = start
	//var end = end
	var range = Set(start, end)


	var successor : ActorRef = acref
	//println("finger created")
	
	var successorId = nodeId
	//var predecessor
	//var predecessorNodeId = predecessorNode ? GetNodeID()
	//var tuple = (1, "test")
	//tuple = tuple.copy(_2 = "new")
	//var Successor = (successor, successorId)
	//println(Successor._1 +"----"+ Successor._2)

	def getStart() : Int={
		return range.head
	}
	//def setStart(start:String) {
	//	range.head = start
	//}
	def getSuccessor(): ActorRef = {
		return successor
	}
	def setSuccessor(node: ActorRef) {
		successor = node
	}
}


