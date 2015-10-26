Each node participating in the Chord lookup service is implemented as an Akka actor.

Chord.scala is the main program that starts the protocol.
Application.scala is the master actor that initializes all the Chord nodes(actors).
Node.scala implements the chord actors.



Functions of the protocol as described in the paper "Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications" had to be modified in order to suit the actor model of programming but they are at par with the pseudocode given.
I used ask(?) feature of Akka messaging in order to query a node and get a reply back as a future.

Features: Key Lookup, New Node Join, Simultaneous requests at different nodes to calculate average no. of hops for lookup for a given network dimension(no. of Nodes).



To run the program:

cd to the directory from the terminal.

>cd Chord

>sbt

>run numNodes numRequests

numNodes can be: 100, 200 etc.

numRequests can be: 2, 3 etc.

Example: run 200 3

Enter the respective values when the terminal prompts:

Enter a key that you want to lookup from 0 to 65535: (Any key between 0 and 65535)

Enter ip address of the newly joining node: (ip address of the new node that's going to join the chord network: could be any string)

Do you want to see Chord in action? : yes 
(To see how Chord behaves when there are simultaneous requests at different nodes and to check the average
no. of hops it takes for the key lookup in a given network)
