
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer networkLayer;

    // boolean indicating whether TCP is being used or not
    private boolean usingTCP;
    
    // window size of GBN receiver
    private int windowSizeGBN;

    // the sequence number that gbn is expecting 
    private int gbnExpectedSeq;

    // window size of TCP receiver
    private int windowSizeTCP;

    // the sequence number the TCP sender is expected (should be the one its about to send out)
    private int tcpExpectedSeq;

    /**
    * Array list to keep track of the sliding window
    * The indexs indicate the packet sequence number and the entries indicate the packet status:
    * 1 means awaiting the packet in the current window 
    * 2 means the packet has been received and ACKed
    **/
    private ArrayList<Integer> packetStatusCode; 

    private ArrayList<Boolean> sentToApp;

    // Array list of Packet objects to keep track of which packets are in the current window
    private ArrayList<Packet> currentWindow; 

    // array list of buffered packets for TCP
    private ArrayList<Packet> bufferedPacketList;  

    /**
    * Constructor of receiver transport which intializes array lists and various objects
    **/
    public ReceiverTransport(NetworkLayer networkLayer){
        ra = new ReceiverApplication();
        this.networkLayer=networkLayer;
        this.packetStatusCode = new ArrayList<Integer>();
        this.bufferedPacketList = new ArrayList<Packet>();
        this.sentToApp = new ArrayList<Boolean>();
        initialize();
    }

    /**
    * Initialize method for receiver
    **/
    public void initialize(){
        this.initializeWindow();
        this.gbnExpectedSeq = 0;
        this.tcpExpectedSeq=0;
    }

    /**
    * Receiver message method which knows what to do when the receiver receives a packet
    **/
    public void receiveMessage(Packet pkt){ 
        // if protocol is TCP
        if(usingTCP){
           
            // if the packet is corrupt, resend most recent ACK
            if(pkt.isCorrupt()){ 

                System.out.println("Received packet is corrupt, sending ack for highest in order packet");

                // find the packet whose sequence number you are expecting, one minus that is the packet that needs to be reACKed

                // if you are expecting the first packet and it is corrupt
                if(tcpExpectedSeq == 0){

                    //resend it
                    Packet resend = new Packet(new Message(" "), -1, 0);
                    networkLayer.sendPacket(resend, Event.SENDER);
                    System.out.println("ACK for 0 has been resent because received packet is corrupt");
         
                } else { //if you are expecting a packet greater than 0

                    int highestACK = 0;

                    // for every packet in the packet status code
                    for(int i = 0 ; i < packetStatusCode.size(); i++){

                        // retrieve its current status
                        int temp = packetStatusCode.get(i);

                        // if it has a status of 2 (received and acked) and if the index is higher then current highest ACK
                        if( (temp == 2) && (i > highestACK)){

                            // update the highest ACked index
                            highestACK = i;
                        }
                    }

                    int lastACKedPacketSeqNum;

                    // if the highest ACK is the first packet
                    if(highestACK == 0){

                        // will sent an ack for the first packet
                        lastACKedPacketSeqNum = 0;
                    }else{

                        // increment it so it acks the one its expecting
                        lastACKedPacketSeqNum = highestACK + 1;
                    }
                    

                    // resend that last ACKed packet
                    Packet resend = new Packet(new Message(" "), -1, lastACKedPacketSeqNum);
                    networkLayer.sendPacket(resend, Event.SENDER);
                    System.out.println("ACK for " + lastACKedPacketSeqNum + " has been resent because received packet is corrupt");
                }
                
            // if the packet is not corrupt
            } else {

                Packet resendTCP=null;

                // The highest sequence number that already been ACKed
                int highestSeqNumACKedTCP=0;

                // add a 1 to the next slot
                packetStatusCode.add(1);
                sentToApp.add(false);

                // get the sequence number of the packet
                int packetSeqNumTCP = pkt.getSeqnum();

                // no buffer
                boolean waiting = false; 

                System.out.println("Receiver has just received packet " + packetSeqNumTCP);


                // for every packet before the received packet
                for(int i = 0; i < packetSeqNumTCP; i++){

                    // if there is a packet before it whose ACK you are awaiting
                    if((packetStatusCode.get(i) == 1) && (packetSeqNumTCP != i)){

                        // must buffer the packet
                        waiting = true;
                        
                        // add the packet to the buffered list
                        bufferedPacketList.add(pkt);
                        System.out.println("Buffering packet " + pkt.getSeqnum() );

                        int highestACK = 0;

                        //find the highest acked packet and resend the ack for it
                        for(int j = 0 ; j < packetStatusCode.size(); j++){
                            int temp = packetStatusCode.get(i);

                            if( (temp == 2) && (j > highestACK)){
                                highestACK = j+1;
                            }
                        }

                        // ack the next packet you are expecting
                        highestSeqNumACKedTCP = highestACK+1;

                        // resend packet with last highest ack (-1 because no seq num in ack)
                        resendTCP = new Packet(new Message(" "), -1, highestSeqNumACKedTCP);
                        networkLayer.sendPacket(resendTCP, Event.SENDER);
                        System.out.println("ACK for " + highestSeqNumACKedTCP + " has been resent because of a gap in the window.");
                    }
                }

                //if did not have to buffer a packet but none of the previous cases have been met
                if(!waiting){

                    //if ack has already been sent for packet, resend highest acked packet
                    if(packetStatusCode.get(packetSeqNumTCP) == 2){

                        int highestACK = 0;

                        for(int i = 0 ; i < packetStatusCode.size(); i++){
                            int temp = packetStatusCode.get(i);

                            if( (temp == 2) && (i > highestACK)){
                                highestACK = i;
                            }
                        }

                        int lastACKedPacketSeqNum = highestACK+1;

                        // resend packet with last highest ack (-1 because no seq num in ack)
                        resendTCP = new Packet(new Message(" "), -1, lastACKedPacketSeqNum);
                        networkLayer.sendPacket(resendTCP, Event.SENDER);
                        System.out.println("ACK for " + lastACKedPacketSeqNum + " has been resent becauce receiver has already received this packet");

                    }else if(packetStatusCode.get(packetSeqNumTCP) == 1){ // if you receive a packet that has not yet been acked

                        // the ack the receiver needs to send is one plus the received seq num
                        int ackNumToSend = packetSeqNumTCP+1;

                        // send the packet
                        Packet packetACKTCP = new Packet(new Message(" "), -1, ackNumToSend);
                        networkLayer.sendPacket(packetACKTCP, Event.SENDER);
                        System.out.println("ACK sent for " + ackNumToSend);

                        // set status code to ACKed for packet
                        packetStatusCode.set(packetSeqNumTCP, 2);
                        tcpExpectedSeq++;

                        if(sentToApp.get(packetSeqNumTCP) == false)
                        {
                            ra.receiveMessage(pkt.getMessage());
                            sentToApp.set(packetSeqNumTCP, true);
                        }
                        
                    }
                   
                }

                // if the buffered packet list is currently buffering a couple packets, update the buffer
                if(!bufferedPacketList.isEmpty()){
                   updateBuffer();
                }
            }

        }else{ // if using GBN

            // if the packet is corrupt, resend ACK for highest seqnum of packet with received ACK
            if(pkt.isCorrupt()) {

                System.out.println("Received packet is corrupt, sending ack for highest in order packet");

                // find the packet whose sequence number you are expecting, one minus that is the packet that needs to be reACKed
                int lastACKedPacketSeqNum = gbnExpectedSeq-1;

                // resend that last ACKed packet
                Packet resend = new Packet(new Message(" "), -1, lastACKedPacketSeqNum);
                networkLayer.sendPacket(resend, Event.SENDER);
                System.out.println("ACK for packet " + lastACKedPacketSeqNum + " has been resent because received packet was corrupt.");

            }else{ // if the packet is not corrupt

                // get the sequence number of the packet you just received
                int packetSeqNum = pkt.getSeqnum();
                System.out.println("Receiver has just received packet " + packetSeqNum);

                // possible packet to resend
                Packet resend=null;

                // add a status code of waiting for sender application to end of list
                packetStatusCode.add(1);
                sentToApp.add(false);

                // currently no gap
                boolean gap = false;
                System.out.println("Packet " + packetSeqNum + " is not corrupt.");

                // send an ack only if there is no gap (an unACKed packet) before the received packet

                // for packet until the received on
                for(int i = 0; i < packetSeqNum; i++){

                    // if there is an unACKed packet before this and no gap
                    if((packetStatusCode.get(i) == 1) && !gap ){

                        // find highest in order ACK
                        int highestinOrderAck = i-1;

                        // resend that ack
                        resend = new Packet(new Message(" "), -1, highestinOrderAck);
                        networkLayer.sendPacket(resend, Event.SENDER);
                        System.out.println("ACK for packet " + highestinOrderAck + " has been resent because gap in packets.");

                        // notify program that it has found a gap 
                        gap = true;
                    }
                }

                if(!gap){ // if no gap was found so that all packects before this one have been ACKed
                    
                    if(packetStatusCode.get(packetSeqNum) == 2) { // if current packet has already been ACKed

                        // find highest in order ACK
                        int highack = gbnExpectedSeq-1;

                        // resend that ack
                        resend = new Packet(new Message(" "), -1, highack);
                        networkLayer.sendPacket(resend, Event.SENDER);
                        System.out.println("ACK for packet " + highack + " has been resent because receiver has already received packet " + packetSeqNum);

                    }else if(packetStatusCode.get(packetSeqNum) == 1) { // if current packet hasn't been ACKed yet
                        // send an ACK
                        Packet packetACK = new Packet(new Message(" "), -1, packetSeqNum);
                        networkLayer.sendPacket(packetACK, Event.SENDER);

                        // set it in status code
                        packetStatusCode.set(packetSeqNum, 2);
                        System.out.println("ACK sent for Packet " + packetSeqNum);

                        // send the message to receiver application
                        if(sentToApp.get(packetSeqNum) == false)
                        {
                            ra.receiveMessage(pkt.getMessage());
                            sentToApp.set(packetSeqNum, true);
                        }

                        // increment the next expected sequence number
                        gbnExpectedSeq++;
                    }
                    
                }
            }       
        } 

    }

    // true if all packets in sequence have been received
    // false if waiting for packet so there are buffered packets
    public boolean checkBuffer(){
        boolean allReceived = false;

        int highestSeqNumInBuffer =0;

        // look through the buffered packet list and find the highest seq num 
        for(int i = 0; i < bufferedPacketList.size(); i++){
            int temp = bufferedPacketList.get(i).getSeqnum();

            if(temp > highestSeqNumInBuffer){
                highestSeqNumInBuffer = temp;
            }
        }

        // for all the packets in the packet list up until the highest seq num being buffered
        for(int j = 0; j < highestSeqNumInBuffer; j++){

            // retrieve the packet object
            int check = packetStatusCode.get(j);

            // if all packets before it have been received, true
            if(check == 2){
                allReceived = true;
            } else { 
                // not all packets before it have been received
                return false;
            }
        }

        return allReceived;
    }

    // method to update the buffer periodically
    public void updateBuffer(){

        // if all packets before the highest seq num in the buffer packet list have been ACKed
        if(checkBuffer()){

            // deliver all those packets to the receiver application
            for(int i = 0; i < bufferedPacketList.size(); i ++){
                Packet toDeliver = bufferedPacketList.get(i);
                if(sentToApp.get(toDeliver.getSeqnum()) == false)
                        {
                            ra.receiveMessage(toDeliver.getMessage());
                            sentToApp.set(toDeliver.getSeqnum(), true);
                        }
            }

        } else { // don't touch buffer if not all packets have been acked
            System.out.println("The buffer is still waiting for in order packets to arrive.");
        }
    }

     /**
    * Set the window size from the program parameters
    **/
    public void setWindowSize(int windowSize){
        if(usingTCP){
            this.windowSizeTCP=windowSize;
            this.currentWindow = new ArrayList<Packet>(this.windowSizeTCP);
            this.initializeWindow();
        }else{
            this.initializeWindow();
        }
    }

    // method to initliaze receiver window
    public void initializeWindow() {

        if(usingTCP){

            // for the window size, set packet status code to 1 for that
            for(int i = 0; i < windowSizeTCP; i++){
                packetStatusCode.add(1);
                sentToApp.add(false);
            }
        }else{

            //gbn receiver has window size of 1
            this.windowSizeGBN = 1;

            // for the window size, set packet status code to 1 for that
            for(int i = 0; i < windowSizeGBN; i++){
                packetStatusCode.add(1);
                sentToApp.add(false);
            }
            ///System.out.println(packetStatusCode);
        }
        
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
}
