
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
    **/
    private ArrayList<Integer> packetStatusCode;

    // Array list of Packet objects to keep track of which packets are in the current window
    // GBN will resend all packets in this window 
    private ArrayList<Packet> currentWindow; 

    /**
    * Array list of Packet objects whose indexs correspond to packet sequence number
    **/
    private HashMap<Integer, Packet> packets; 

    /**
    * Constructor
    * Sender TL is creater with the NL and intialized
    **/
    public SenderTransport(NetworkLayer networkLayer){
        this.networkLayer=networkLayer;
        this.packetStatusCode = new ArrayList<Integer>();
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

        // set the packet sequence number to the global sequence number
        int packetSeqNum = sequenceNumber;
        
        // increment sequence number
        sequenceNumber++;

        // set the sent as false
        boolean sent = false;
        
        // for every packet in the status code array
        for(int i = 0; i < packetStatusCode.size(); i++){

            // if the packet has not yet been sent AND the sequence number is equal to the tcpACKnum
            if((packetStatusCode.get(i) == 1) && (packetSeqNum == tcpACKnum)){

             // set the packet status code to sent but waiting for ACK
              packetStatusCode.set(i, 2);

              //create a new packet with the message (-1 because no ack num)
              Packet newPacket = new Packet(msg, i,-1);

              // add packet to current window
              currentWindow.add(newPacket);

             // analyzeCurrentWindow();

              // place new packet in hash map with associated sequence number
              packets.put(i, newPacket);

              System.out.println("Packet " + i + " has been sent.");

              // send the packet to the network layer
              networkLayer.sendPacket(newPacket, Event.RECEIVER);

              // set sent as true
              sent = true;

              //initialize the ACK count for the packet to 0
              ackCountTCP.add(i,0);

              if(!timerOn){
                timeline.startTimer(50);
                timerOn = true;
              }

            }

          // if the packets in the window have already been sent, break out of the code
          if(sent){
            tcpACKnum++;
            break;
          }
        }

      }else{ //if the protocol is GBN

        // initialize the packet sequence number as the global sequence number
        int packetSeqNum = sequenceNumber;

        // increment sequence number
        sequenceNumber++;

        // set the sent boolean to false
        boolean sent = false;

        // for every packet in the status code array
        for(int i = 0; i < packetStatusCode.size(); i++){

            // if the packet has not yet been sent
            if(packetStatusCode.get(i) == 1){

             // set the packet status code to sent but waiting for ACK
              packetStatusCode.set(i, 2);

              //create a new packet with the message (-1 because no ack num)
              Packet newPacket = new Packet(msg, i,-1);

              // add packet to current window
              currentWindow.add(newPacket);

              // place new packet in hash map with associated sequence number
              packets.put(i, newPacket);

              System.out.println("Packet " + i + " has been sent.");

              // send the packet to the network layer
              networkLayer.sendPacket(newPacket, Event.RECEIVER);

              // set the sent variable to true
              sent = true;

              // if the timer isn't already on, turn it on
              // should only turn on for the first packet sent
              if(!timerOn){
                timeline.startTimer(50);
                timerOn = true;
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

           //received not corrupted ack. 
          }else {

             // get the ACK number of the packet
              int ackExpectedNum = receivedPacket.getAcknum();
              int ackNum = ackExpectedNum-1;
              boolean lostFirst = false;

              if(ackExpectedNum == -1){
                  Packet firstresend = packets.get(0);
                  networkLayer.sendPacket(firstresend, Event.RECEIVER);

                  if(!timerOn){
                      timeline.startTimer(50);
                      timerOn = true;
                  }

                  lostFirst = true;
                  System.out.println("Packet 0 has been lost and is being resent");
              }
              
              if(timerOn){
                      timeline.stopTimer();
                    timerOn = false;
                  } 
              if(!lostFirst)
              {
              // if the packet has already been sent and ACKed
              if(packetStatusCode.get(ackNum) == 3){

                // get the amount of times it has been ACKed
                int count = ackCountTCP.get(ackNum);

                // increment the ACK
                ackCountTCP.set(ackNum, count++);

                // if the incremented ACK num is 3
                if(ackCountTCP.get(ackNum) == 3){

                  // fetch packet object from hash map with the ack num
                  Packet resend = packets.get(ackNum);

                  networkLayer.sendPacket(resend, Event.RECEIVER);

                  System.out.println("Packet " + ackNum + "has been resent");
                }

                //if the received packet it sent but not acked
                }else if(packetStatusCode.get(ackNum) == 2) {

                  System.out.println("ACK of " + ackExpectedNum + " received");

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
                  Packet resend = packets.get(packetNum);

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
                Packet firstresend = packets.get(0);
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
                      Packet resend = packets.get(packetNum);

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

                  //restart timer if packets still in flight
                  for(int i = 0; i < currentWindow.size(); i++){

                  // get the packet number of the packet in current window
                    int packetNum = currentWindow.get(i).getSeqnum();

                    // that have been sent but not yet ACKed
                    if(packetStatusCode.get(packetNum) == 2){

                      // if(!timerOn){
                      // timeline.startTimer(50);
                      // timerOn = true;
                      // }
                    }
                  }
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

        System.out.println("Timer for oldest inflight packet has expired, resend oldest unACKed packet");

        //analyzeCurrentWindow();

        // for all the packets in the current window
        for(int i = 0; i < currentWindow.size(); i++){

          // get the packet number of the packet in current window
          int packetNum = currentWindow.get(i).getSeqnum();

          // that have been sent but not yet ACKed
          if(packetStatusCode.get(packetNum) == 2){

            // get the packet object to be resent
            Packet resend = packets.get(packetNum);

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
                Packet resend = packets.get(packetNum);

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
      packetStatusCode.add(1);
      System.out.println("Placing status code of 1 for packet " + nextPacketSeqNum);

      currentWindow.remove(0);
      System.out.println("Removing packet " + packetAckNum + " from current window");
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
