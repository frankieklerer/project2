
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer networkLayer;
    private boolean usingTCP;
    private int windowSizeGBN;
    private int windowSizeTCP;

    /**
    * Array list to keep track of the sliding window
    * The indexs indicate the packet sequence number and the entries indicate the packet status:
    * 1 means awaiting the packet in the current window 
    * 2 means the packet has been received and ACKed
    **/
    private ArrayList<Integer> packetStatusCode; 

    // Array list of Packet objects to keep track of which packets are in the current window
    private ArrayList<Packet> currentWindow; 

    private ArrayList<Packet> bufferedPacketList;  

    public ReceiverTransport(NetworkLayer networkLayer){
        ra = new ReceiverApplication();
        this.networkLayer=networkLayer;
        this.packetStatusCode = new ArrayList<Integer>();
        this.bufferedPacketList = new ArrayList<Packet>();
        initialize();
    }

    public void initialize(){
        this.initializeWindow();
    }

    public void receiveMessage(Packet pkt){ //out of order pkts?
        if(usingTCP){
            if(pkt.isCorrupt()){ // resend most recent ack
                for(int i = 0; i < packetStatusCode.size(); i++ ){
                    if(packetStatusCode.get(i) == 2){
                        Packet p9 = new Packet(new Message(" "), -1, i);
                        networkLayer.sendPacket(p9, Event.SENDER);
                        System.out.println("Packet is corrupt, resend ack highest in order packet: " + i);
                    } 
                }

            } 
            else {
                int seqnum = pkt.getSeqnum();
                boolean waiting = false;
                

                for(int i = 0; i < seqnum; i++){
                    // a packet before the one you have just received is still waiting to be received
                    if(packetStatusCode.get(i) == 1){
                        waiting = true;
                        
                        bufferedPacketList.add(pkt);

                        Packet p = new Packet(pkt.getMessage(), -1, i+1);
                        networkLayer.sendPacket(p, Event.SENDER);
                    }
                }

                if(!waiting){
                    Packet p11 = new Packet(pkt.getMessage(), -1, seqnum+1);
                    networkLayer.sendPacket(p11, Event.SENDER);
                    packetStatusCode.set(seqnum, 2);
                }
                moveWindow();
                updateBuffer();
            }

        }

        else{

            int packetSeqNum = pkt.getSeqnum();
            System.out.println("Receiver has just received packet " + packetSeqNum);
            int highestSeqNumACKed=0;
            Packet resend=null;
            packetStatusCode.add(1);

            // if the packet is corrupt, resend ACK for highest seqnum of packet with received ACK
            if(pkt.isCorrupt()) { 

                System.out.println("Received packet is corrupt, sending ack for highest in order packet");

                // if the first packet is corrupted
                if(packetSeqNum == 0){
                    // resend first packet (-1 because no seq num in ack)
                    resend = new Packet(new Message(" "), -1, 0);
                    networkLayer.sendPacket(resend, Event.SENDER);
                }
                else
                {
                    // find the sequence number of the highest ACKed packet
                    for (int i = packetSeqNum; i > 0; i--){
                        if(packetStatusCode.get(i) == 2){
                            highestSeqNumACKed = i;
                            break;
                        }
                    }

                    // resend packet with last highest ack (-1 because no seq num in ack)
                    resend = new Packet(new Message(" "), -1, highestSeqNumACKed);
                    networkLayer.sendPacket(resend, Event.SENDER);
                    System.out.println("ACK for packet " + highestSeqNumACKed + " has been resent because it was corrupt.");
                }   
            }else{
                boolean gap = false;
                System.out.println("Packet " + packetSeqNum + " is not corrupt.");
                // if the packet is not corrupt
                // send an ack only if there is no gap (an unACKed packet) before the received packet


                 for(int i = 0; i < packetSeqNum; i++){
                    // if there is an unACKed packet before this
                    if(packetStatusCode.get(i) == 1){

                        int highestinOrderAck = i-1;

                        if(highestinOrderAck == -1)
                        {
                            resend = new Packet(new Message(" "), -1, 0);
                            networkLayer.sendPacket(resend, Event.SENDER);
                            System.out.println("ACK for packet " + 0 + " has been resent because gap in ACKs.");
                            gap = true;
                        }
                        else{
                            // resend ACK for most recently ACKed packet
                            // resend packet with last highest ack (-1 because no seq num in ack)
                            resend = new Packet(new Message(" "), -1, highestinOrderAck);
                            networkLayer.sendPacket(resend, Event.SENDER);
                            System.out.println("ACK for packet " + highestinOrderAck + " has been resent because gap in ACKs.");
                            gap = true;
                        }   
                    }
                }
                if(!gap)
                {
                      // if all packets before it have been ACKed
                    Packet packetACK = new Packet(new Message(" "), -1, packetSeqNum);
                    networkLayer.sendPacket(packetACK, Event.SENDER);
                    packetStatusCode.set(packetSeqNum, 2);
                    System.out.println("ACK sent for Packet " + packetSeqNum);
                    ra.receiveMessage(pkt.getMessage());
                }
            }       
        }      
    }

    // true if all packets in sequence have been received
    // false if waiting for packet so there are buffered packets
    public boolean checkBuffer(){
        int highestSeqNum=-1;
        for(int i = 0; i < bufferedPacketList.size(); i++){
            Packet p = bufferedPacketList.get(i);
            if(p.getSeqnum() > highestSeqNum){
                highestSeqNum = p.getSeqnum();
            }

        }

        for(int j =0; j < highestSeqNum; j++){
            if(packetStatusCode.get(j) == 1){
                return false;
            }
        }

        return true;
    }

    public void updateBuffer(){
        if(checkBuffer()){
            //send all packets to upper layer
        } else {

        }
    }

     public void moveWindow(){
        // 1 = in receiver window but nothing received
        // 2 = in receiver window, recevied packet and sent ACK
       
        for(int i = 0; i < packetStatusCode.size(); i++)
        {
            if(packetStatusCode.get(i) == 1 )
                continue;
            else if(packetStatusCode.get(i) == 2)
            {
                if(packetStatusCode.get(i+1) == 2)
                    continue;
                else
                {
                    for(int j = i+1; j < i+1+windowSizeTCP; j++)
                        packetStatusCode.add(1);
                }   
            }
            else
                continue;
        }
    }


     /**
    * Set the window size from the program parameters
    **/
    public void setWindowSize(int windowSize){
        if(usingTCP)
        {
            this.windowSizeTCP=windowSize;
            this.currentWindow = new ArrayList<Packet>(this.windowSize);
            this.initializeWindow();
        }
        else
        {
            this.initializeWindow();
        }
      
    }

    public void initializeWindow() {
        if(usingTCP)
        {
            for(int i = 0; i < windowSizeTCP; i++){
                packetStatusCode.add(1);
            }
        }
        else{
            //this.currentWindow = new ArrayList<
            this.windowSizeGBN = 1;
            for(int i = 0; i < windowSizeGBN; i++){
                packetStatusCode.add(1);
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
