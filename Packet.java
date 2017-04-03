import java.util.*;

/**
 * A class which represents a packet
 */
public class Packet{
    
    private Message msg; //the enclosed message
    private int seqnum; //packets seq. number
    private int acknum; //packet ack. number 
    private int checksum; //packet checksum

    Random ran; //random number generator

    public Packet(Message msg, int seqnum, int acknum, int checksum){
        this.msg=msg;
        this.seqnum=seqnum;
        this.acknum=acknum;
        this.checksum=checksum;
        this.ran=new Random();
        this.checksum = calculateCheckSum();
    }

    public Packet(Message msg, int seqnum, int acknum){
        this.msg=msg;
        this.seqnum=seqnum;
        this.acknum=acknum;
        this.ran=new Random();
        this.checksum = calculateCheckSum();
    }

    public int getAcknum(){
        return acknum;
    }
    
    public int getSeqnum() {
        return seqnum;
    }

    public Message getMessage(){
        return msg;
    }
    
    //set checksum equal to the calculated checksum
    public void setChecksum(){
        checksum = calculateCheckSum();
    }
    
    //if the calculated check sum is equal to the checksum on packet, then it is not corrupt
    public boolean isCorrupt(){
        if(checksum == calculateCheckSum()){
          return false;
        } else
            return true;
    }
    
    /**
     * This method curropts the packet the follwing way:
     * curropt the message with a 75% chance
     * curropt the seqnum with 12.5% chance
     * curropt the ackum with 12.5% chance
     */
    public void corrupt() {
        if(ran.nextDouble()<0.75)
        {this.msg.corruptMessage();}
        else if(ran.nextDouble()<0.875)
        {this.seqnum=this.seqnum+1;}
        else
        {this.acknum=this.acknum+1;}

    }

/* Here is a method to calculate the checksum of the message and return an int.
* We take the message, extract the string, change it into bytes, then sum the bytes
*/
    public int calculateCheckSum(){
        String message = msg.getMessage();
        byte[] bytes = message.getBytes();
        int sum = bytes[0];
        sum = sum ^ acknum;
        sum = sum ^ seqnum;
        for(int i = 0; i < bytes.length; i++)
        {
            sum = sum ^ bytes[i];
        }

        return sum;
    }
    

}
