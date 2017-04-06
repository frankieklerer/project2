------------------------------------------------------------------------
This is the project README file. Here, you should describe your project.
Tell the reader (someone who does not know anything about this project)
all he/she needs to know. The comments should usually include at least:
------------------------------------------------------------------------

PROJECT TITLE: Implementing Transport Layer Protocol
PURPOSE OF PROJECT: to learn how to implement a real world protocol
VERSION or DATE: 3/22/17
HOW TO START THIS PROJECT: 
AUTHORS: Frankie & Aggie
USER INSTRUCTIONS:
	Run terminal and ls into the "project2" folder.
	Once there, run "javac *.java" to compile the program
	When you're ready to run the program, there are several parameters to consider..

	"java NetworkSimulator test.txt a b c d e f" is the basic format of the input.

		They are explained below:
		- 'java NetworkSimulator test.txt' is mandatory because it runs the simulation 
		class that runs the program and it takes test.txt file as input to the program, 
		which contains all the messages to be sent (each line is a new message)
		- the 'a' parameter is the average time between all messages sent from the 
		senders application layer. A time of 5 will send messages much faster than a 
		time of 50 (which might cause many timeouts within the program). It can be any
		non-zero, positive integer.
		- the 'b' parameter is the loss probability within the sender and the receiver.
		A rate of 0.1 means that on average, one in ten packets get lost. It can be any
		value between 0 and 10. (0.1, 0.2, .... 0.8, 0.9)
		- the 'c' parameter is the corruption probability within the sender and the 
		receiver. A rate of 0.2 means that on average, one in ten packets get corrupted.
		The packets data, sequence number, ACK number and checksum fields can be corrupted.
		- the 'd' parameter controls the sender window size for the Go-Back-N protocol, it 
		can be any non-zero positive integer.
		- the 'e' parameter controls the protocol type being used in the program. A value of 
		0 means the program is using the Go-Back-N protocol. A value of 1 means the program is 
		using the TCP protocol.
		- the 'f' parameter controls the tracing aspects of the program so that the user can
		see what is happening within the program. A tracing value of 0 will turn this off. A 
		tracing of value 1 prints out times for sending, receiving and timers expiring events.
		A tracing of value 2, prints out when a message is corrupted and lost.  A tracing 
		value greater than 2 will display messages that are related to the event timeline.

When you run the format, the program will start and various print lines will appear on your 
screen. You can use these to see what is going on in the program.s
