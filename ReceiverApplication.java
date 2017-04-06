
/**
 * A class which represents the receiver's application. It simply prints out the message received from the tranport layer.
 */
public class ReceiverApplication{

	// the receivers application layer is delivered messages using this method which is called in the receivers transport layer class
    public void receiveMessage(Message msg){
        System.out.println("Receiver Application has received message " + msg.getMessage());
    }

}
