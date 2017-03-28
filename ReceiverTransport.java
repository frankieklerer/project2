
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
    private ArrayList<Message> msglist = new ArrayList<Message>(50); 
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
        pktlist.add(1);
        gbnwindow = 0;
    }

    public void receiveMessage(Packet pkt)
    { //out of order pkts?
        if(usingTCP)
        {

        }
        else{
                // int seqnum = pkt.getSeqnum();
                // msglist.add(pkt.getMessage());
                // Packet p = new Packet(pkt.getMessage(), -1, seqnum);
                // nl.sendPacket(p, Event.SENDER);
                // System.out.println("Ack sent for packet " + seqnum);

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

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

}
