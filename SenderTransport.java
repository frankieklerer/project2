
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class which represents the sender transport layer
 */
public class SenderTransport
{
    private NetworkLayer networkLayer;
    private Timeline timeline;

    // window size of the sender
    private int windowSize;

    // boolean indicating whether the protocol is TCP or not
    private boolean usingTCP;

    // global sequence number for outgoing packets
    private int sequenceNumber;

    // boolean indicating if the timer for the oldest sent packet is on or not
    private boolean timerOn;

    // keeps track of what the expected ACk number is  
    private int tcpACKnum;

    // array list that keeps trac of for many times a packet has been ACKed
    // helps look for Triple Duplicate ACK
    private ArrayList<Integer> ackCountTCP;

    /**
    * Array list to keep track of the sliding window
    * The indexs indicate the packet sequence number and the entries indicate the packet status:
    * 1 means the packet has not yet been sent but it is in the current window
    * 2 means the packet has been sent but its ACK has not been received
    * 3 means the packet has been sent and ACKed
    * 5 means window was full and packet is waiting to be sent
    **/
    private ArrayList<Integer> packetStatusCode;

    // Array list of Packet objects to keep track of which packets are in the current window
    // GBN will resend all packets in this window 
    private ArrayList<Packet> currentWindow; 

    /**
    * Array list of Packet objects whose indexs correspond to packet sequence number
    **/
    private HashMap<Integer, Packet> packets; 

    //packet buffer for when window is full
    private ArrayList<Packet> waitingToSend;

    /**
    * Constructor
    * Sender TL is creater with the NL and intialized
    **/
    public SenderTransport(NetworkLayer networkLayer){
        this.networkLayer=networkLayer;
        this.packetStatusCode = new ArrayList<Integer>();
        this.waitingToSend = new ArrayList<Packet>();
        this.packets = new HashMap<Integer, Packet>(); 
        this.initialize();
       
    }

    /**
    * Initialize method sets all sequence number
    **/
    public void initialize(){
      // set the sequence number to 0
      this.sequenceNumber = 0;

      // set the timer on to false
      this.timerOn = false;

      if(usingTCP) {
        // for TCP, set these variables
        tcpACKnum = 0;
      }else{
          
      }     
    }

