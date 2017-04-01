
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class which represents the sender transport layer
 */
public class SenderTransport
{
    private NetworkLayer networkLayer;
    private Timeline timeline;
    private int windowSize;
    private boolean usingTCP;
    private int sequenceNumber;
    private boolean timerOn;
    private int tcpACKnum;
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
    * Array list of Message objects whose indexs correspond to packet sequence number
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

    public void initialize(){
      this.sequenceNumber = 0;
       this.timerOn = false;
        if(usingTCP) {
          tcpACKnum = 0;
          this.ackCountTCP = new ArrayList<Integer>();
        }else{
            
        }     
    }

    public void sendMessage(Message msg){

    
        if(usingTCP){
          int packetSeqNum = sequenceNumber;
          boolean sent = false;
          

<<<<<<< HEAD
          

        }else{
=======
          // increment sequence number
          sequenceNumber++;

          for(int i = 0; i < packetStatusCode.size(); i++){

              // if the packet has not yet been sent
              if(packetStatusCode.get(i) == 1 && packetSeqNum == tcpACKnum){

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
                sent = true;
                ackCountTCP.add(0);

                if(!timerOn){
                  timeline.startTimer(50);
                  timerOn = true;
                }
              }

              if(sent){
                break;
              }
          }
        }

        else{
>>>>>>> 4fb04c5e61a0a5059dd971c08c2863412080236e

          int packetSeqNum = sequenceNumber;
          boolean sent = false;

          // increment sequence number
          sequenceNumber++;

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
                sent = true;

                if(!timerOn){
                  timeline.startTimer(50);
                  timerOn = true;
                }
              }

              if(sent){
                break;
              }

          }

        }
        

    }

    /**
    * Receive an ACK from the receiver
    **/
    public void receiveMessage(Packet receivedPacket){

        if(usingTCP){
            // if the packet is corrupt
            if(receivedPacket.isCorrupt())
            {
              //if pkt is corrupt, wait for timeout.
              System.out.println("Received ACK is corrupt, wait for timeout or duplicate acks.");
            }
            else //received not corrupted ack. 
            {
               // get the ACK number of the packet
                int ackNum = receivedPacket.getAcknum();

                if(packetStatusCode.get(ackNum) == 3)
                {
                  int count = ackCountTCP.get(ackNum);
                  ackCountTCP.set(ackNum, count++);
                  if(ackCountTCP.get(ackNum) == 3)
                  {
                      // fetch packet object from hash map with the ack num
                      Packet resend = packets.get(ackNum);

                      networkLayer.sendPacket(resend, Event.RECEIVER);

                      System.out.println("Packet " + ackNum + "has been resent");
                  }
                }

                else if(packetStatusCode.get(ackNum) == 2) //sent but ack not yet recieved
                {
                    System.out.println("ACK received for Packet " + ackNum);

                    // set it as ack
                    moveWindow(ackNum); 

                    // check if there are unACKed packets before this packet 
                    for(int j = 0; j < ackNum; j++){

                     // Packet unACKed = currentWindow.get(j);

                      // if they still have not been ACKed
                    if(packetStatusCode.get(j) == 2){

                        System.out.println("Cumulative ACK for Packet " + j);

                        // ack them all
                        packetStatusCode.set(j, 3);
                        moveWindow(j);
                      }
                 }
   
                }
                


            }




            if(timerOn)
            {
                timeline.stopTimer();
                timerOn = false;
            }
        }

        else{

            // get the ACK number of the packet
            int ackNum = receivedPacket.getAcknum();

            if(timerOn)
            {
                timeline.stopTimer();
                timerOn = false;
            }


            // if the packet is corrupt
            if(receivedPacket.isCorrupt()){

              //if pkt is corrupt, resend all sent but unacked messages in current window (2)
              System.out.println("Received ACK is corrupt, resending all sent but unacked packets in window.");

              // for all the packets in the current window
              for(int i = 0; i < currentWindow.size(); i++){

                // that have been sent but not yet ACKed
                if(packetStatusCode.get(i) == 2){

                  // fetch packet object from hash map with the ack num
                  Packet resend = packets.get(ackNum);

                  networkLayer.sendPacket(resend, Event.RECEIVER);

                  // re starts timer for oldest unACKed packet
                  if(!timerOn){
                    timeline.startTimer(50);
                    timerOn = true;
                  }

                  System.out.println("Packet " + ackNum + " has been resent");
                }
              }

            } else { // received an uncorrupted ACK

              // if the packet has already been ACKed, then the receiver is confused/received a corrupted packet
              if(packetStatusCode.get(ackNum) == 3){

                   // for all the packets in the current window
                  for(int i = 0; i < currentWindow.size(); i++){

                    // that have been sent but not yet ACKed
                    if(packetStatusCode.get(i) == 2){

                      // fetch packet object from hash map with the ack num
                      Packet resend = packets.get(ackNum);

                      networkLayer.sendPacket(resend, Event.RECEIVER);

                      System.out.println("Packet " + ackNum + "has been resent");
                    }
                  }

                }
              // if sent but unACKed
              if(packetStatusCode.get(ackNum) == 2){
                System.out.println("ACK received for Packet " + ackNum);

                // set it as ack
                moveWindow(ackNum); 

                // check if there are unACKed packets before this packet 
                for(int j = 0; j < ackNum; j++){

                 // Packet unACKed = currentWindow.get(j);

                  // if they still have not been ACKed
                  if(packetStatusCode.get(j) == 2){

                    System.out.println("Cumulative ACK for Packet " + j);

                    // ack them all
                    packetStatusCode.set(j, 3);
                    moveWindow(j);
                  }
                 }

                
                }
              }
          }
    }

    public void timerExpired()
    { 
      timerOn = false;
      boolean resent = false;

        if(usingTCP){

          System.out.println("Timer for oldest inflight packet has expired, resend oldest unACKed packet");

          for(int i = 0; i < sequenceNumber; i++)
          {
              if(packetStatusCode.get(i) == 2)
              {
                Packet toBeResent = currentWindow.get(i);

                // fetch packet object from hash map with the ack num
                Packet resend = packets.get(toBeResent.getSeqnum());

                networkLayer.sendPacket(resend, Event.RECEIVER);
                System.out.println("Packet " +  toBeResent.getSeqnum() + " has been resent.");

                if(!timerOn){
                   timeline.startTimer(50);
                   timerOn = true;
                  }

                resent = true;
              }
              
              if(resent)
                break;
          }
        } else {
            //when timeout resend all sent but unacked pkts

            System.out.println("Timer for oldest inflight packet has expired, resend all sent but unacked packets");

            // for all the packets in the current window
              for(int i = 0; i < sequenceNumber; i++){

                // that have been sent but not yet ACKed
                if(packetStatusCode.get(i) == 2){


                  Packet toBeResent = currentWindow.get(i);

                  // fetch packet object from hash map with the ack num
                  Packet resend = packets.get(toBeResent.getSeqnum());

                  networkLayer.sendPacket(resend, Event.RECEIVER);

                  System.out.println("Packet " + toBeResent.getSeqnum() + " has been resent");

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
        for(int i = 0; i < windowSize; i++) {
          System.out.println("Placing status code 1 for packet " + i);
          packetStatusCode.add(i,1);
        }
    }
}
