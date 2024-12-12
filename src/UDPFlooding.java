import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPFlooding {
    private static final int PORT = 9876;
    private static final String BROADCAST_ADDRESS = "172.18.255.255";
    private DatagramSocket	senderSocket;
    private DatagramSocket	receiverSocket;
    private boolean			running;
    private	String			message;
    private P2P				p2p;

    public UDPFlooding(P2P p2p) {
    	this.p2p = p2p;
        this.running = false;
    }
    
    public void setMessage(String message) {
    	this.message = message;
    }

    public void connect(String message) {
    	this.message = message;
    	this.running = true;
    	new Thread(this::startSender).start();
    	new Thread(this::startReceiver).start();
    }
    
    public void	disconnect() {
    	senderSocket.close();
    	receiverSocket.close();
    	this.running = false;
    }

    public void startSender() {
        try {
        	senderSocket = new DatagramSocket();
        	senderSocket.setBroadcast(true);

            while (running) {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(BROADCAST_ADDRESS), PORT);

                senderSocket.send(packet);
                System.out.println("Broadcast message sent: " + message);
                for (int i = 0; running && i < 10; i++)
                	Thread.sleep(300);
            }
            System.out.println("Close sender socket");
        } catch (IOException | InterruptedException e) {
        	if (e.getMessage().equals("Socket closed"))
            	System.out.println("Close sender socket");
            else
            	e.printStackTrace();
        }
    }

    public void startReceiver() {
        try {
        	receiverSocket = new DatagramSocket(PORT);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running) {
            	receiverSocket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message received: " + receivedMessage + " from " + packet.getAddress());
                p2p.addElementToFoundList(receivedMessage + " from " + packet.getAddress());
            }
            
        } catch (IOException e) {
            if (e.getMessage().equals("Socket closed"))
            	System.out.println("Close receiver socket");
            else
            	e.printStackTrace();
        }
    }
}