    /**
    * Send Message method which takes in a Message object and sends a packet depending 
    * on the protocol
    **/
    public void sendMessage(Message msg){

      // if the protocol is TCP
      if(usingTCP){

        //add extra place incase of overflow of packets
        packetStatusCode.add(1);

        // set the packet sequence number to the global sequence number
        int packetSeqNum = sequenceNumber;
        
        // increment sequence number
        sequenceNumber++;

        // set the sent & stored as false
        boolean sent = false;
        boolean stored = false;
        
        // for every packet in the status code array
        for(int i = 0; i < packetStatusCode.size(); i++){

            // if the packet has not yet been sent // took out AND the sequence number is equal to the tcpACKnum
            if(packetStatusCode.get(i) == 1 && i==packetSeqNum){

              //create a new packet with the message (-1 because no ack num)
              Packet storePacket = new Packet(msg, i,-1);
              Packet sendPacket = new Packet(msg, i, -1);
              
             // analyzeCurrentWindow();

              // place new packet in hash map with associated sequence number
              packets.put(i, storePacket);

              //initialize the ACK count for the packet to 0
              ackCountTCP.add(i,0);

              //if the window is not full
              if(currentWindow.size() < windowSize)
              {
                // add packet to current window
                currentWindow.add(storePacket);

                // set the packet status code to sent but waiting for ACK
                packetStatusCode.set(i, 2);

                System.out.println("Packet " + i + " has been sent.");

                // send the packet to the network layer
                networkLayer.sendPacket(sendPacket, Event.RECEIVER);

                // set sent as true
                sent = true;

                if(!timerOn){
                  timeline.startTimer(50);
                  timerOn = true;
                }
              } //else if the window is full 
              else {
                if(!stored && !sent){ //and the message has not been stored or sent yet
                  System.out.println("Window is currently full, storing packet " + i + " , will try to resend later.");
                  
                  //add packet to the waiting to send list
                  waitingToSend.add(storePacket);

                  //set status to waiting to be sent
                  packetStatusCode.set(i,5);
                  stored = true;
                }
              }

            }

          // if the packets in the window have already been sent, break out of the code
          if(sent){
            tcpACKnum++;
            break;
          }
        }

      }else{ //if the protocol is GBN

        //add extra place in case of overflow of packets
        packetStatusCode.add(1);
        
        // initialize the packet sequence number as the global sequence number
        int packetSeqNum = sequenceNumber;

        // increment sequence number
        sequenceNumber++;

        // set the sent & stored boolean to false
        boolean sent = false;
        boolean stored = false;

        // for every packet in the status code array
        for(int i = 0; i < packetStatusCode.size(); i++){

            // if the packet has not yet been sent
            if(packetStatusCode.get(i) == 1 && i==packetSeqNum){

              //create a new packet with the message (-1 because no ack num)
              Packet storePacket = new Packet(msg, i,-1);
              Packet sendPacket = new Packet(msg, i, -1);

              // place new packet in hash map with associated sequence number
              packets.put(i, storePacket);

             //if the window is not full
             if(currentWindow.size() < windowSize)
              { 
                // add packet to current window
                currentWindow.add(storePacket);
                
                // set the packet status code to sent but waiting for ACK
                packetStatusCode.set(i, 2);
                
                System.out.println("Packet " + i + " has been sent.");

                // send the packet to the network layer
                networkLayer.sendPacket(sendPacket, Event.RECEIVER);

                // set sent as true
                sent = true;

                if(!timerOn){
                  timeline.startTimer(50);
                  timerOn = true;
                }
              }
              else { //if the window is full 
                if(!stored && !sent){ //and the message has not been stored or sent yet
                  System.out.println("Window is currently full, storing packet " + i + " , will try to resend later.");
                  
                  //add the packet to the waiting to send list
                  waitingToSend.add(storePacket);
                  
                  //set as waiting to send 
                  packetStatusCode.set(i,5);
                  stored = true;
                }
              }
            }

            // don't send a packet more than once
            if(sent){
              break;
            }
        }
      }
    }

