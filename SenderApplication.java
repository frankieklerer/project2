import java.util.*;

/**
 * A class which represents the sender's application. the sendMessage will be called at random times.
 */
public class SenderApplication
{
    private SenderTransport st; //transport layer used
    private ArrayList<String> messages; //all messages the application will send
    private int index; //how many messages has the application sent so far
    private Timeline tl; //the timeline associated with the simulation

    // sender application constructor 
    public SenderApplication(ArrayList<String> messages, NetworkLayer nl){
        st = new SenderTransport(nl);
        this.messages=messages;
        index=0;    
    }
    
    // returns the sender transport
    public SenderTransport getSenderTransport(){
        return st;
    }

    // sends the message
    public void sendMessage(){
       st.sendMessage(new Message(messages.get(index++)));
    }
    
    //counts the amount of messages to be sent
    public int messageCount(){
        return messages.size();
    }
    
}
