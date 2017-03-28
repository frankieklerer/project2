
import java.util.ArrayList;
/**
 * A class which represents the sender transport layer
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int n;
    private boolean usingTCP;
    private boolean start = true;
    private ArrayList<Integer> pktlist = new ArrayList<Integer>(50); 

    private ArrayList<Packet> currentWindow;
    //here we create an arraylist to keep track of the sliding window, 
    //each index is for a packet and if the entry is: 
    // 1 -> packet not yet sent, but in sending window
    // 2 -> packet is sent but not yet acked
    // 3 -> packet's ack has been received 
    private ArrayList<Message> msglist = new ArrayList<Message>(50); 

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();

    }

    public void initialize()
    {
        if(usingTCP)
        {

        }
        else
        {
            
        }
        
    }

    public void sendMessage(Message msg)
    {

        
        if(usingTCP)
        {

        }
        else
        {
            moveWindow();
            boolean full = true;
            for(int i = 0; i < pktlist.size(); i++)
            {
                if(pktlist.get(i) == 1)
                {
                    Packet p = new Packet(msg, i, -1);

                    currentWindow.add(p);

                    nl.sendPacket(p, Event.RECEIVER);
                    //tl.startTimer(10);
                    pktlist.set(i,2);
                    msglist.add(msg);
                    full = false;
                    System.out.println("Message sent in packet: " + i);
                    break;
                }
            }
            if(full)
            {
                System.out.println("Window is full");
            }
        }
        

    }

    public void receiveMessage(Packet pkt)
    {
        if(usingTCP)
        {

        }
        else
        {
            int a = pkt.getAcknum();
           // tl.stopTimer();
            if(pkt.isCorrupt())
            {//if pkt is corrupt, resend all sent but unacked messages
                System.out.println("Packet was corrupt, resend all sent but unacked packets");
                for(int x = 0; x < pktlist.size(); x++)
                {
                    if(pktlist.get(x) == 2)
                    {
                        Packet temp = new Packet(msglist.get(x), x, -1);
                        nl.sendPacket(temp, Event.RECEIVER);
                       // tl.startTimer(10);
                        System.out.println("Resent packet: " + x);
                    }
                }
               // tl.startTimer(10);
            }
            else if(pktlist.get(a) == 2) //if the pkt has not been acked yet
            { //change the pkt to be acked
                pktlist.set(a,3);
                System.out.println("Ack received for packet " + a);
                for(int i = 0; i < a; i++) //accounts for cumulative acks
                {
                    if(pktlist.get(i) == 2 && i != a) 
                    {
                        pktlist.set(i,3);
                        System.out.println("Cumulatively acking packet " + a);
                    }
                }
                moveWindow(); 
            }
            else 
            { //else resend all sent but unacked pkts
                System.out.println("Ack received for out of order packet, resend all sent but unacked packets");
                for(int j = 0; j < pktlist.size(); j++)
                {
                    if(pktlist.get(j) == 2)
                    {
                        Packet resend = new Packet(msglist.get(j), j, -1);
                        nl.sendPacket(resend, Event.RECEIVER);
                      //  tl.startTimer(10);
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
            for(int j = 0; j < pktlist.size(); j++)
            {
                if(pktlist.get(j) == 2)
                {
                    Packet resend = new Packet(msglist.get(j), j, -1);
                    nl.sendPacket(resend, Event.RECEIVER);
                    tl.startTimer(10);
                    System.out.println("Resent packet " + j);
                }
            }
        }
    }

    public void setTimeLine(Timeline tl)
    {
        this.tl=tl;
    }

    public void setWindowSize(int n)
    {
        this.n=n;
        this.currentWindow = new ArrayList<Packet>(this.n);
        this.initializeWindow();
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

    public void initializeWindow() {


        for(int i = 0; i < n; i++)
        {
            pktlist.add(1);
        }


    }

    public void moveWindow()
    {
       
        for(int i = 0; i < pktlist.size(); i++)
        {
            if(pktlist.get(i) == 1 || pktlist.get(i) == 2)
                continue;
            else if(pktlist.get(i) == 3)
            {
                if(pktlist.get(i+1) == 3)
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
}