    /**
    * Receive an ACK from the receiver, act accordingly
    **/
    public void receiveMessage(Packet receivedPacket){

        if(usingTCP){

          // if the packet is corrupt
          if(receivedPacket.isCorrupt()){

            //if pkt is corrupt, wait for timeout.
            System.out.println("Received ACK is corrupt, wait for timeout or duplicate acks.");

          }else{ //received not corrupted ack. 
              if(timerOn){
                  timeline.stopTimer();
                  timerOn = false;
                }

              // get the ACK number of the packet
              int ackExpectedNum = receivedPacket.getAcknum();

              // the ack you receive will be the ack for 1 below the received ack number
              int ackNum = ackExpectedNum-1; 

              // indicating if the first packet was lost or not
              boolean lostFirst = false;

              // if the packet 0 was lost, then it sends an ack for 0 since there is nothing below it
              if(ackExpectedNum == 0){

                // resend the first packet
                Packet firstresend = new Packet(packets.get(0).getMessage(), packets.get(0).getSeqnum(), -1);
                networkLayer.sendPacket(firstresend, Event.RECEIVER);

                // start the timer for it
                if(!timerOn){
                    timeline.startTimer(50);
                    timerOn = true;
                }

                lostFirst = true;
                System.out.println("Packet 0 has been lost and is being resent");
              }

              if(!lostFirst){

                // if the packet has already been sent and ACKed
                if(packetStatusCode.get(ackNum) == 3){

                  // get the amount of times it has been ACKed
                  int count = ackCountTCP.get(ackNum);

                  // increment the ACK
                  ackCountTCP.set(ackNum, count++);

                  // if the incremented ACK num is 3 TRIPLE DUPLICATE ACK CASE
                  if(ackCountTCP.get(ackNum) == 3){

                    // fetch packet object from hash map with the ack num
                    Packet resend = new Packet(packets.get(ackNum).getMessage(), packets.get(ackNum).getSeqnum(), -1);

                    networkLayer.sendPacket(resend, Event.RECEIVER);

                    System.out.println("Packet " + ackNum + "has been resent");
                  }

                  //if the received packet it sent but not acked
                  }else if(packetStatusCode.get(ackNum) == 2) {

                    System.out.println("ACK for packet " + ackNum + " received");

                    analyzeCurrentWindow();

                    // for all the packets in the current window
                    for(int i = 0; i < currentWindow.size(); i++){

                      // get the packet number of the packet in current window
                      int packetNum = currentWindow.get(i).getSeqnum();

                      // that have been sent but not yet ACKed
                      if(packetStatusCode.get(packetNum) == 2 && (packetNum < ackNum)){

                        System.out.println("Cumulative ACK for Packet " + packetNum);

                        // ack them all
                        moveWindow(packetNum);
                      }
                    }
                      moveWindow(ackNum);
                  }

                  //restart timer if packets still in flight
                    for(int j = 0; j < currentWindow.size(); j++){

                    // get the packet number of the packet in current window
                    int tempseq = currentWindow.get(j).getSeqnum();

                      // that have been sent but not yet ACKed
                      if(packetStatusCode.get(tempseq) == 2){

                         if(!timerOn){
                          timeline.startTimer(50);
                          timerOn = true;
                         }
                      }
                     }
                }
            }
        
        }else{ // using GBN

            // if the packet is corrupt
            if(receivedPacket.isCorrupt()){

              //if pkt is corrupt, resend all sent but unacked messages in current window (2)
              System.out.println("Received ACK is corrupt, resending all sent but unacked packets in window.");

              //analyzeCurrentWindow();

                   // for all the packets in the current window
              for(int i = 0; i < currentWindow.size(); i++){

                // get the packet number of the packet in current window
                int packetNum = currentWindow.get(i).getSeqnum();

                // that have been sent but not yet ACKed
                if(packetStatusCode.get(packetNum) == 2){

                  // get the packet object to be resent
                  Packet resend = new Packet(packets.get(packetNum).getMessage(), packets.get(packetNum).getSeqnum(), -1);

                  networkLayer.sendPacket(resend, Event.RECEIVER);

                  //System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");
                  System.out.println("Packet " + packetNum + " has been resent");

                  // re starts timer for oldest unACKed packet
                  if(!timerOn){
                    timeline.startTimer(50);
                    timerOn = true;
                  }
                }
              }

            } else {  // received an uncorrupted ACK

              if(timerOn){
                  timeline.stopTimer();
                  timerOn = false;
              }

              boolean lostFirst = false;

              // get the ACK number of the packet
              int ackNum = receivedPacket.getAcknum();

              //if first pkt was lost and receive an ACK for -1
              if(ackNum == -1){
                Packet firstresend = new Packet(packets.get(0).getMessage(), packets.get(0).getSeqnum(), -1);
                networkLayer.sendPacket(firstresend, Event.RECEIVER);

                if(!timerOn){
                    timeline.startTimer(50);
                    timerOn = true;
                }

                lostFirst = true;
                System.out.println("Packet " + 0 + " has been lost and is being resent");

                //analyzeCurrentWindow();

                 // for all the packets in the current window
                for(int i = 1; i < currentWindow.size(); i++){

                // get the packet number of the packet in current window
                int packetNum = currentWindow.get(i).getSeqnum();

                  // that have been sent but not yet ACKed
                  if(packetStatusCode.get(packetNum) == 2){

                    // get the packet object to be resent
                    Packet resend = packets.get(packetNum);

                    networkLayer.sendPacket(resend, Event.RECEIVER);

                    //System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");
                    System.out.println("Packet " + packetNum + " has been resent because packet 0 was lost");

                  }
                }
              }

              // if the first packet sent wasn't lost..
              if(!lostFirst){
           
                 // if the packet has already been ACKed, then the receiver is confused/received a lost/corrupted packet
                if(packetStatusCode.get(ackNum) == 3){

                  // for all the packets in the current window
                  for(int i = 0; i < currentWindow.size(); i++){

                    // get the packet number of the packet in current window
                    int packetNum = currentWindow.get(i).getSeqnum();

                    // that have been sent but not yet ACKed
                    if(packetStatusCode.get(packetNum) == 2){

                      // get the packet object to be resent
                      Packet resend = new Packet(packets.get(packetNum).getMessage(), packets.get(packetNum).getSeqnum(), -1);

                      networkLayer.sendPacket(resend, Event.RECEIVER);

                      //System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");
                      System.out.println("Packet " + packetNum + " has been resent");
                      
                      if(!timerOn){
                        timeline.startTimer(50);
                        timerOn = true;
                      }

                    }
                  }

                }

                // if sent but unACKed
                if(packetStatusCode.get(ackNum) == 2){

                  if(timerOn){
                    timeline.stopTimer();
                    timerOn = false;
                  }

                  System.out.println("ACK received for Packet " + ackNum);

                  analyzeCurrentWindow();

                  // for all the packets in the current window
                  for(int i = 0; i < currentWindow.size(); i++){

                    // get the packet number of the packet in current window
                    int packetNum = currentWindow.get(i).getSeqnum();

                    // that have been sent but not yet ACKed
                    if(packetStatusCode.get(packetNum) == 2 && (packetNum < ackNum)){

                      System.out.println("Cumulative ACK for Packet " + packetNum);

                      // ack them all
                      moveWindow(packetNum);

                      if(!timerOn){
                      timeline.startTimer(50);
                      timerOn = true;
                      }
                    }
                  
                  }

                  // set it as ack
                  moveWindow(ackNum); 
                  

                  
                }
              }
              //restart timer if packets still in flight
              boolean stillSending = false;
              for(int i = 0; i < currentWindow.size(); i++){

              // get the packet number of the packet in current window
                int packetNum = currentWindow.get(i).getSeqnum();

                // that have been sent but not yet ACKed
                if(packetStatusCode.get(packetNum) == 2){
                  if(!timerOn){
                     timeline.startTimer(50);
                     timerOn = true;
                   }
                   stillSending = true;
                }
              }
              if(!stillSending)
              {
                if(timerOn){
              timeline.stopTimer();
              timerOn = false;
                }
              }
            }
          }
    }

