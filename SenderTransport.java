
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

    /**
    * Constructor
    * Sender TL is creater with the NL and intialized
    **/
    public SenderTransport(NetworkLayer networkLayer){
        this.networkLayer=networkLayer;
        this.initialize();
    }

    public void initialize(){
        this.initializeWindow();

        if(usingTCP) {

        }else{
            
        }
        
    }

    public void sendMessage(Message msg)
    {
        
        if(usingTCP)
        {

        }else{

            // for every packet in the current window 
            for(int i = 0; i < currentWindow.size(); i++){

                // if the packet has not yet been sent
                if(packetList.get(i) == 1){

                    //create a new packet with the message
                    Packet newPacket = new Packet(msg, sequenceNumber,-1);

                    // set the packet status code to sent but waiting for ACK
                    packetStatusCode.set(i, 2);

                    // place new packet in hash map with associated sequence number
                    packetList.put(sequenceNumber, newPacket);

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

    public void receiveMessage(Packet pkt){

        if(usingTCP){

        }else{

            // get the ACK number of the packet
            int ackNum = pkt.getAcknum();

           // timeline.stopTimer();

            // if the packet is corrupt
            if(pkt.isCorrupt()){

              //if pkt is corrupt, resend all sent but unacked messages in current window (2)
              System.out.println("Received ACK is corrupt, resending all sent but unacked packets in window.");

              // for all the packets in the current window
              for(int i = 0; i < currentWindow.size(); i++){

                // that have been sent but not yet ACKed
                if(packetStatusCode.get(i) == 2){

                  // fetch packet object from hash map with the ack num
                  Packet resend = packetList.get(ackNum);

                  networkLayer.sendPacket(resend, Event.RECEIVER);

                  // timeline.startTimer(10);
                  System.out.println("Packet " + ackNum + has been resent);
                }
              }

             // timeline.startTimer(10);
            } else {
              
            } if(packetStatusCode.get(ackNum) == 2){ //if the pkt has not been acked yet

                packetStatusCode.set(a,3);
                System.out.println("Ack received for packet " + a);
                for(int i = 0; i < a; i++) //accounts for cumulative acks
                {
                    if(packetStatusCode.get(i) == 2 && i != a) 
                    {
                        packetStatusCode.set(i,3);
                        System.out.println("Cumulatively acking packet " + a);
                    }
                }
                moveWindow(); 
            }
            else 
            { //else resend all sent but unacked pkts
                System.out.println("Ack received for out of order packet, resend all sent but unacked packets");
                for(int j = 0; j < packetStatusCode.size(); j++)
                {
                    if(packetStatusCode.get(j) == 2)
                    {
                        Packet resend = new Packet(messageList.get(j), j, -1);
                        networkLayer.sendPacket(resend, Event.RECEIVER);
                      //  timeline.startTimer(10);
                        System.out.println("Resent packet " + j);
                    }
                }
            }
        }
    }

    public void timerExpired()
    { 

        if(usingTCP){

        } else {
            //when timeout 
            //resend all sent but unacked pkts
            //need to write this --- how to know when timeout actually occurs
            System.out.println("Timer for oldest inflight packet has expired, resend all sent but unacked packets");
            for(int j = 0; j < packetStatusCode.size(); j++)
            {
                if(packetStatusCode.get(j) == 2)
                {
                    Packet resend = new Packet(messageList.get(j), j, -1);
                    networkLayer.sendPacket(resend, Event.RECEIVER);
                    timeline.startTimer(10);
                    System.out.println("Resent packet " + j);
                }
            }
        }
    }

    /**
    * Method that updates the window and packet list on every arrival of an uncorrupted ACK
    **/
    public void moveWindow()
    {
        // find the first instance 
        for(int i = 0; i < packetStatusCode.size(); i++)
        {
            if(packetStatusCode.get(i) == 1 || packetStatusCode.get(i) == 2)
                continue;
            else if(packetStatusCode.get(i) == 3)
            {
                if(packetStatusCode.get(i+1) == 3)
                    continue;
                else
                {
                    for(int j = i+1; j < i+1+n; j++)
                            packetStatusCode.add(1);
                }   
            }
            else
                continue;
        }
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
        this.windowSize=windowSize;
        this.currentWindow = new ArrayList<Packet>(this.windowSize);
    }

    /**
    * Sets the protocol to GBN or TCP
    * 0 means you are using GBN
    * 1 means you are using TCP
    **/
    public void setProtocol(int protocolType){
        if(protocolType == 0){
            usingTCP=false;
        }else{
            usingTCP=true;
        }
    }

    /**
    * Initialize the window size by going through the window size and adding 1 (packet in window but not sent) in the indexs
    **/
    public void initializeWindow() {
        for(int i = 0; i < windowSize; i++) {
            packetList.add(i,1);
        }
    }
}
