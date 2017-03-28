
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer nl;
    private boolean usingTCP;

    private ArrayList<Integer> pktlist = new ArrayList<Integer>(50);
    //this will represent the pkt list, it will tell us the status of pkts
    // 1 = in current window
    // 2 = pkt received and ack sent

    private ArrayList<Packet> currentWindow;
    private ArrayList<Packet> bufferedPacketList = new ArrayList<PAcket>(50); 


    //for tcp to buffer out or order messages 
    private int gbnwindow;

    //how do you deliver messages to upper layer??

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
    }

    public void initialize()
    {
        gbnwindow = 0;
    }

    public void receiveMessage(Packet pkt)
    { //out of order pkts?
        if(usingTCP)
        {
            if(pkt.isCorrupt()){ // resend most recent ack
                for(int i = pktlist.size(); i > 0; i-- ){

                    if(pktlist.get(i) == 2){
                        Packet p9 = new Packet(new Message(" "), -1, i);
                        nl.sendPacket(p9, Event.SENDER);
                        System.out.println("Packet is corrupt, resend ack highest in order packet: " + i));
                    } 
                }

            } else {
                int seqnum = pkt.getSeqnum();
                boolean waiting = false;
                int 

                for(int i = 0; i < seqnum; i++){
                    // a packet before the one you have just received is still waiting to be received
                    if(pktlist.get(i) == 1){
                        waiting = true;
                        // ack
                        bufferedPacketList.add(pkt);

                        if(i == 0) { // account for the first case
                            Packet p = new Packet(pkt.getMessage(), -1, 0);
                        }

                        Packet p = new Packet(pkt.getMessage(), -1, i-1);
                        nl.sendPacket(p, Event.SENDER);
                    }
                }

                if(!waiting){
                    Packet p11 = new Packet(pkt.getMessage(), -1, seqnum);
                    nl.sendPacket(p11, Event.SENDER);
                    pktlist.set(seqnum, 2);
                }
                moveWindow();
                updateBuffer();
            }

        }
        else{
            if(pkt.isCorrupt()) 
            { //if pkt is corrupt, resend ack for highest pkt received 
              //  System.out.println("Packet received is corrupt, sending ack for highest in order packet");
                if(gbnwindow == 0)
                    {
                        Packet p6 = new Packet(new Message(" "), -1, 0);
                        nl.sendPacket(p6, Event.SENDER);
                    }
                Packet p2 = new Packet(new Message(" "), -1, gbnwindow-1);
                nl.sendPacket(p2, Event.SENDER);
                System.out.println("Packet is corrupt, resend ack highest in order packet: " + (gbnwindow-1));
            }
            else
            {//send ack for pkt recieved
                int seqnum = pkt.getSeqnum(); 
                if(gbnwindow == seqnum)
                {
                    Packet p = new Packet(pkt.getMessage(), -1, seqnum);
                    nl.sendPacket(p, Event.SENDER);
                    gbnwindow++;
                    msglist.add(pkt.getMessage());
                    System.out.println("Ack sent for packet " + seqnum);
                    pktlist.set(seqnum,2);
                    pktlist.add(1);
                }
                else
                {
                    if(gbnwindow == 0)
                    {
                        Packet p3 = new Packet(new Message(" "), -1, 0);
                        nl.sendPacket(p3, Event.SENDER);
                    }
                    Packet p4 = new Packet(msglist.get(gbnwindow-1), -1, gbnwindow-1);
                    nl.sendPacket(p4, Event.SENDER);
                    System.out.println("Out of Order Pkt receieved, resend ack for highest in order packet: " + (gbnwindow-1));
                }
            }       
        }      
    }

     public void setWindowSize(int n)
    {
        this.n=n;
        this.curentWindow = new ArrayList<Packet>(this.n);
        this.initializeWindow();
    }

     public void initializeWindow() {
        for(int i = 0; i < n; i++)
        {
            pktlist.add(1);
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
            if(pktlist.get(j) == 1){
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
       
        for(int i = 0; i < pktlist.size(); i++)
        {
            if(pktlist.get(i) == 1 )
                continue;
            else if(pktlist.get(i) == 2)
            {
                if(pktlist.get(i+1) == 2)
                    continue;
                else
                {
                    for(int j = i+1; j < i+1+n; j++)
                        pktlist.add(1);
                }   
            }
            else
                continue;
        }
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

}