    /**
    * timer expired method acts on what to do when the timer expires
    **/
    public void timerExpired(){

      timerOn = false;
      boolean resent = false;

      if(usingTCP){

        System.out.println("Timer has expired, resend oldest unACKed packet");

        //analyzeCurrentWindow();

        // for all the packets in the current window
        for(int i = 0; i < currentWindow.size(); i++){

          // get the packet number of the packet in current window
          int packetNum = currentWindow.get(i).getSeqnum();

          // that have been sent but not yet ACKed
          if(packetStatusCode.get(packetNum) == 2){

            // get the packet object to be resent
            Packet resend = new Packet(packets.get(packetNum).getMessage(), packets.get(packetNum).getSeqnum(), -1);

            networkLayer.sendPacket(resend, Event.RECEIVER);

            //System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");
            System.out.println("Packet " + packetNum + " has been resent");

            if(!timerOn){
               timeline.startTimer(50);
               timerOn = true;
            }

            resent = true;
          }
            
          if(resent)
            break;
        }

      } else { // if using GBN

          //when timeout resend all sent but unacked pkts

          System.out.println("Timer for oldest inflight packet has expired, resend all sent but unacked packets");

          //analyzeCurrentWindow();

          // for all the packets in the current window
            for(int i = 0; i < currentWindow.size(); i++){

              // get the packet number of the packet in current window
              int packetNum = currentWindow.get(i).getSeqnum();

              // that have been sent but not yet ACKed
              if(packetStatusCode.get(packetNum) == 2){

                // get the packet object to be resent
                Packet resend = new Packet(packets.get(packetNum).getMessage(), packets.get(packetNum).getSeqnum(), -1);

                networkLayer.sendPacket(resend, Event.RECEIVER);

                //System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");
                System.out.println("Packet " + packetNum + " has been resent");

                if(!timerOn){
                 timeline.startTimer(50);
                 timerOn = true;
                }
              }
            }
      }
    }

