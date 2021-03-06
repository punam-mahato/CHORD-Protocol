import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import java.net.InetAddress
import java.net.URL
import java.security.MessageDigest
import scala.collection.mutable.ArrayBuffer
import scala.math
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.TreeMap
import util.control.Breaks._



case class Start()


class Application(acsys: ActorSystem, nrNodes: Int, numRequests: Int) extends Actor{
	var	nodesList: ArrayBuffer[ActorRef] = new  ArrayBuffer[ActorRef]
	var ID_Node_map = new TreeMap[Int, ActorRef]
	var sortedKeyArray: ArrayBuffer[Int] = new ArrayBuffer[Int]
	//val num = (scala.math.ceil(scala.math.log(numNodes) / scala.math.log(2))).toInt
	val KEY_LENGTH :Int = 16
	val MAX_KEY: Int = (Math.pow(2, KEY_LENGTH) -1).toInt


	val startTime: Long = System.currentTimeMillis();
	implicit val timeout = Timeout(1 seconds)

	val host: String = InetAddress.getLocalHost().getHostAddress();
	val port:Int = 3000;
	var numNodes:Int =nrNodes

	def receive ={
		case Start() =>
	//create chord nodes
			for (i<-0 until numNodes) {
						var url: URL = new URL("http", host, port + i, "")
						val sha = MessageDigest.getInstance("SHA-1")
						val hash: String = sha.digest((url.toString).getBytes)
		              		.foldLeft("")((s: String, b: Byte) => s +
		                	Character.forDigit((b & 0xf0) >> 4, 16) +
		                	Character.forDigit(b & 0x0f, 16))
		                val trimmedHash= hash.substring(0,4)
		                val nodeId = Integer.parseInt(trimmedHash, 16)
						if(ID_Node_map.contains(nodeId) == false ){
							val acref =  context.actorOf(Props(classOf[Node], nodeId, KEY_LENGTH, MAX_KEY))
							ID_Node_map += (nodeId -> acref)
							nodesList += acref
						}
						else{
							println("Duplicated nodeId")
							numNodes = numNodes -1							
						}
						
						

							
						}	
			//println (nodesList)
			//println(ID_Node_map)			
			println("Nodes created.")

			//concurrent join and stabilize()
			for ((k,v) <- ID_Node_map) {
				sortedKeyArray += k
			}

			println (sortedKeyArray)

			Thread.sleep(3000)


			


			for (i<-0 until numNodes){
				var j= i
				if (i==0){j= numNodes-1}
				else{j = i-1}

				
				var node: ActorRef= ID_Node_map.apply(sortedKeyArray(i))
				var pred: ActorRef = ID_Node_map.apply(sortedKeyArray(j))
				var predId = sortedKeyArray(j)
				//println("pred : "+ node +"----"+ pred)
				node ! SetPredecessor(pred, predId)

			}
			

			for (i<-0 until numNodes){
				var nodeId = sortedKeyArray(i)
				var node = ID_Node_map.apply(nodeId)
				for (j<-0 until KEY_LENGTH){
					var start = ((nodeId + Math.pow(2,(j))) % Math.pow(2, KEY_LENGTH)).toInt 
					
					var k:Int =0
					if (start > sortedKeyArray(numNodes-1)){
						k = 0
						
					}
					else {
						
						breakable {
						for (l<-0 until numNodes){
							
							if (sortedKeyArray(l) >= start){
								k=l
								break
							}
						}}
					}
					//println("k: "+ k)
					var key = sortedKeyArray(k)
					var value = ID_Node_map.apply(key)	
					//println(node +"---nodeId : "+ nodeId + "--start: "+start +"----i: "+j +"--successor: " + value+ "---successorId: "+ key )
					node ! SetFinger(j, value, key)			
				}

			}



			println("\nChord ring is established.")
			println("-------------------------------------------------------")


			val scanner = new java.util.Scanner(System.in)


			print("\n\nEnter a key that you want to lookup from 0 to "+ MAX_KEY +": ")
			var hopcount:Int = 0
			val input = scanner.nextLine()
			//println(input.toInt)
			val r = (scala.util.Random).nextInt(nodesList.length)
			val futr = nodesList(r) ? Find_Successor(input.toInt, hopcount)
			val result = Await.result(futr, timeout.duration).asInstanceOf[(ActorRef, Int, Int)]
			
			println("\n\nThe node responsible for the given key is: "+ result._1+ " whose nodeId is: "+result._2)
			println("Number of hops: "+ result._3)




			//NodeJoin
			println("---------------------------------------------------------")
			val scanner2 = new java.util.Scanner(System.in)
			print("\n\n\nEnter ip address of the newly joining node: ")


			val input2 = scanner2.nextLine()
			//println(input2.toInt)
			
			//var url: URL = new URL("http", host, port + i, "")
			val sha = MessageDigest.getInstance("SHA-1")
			val hash: String = sha.digest((input2.toString).getBytes)
          		.foldLeft("")((s: String, b: Byte) => s +
            	Character.forDigit((b & 0xf0) >> 4, 16) +
            	Character.forDigit(b & 0x0f, 16))
            val trimmedHash= hash.substring(0,4)
            var newnodeId = Integer.parseInt(trimmedHash, 16)
            println("\nNew node's Chord id: "+ newnodeId)
			val newacref =  context.actorOf(Props(classOf[Node], newnodeId, KEY_LENGTH, MAX_KEY))
			val z = (scala.util.Random).nextInt(nodesList.length)
			newacref ! Join(nodesList(z))
			nodesList += newacref
			
			if (ID_Node_map.contains(newnodeId) == true ) {
					println("Duplicated nodeId")
				}	
			else {
				ID_Node_map += (newnodeId -> newacref)}

						


			Thread.sleep(2000)

			println("\n\n")
			println("---------------------------------------------------------")
			println("\n\n------------------CHORD IN ACTION--------------------\n\n")

			println("---------------------------------------------------------")
			val scanner3 = new java.util.Scanner(System.in)
			print("\n\n\nDo you want to see Chord in action? ")


			val input3 = scanner2.nextLine()			
			if (input3.toString == "yes"){
				println("Performing "+numRequests+ " lookups on each node: ")
				var TotalHops =0
				//var futureArray:ArrayBuffer[Future[Any]] = new ArrayBuffer[Future[Any]]
				//var resultArray: ArrayBuffer[(ActorRef, Int, Int)] =new ArrayBuffer[(ActorRef, Int, Int)]
				for (i <- 0 until numRequests){
					for (j<-0 until nodesList.length){
	
						var hopcount:Int = 0
						var key = (scala.util.Random).nextInt(MAX_KEY)
						println("\n\nThe key to lookup is: "+key)
						println(nodesList(j))
						var future10 = nodesList(j) ? Find_Successor(key, hopcount)
						var result= Await.result(future10, timeout.duration).asInstanceOf[(ActorRef, Int, Int)]
					
						println("The node responsible for key "+ key +" is: "+ result._1+ " whose nodeId is: "+result._2)
						println("Number of hops: "+ result._3)
						TotalHops += result._3

						future10 = null
						result= null
					}
				}

			println("\n\nPerformed a lookup of "+ (numRequests*numNodes) +" keys.")
			println("Average number of hops per request was: "+ (TotalHops/(numRequests*numNodes)).toInt)
			}

			

			Thread.sleep(2000)

/*
			//Failure Model
			println("\n\n--------Failure model------------- ")
			for (i<-0 until numNodes/2){
				val z = (scala.util.Random).nextInt(nodesList.length)
				context.stop(nodesList(z))
				nodesList -= nodesList(z)
				numNodes = numNodes - 1
			}
			println("nodesList length= " + nodesList.length)
			val scanner4 = new java.util.Scanner(System.in)


			print("\n\nEnter a key that you want to lookup from 0 to "+ MAX_KEY +": ")
			var hpcount = 0
			val input4 = scanner4.nextLine()
			//println(input.toInt)
			val m = (scala.util.Random).nextInt(nodesList.length)
			println("At node: "+ nodesList(m) + " search for key: " + input4)
			val futr1 = nodesList(m) ? Find_Successor(input4.toInt, hpcount)
			val result1 = Await.result(futr1, timeout.duration).asInstanceOf[(ActorRef, Int, Int)]
			
			println("\n\nThe node responsible for the given key is: "+ result1._1+ " whose nodeId is: "+result1._2)
			println("Number of hops: "+ result1._3)

*/

			context.system.shutdown()
			


					
			//Concurrent Join
			/*val r = (scala.util.Random).nextInt(nodesList.length)			
			nodesList(i) ! ConcurrentJoin(nodesList(r))				
			nodesList(i) ! Stabilize()
			val future1 = nodesList(i) ? GetSuccessor()
			val successorNode = Await.result(future1, timeout.duration).asInstanceOf[ActorRef]
			successorNode ! Stabilize()
			val future2 = nodesList(i) ? GetPredecessor()
			val predecessorNode = Await.result(future2, timeout.duration).asInstanceOf[ActorRef]
			if (predecessorNode != null){predecessorNode ! Stabilize()}
			*/


			//fix_fingers()
			/*for (i<-0 until numNodes) {
					nodesList(i) ! Fix_Fingers()
					
					}*/

}
}