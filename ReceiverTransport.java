
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

    //how do you deliver messages to upper layer??

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
    }

    public void initialize()
    {
        pktlist.add(1);
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

            if(pkt.isCorrupt()) //corrupt option not working--always says corrupt. 
            { //if pkt is corrupt, resend ack for highest pkt received 
              //  System.out.println("Packet received is corrupt, sending ack for highest in order packet");
                for(int i = pktlist.size(); i > 0; i--)
                {
                    if(pktlist.get(i) == 2)
                    {
                        Packet resend = new Packet(new Message(" "), -1, i);
                        nl.sendPacket(resend, Event.SENDER);
                        System.out.println("Ack for " + i + " is resent");
                    }
                }
            }
            else
            {//send ack for pkt recieved
                int seqnum = pkt.getSeqnum();
                if(pktlist.get(seqnum) == 1)
                {
                    Packet p = new Packet(new Message(" "), -1, seqnum);
                    nl.sendPacket(p, Event.SENDER);
                    System.out.println("Ack sent for packet " + seqnum);
                    pktlist.set(seqnum,2);
                    pktlist.add(1);
                }
                else
                {
                    for(int i = 0; i < pktlist.size(); i++)
                    {
                        if(pktlist.get(i) == 1)
                        {
                            System.out.println("Resend ack highest in order packet: " + i);
                        }

                    }
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