    /**
    * Method that updates the window and packet list on every arrival of an uncorrupted ACK
    **/
    public void moveWindow(int packetAckNum){

      int nextPacketSeqNum = windowSize+packetAckNum;
      System.out.println("Marking packet " + packetAckNum + " as status code of 3");
      packetStatusCode.set(packetAckNum,3);
      //System.out.println(nextPacketSeqNum);

      // expand status code 
      packetStatusCode.set(nextPacketSeqNum,1);
      System.out.println("Placing status code of 1 for packet " + nextPacketSeqNum);

      currentWindow.remove(0);
      System.out.println("Removing packet " + packetAckNum + " from current window");

      //try to send packets that were buffered because the window was full
      this.attemptSend();
    }

    public void attemptSend()
    {
      if(waitingToSend.isEmpty())
      {
        //nothing to do here?
      }
      else{
        for(int i = 0; i < waitingToSend.size(); i++)
        { //for all the packets waiting to send, send as many as fit in the window
          if(currentWindow.size() < windowSize)
          {
            //extract the msg and seq number from packet to send
            Packet resend = waitingToSend.get(i);
            Message msg = resend.getMessage();
            int seqn = resend.getSeqnum();

            networkLayer.sendPacket(new Packet(msg, seqn, -1), Event.RECEIVER);
            
            // add packet to current window
            currentWindow.add(waitingToSend.get(i));

            // set the packet status code to sent but waiting for ACK
            packetStatusCode.set(seqn, 2);
            
            System.out.println("Opening in the window, packet " + seqn + " has been sent.");
            //remove packet because its been sent
            waitingToSend.remove(i);

            if(!timerOn){
              timeline.startTimer(50);
              timerOn = true;
            }
          }
        }
      }
    }

    public void analyzeCurrentWindow(){
      ArrayList<String> toPrint = new ArrayList<String>();
      for(int i = 0; i < currentWindow.size(); i++){
          toPrint.add("Packet " + currentWindow.get(i).getSeqnum() + "("  + packetStatusCode.get(i) + ")");
      }

      System.out.println("Current Window: " + toPrint);
    } 

    /**
    * Initialize the timeline class
    **/
    public void setTimeLine(Timeline timeline){
        this.timeline=timeline;
    }

    /**
    * Set the window size from the program parameters
    **/
    public void setWindowSize(int windowSize){
      //System.out.println("Window size of " + windowSize);
      this.windowSize=windowSize;
      this.currentWindow = new ArrayList<Packet>(this.windowSize);
      this.ackCountTCP = new ArrayList<Integer>();
      this.initializeWindow();

    }

    /**
    * Sets the protocol to GBN or TCP
    * 0 means you are using GBN
    * 1 means you are using TCP
    **/
    public void setProtocol(int protocolType){
      if(protocolType == 0){
        usingTCP=false;
        System.out.println("Congrats! You are using the GBN protocol");
      }else{
        usingTCP=true;
        System.out.println("Congrats! You are using the TCP protocol");
      }
    }

    /**
    * Initialize the window size by going through the window size and adding 1 (packet in window but not sent) in the indexs
    **/
    public void initializeWindow() {

      if(usingTCP){
        for(int i = 0; i < windowSize; i++) {
          System.out.println("Placing status code 1 for packet " + i);
          packetStatusCode.add(i,1);
        }

      } else {
        for(int i = 0; i < windowSize; i++) {
          System.out.println("Placing status code 1 for packet " + i);
          packetStatusCode.add(i,1);
        }
      }
    }
}
