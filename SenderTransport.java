
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
    private int sequenceNumber=0;

    /**
    * Array list to keep track of the sliding window
    * The indexs indicate the packet sequence number and the entries indicate the packet status:
    * 1 means the packet has not yet been sent but it is in the current window
    * 2 means the packet has been sent but its ACK has not been received
    * 3 means the packet has been sent and ACKed
    **/
    private ArrayList<Integer> packetStatusCode = new ArrayList<Integer>(50); 

    // Array list of Packet objects to keep track of which packets are in the current window
    // GBN will resend all packets in this window 
    private ArrayList<Packet> currentWindow; 

    /**
    * Array list of Message objects whose indexs correspond to packet sequence number
    **/
    private HashMap<Integer, Packet> packets = new HashMap<Integer, Packet>(50); 

    private ArrayList<String> messageList;

    /**
    * Constructor
    * Sender TL is creater with the NL and intialized
    **/
    public SenderTransport(NetworkLayer networkLayer, ArrayList<String> messages){
        this.networkLayer=networkLayer;
        this.messageList = messages;
        this.initialize();
    }

    public void initialize(){
        if(usingTCP) {

        }else{
            
        }     
    }

    public void sendMessage(Message msg){
    
        if(usingTCP){

        }else{

            // for every packet in the current window 
            for(int i = 0; i < windowSize; i++){

                // if the packet has not yet been sent
                if(packetStatusCode.get(i) == 1){

                    //create a new packet with the message
                    Packet newPacket = new Packet(msg, sequenceNumber,-1);

                    // add packet to current window
                    currentWindow.add(i, newPacket);

                    // set the packet status code to sent but waiting for ACK
                    packetStatusCode.set(i, 2);

                    // place new packet in hash map with associated sequence number
                    packets.put(sequenceNumber, newPacket);

                    // send the packet to the network layer
                    networkLayer.sendPacket(newPacket, Event.RECEIVER);

                    //timeline.startTimer(10);

                    System.out.println("Packet " + sequenceNumber + " has been sent.");

                    // increment sequence number
                    sequenceNumber++;
                }
            }
        }
        

    }

    public void receiveMessage(Packet receivedPacket){

        if(usingTCP){

        }else{

            // get the ACK number of the packet
            int ackNum = receivedPacket.getAcknum();

           // timeline.stopTimer();

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

                  // timeline.startTimer(10);
                  System.out.println("Packet " + ackNum + "has been resent");
                }
              }

             // timeline.startTimer(10);

            } else { // received an uncorrupted ACK

              if(packetStatusCode.get(ackNum) == 2){
                System.out.println("ACK received for Packet " + ackNum);
                packetStatusCode.set(packetStatusCode.get(ackNum), 3);
                moveWindow(ackNum); 

                // find where the packet is in current window
                int indexOfPacket = currentWindow.indexOf(receivedPacket);

                // check if there are unACKed packets before this packet 
                for(int j = 0; j < indexOfPacket; j++){

                  Packet unACKed = currentWindow.get(j);

                  // if they still have not been ACKed
                  if(packetStatusCode.get(unACKed.getAcknum()) == 2){

                    System.out.println("Cumulative ACK for Packet " + unACKed.getAcknum());
                    packetStatusCode.set(unACKed.getAcknum(), 3);
                    moveWindow(unACKed.getAcknum());
                  }
                 }
                }
              }
          }
    }

    public void timerExpired()
    { 

        // if(usingTCP){

        // } else {
        //     //when timeout 
        //     //resend all sent but unacked pkts
        //     //need to write this --- how to know when timeout actually occurs
        //     System.out.println("Timer for oldest inflight packet has expired, resend all sent but unacked packets");
        //     for(int j = 0; j < packetStatusCode.size(); j++)
        //     {
        //         if(packetStatusCode.get(j) == 2)
        //         {
        //             Packet resend = new Packet(messageList.get(j), j, -1);
        //             networkLayer.sendPacket(resend, Event.RECEIVER);
        //             timeline.startTimer(10);
        //             System.out.println("Resent packet " + j);
        //         }
        //     }
        // }
    }

    /**
    * Method that updates the window and packet list on every arrival of an uncorrupted ACK
    **/
    public void moveWindow(int packetAckNum){

        packetStatusCode.add(packetAckNum+windowSize, 1);
        System.out.println("Placing status code of 1 for " + packetAckNum+windowSize);
        Packet toBeRemoved = currentWindow.get(packetAckNum);
        currentWindow.remove(toBeRemoved);
        Packet newPacket = packets.get(packetAckNum+windowSize);
        currentWindow.add(newPacket);
        System.out.println("Adding packet " + packetAckNum+windowSize + " to current");
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
      System.out.println("Window size of " + windowSize);
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
          //currentWindow.add(i);
        }
    }
}
