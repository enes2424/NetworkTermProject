import java.io.IOException;
import java.io.File;

import java.nio.file.Path;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.gson.Gson;

import java.security.NoSuchAlgorithmException;

public class UDPFlooding {
    private static final int    PORT = 9876;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final int    CHUNK_SIZE = 256 * 1024 + 1024;
    private DatagramSocket	    senderSocket;
    private DatagramSocket	    receiverSocket;
    private byte[]              buffer = new byte[CHUNK_SIZE];
    private DatagramPacket      packet = new DatagramPacket(buffer, buffer.length);
    private boolean			    running;
    private Gson                gson = new Gson();
    private P2P				    p2p;

    public UDPFlooding(P2P p2p) {
    	this.p2p = p2p;
        this.running = false;
    }


    public void connect() {
    	this.running = true;
    	new Thread(this::startSender).start();
    	new Thread(this::startReceiver).start();
    }
    
    public void	disconnect() {
        try {
            sendFoundFiles("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    	this.running = false;
    	senderSocket.close();
    	receiverSocket.close();
    }

    private void sendFoundFiles(String message) throws IOException {
        byte[] buffer = ("FOUND " + message).getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(BROADCAST_ADDRESS), PORT);

        senderSocket.send(packet);
        System.out.println("Broadcast message sent: " + message);
    }

    private void startSender() {
        try {
        	senderSocket = new DatagramSocket();
        	senderSocket.setBroadcast(true);

            x: while (running) {
                String message = p2p.getMessage();
                if (!message.equals("")) {
                    File folder = new File(message);
                    Path baseFolderPath = folder.toPath();
                    File[] files = folder.listFiles();
                    StringBuilder sb = new StringBuilder();
                    for (File file : files) {
                        sb.append(baseFolderPath.relativize(file.toPath()).toString());
                        sb.append(',');
                    }
                    sb.append(';');
                    for (File file : files) {
                        try {
                            String jsonData = gson.toJson(FolderComparison.getAllFileHashes(file));
                            sb.append(jsonData);
                            sb.append('|');
                        } catch (IOException | NoSuchAlgorithmException e) {
                            System.err.println("An error occurred: " + e.getMessage());
                            continue x;
                        }
                    }
                    sendFoundFiles(sb.toString());
                } else
                    sendFoundFiles("");
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

    private void startReceiver() {
        try {
        	receiverSocket = new DatagramSocket(PORT);

            while (running) {
            	receiverSocket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                if (receivedMessage.startsWith("FOUND ")) {
                    receivedMessage = receivedMessage.substring(6);
                    // packet.getAddress() bazen ipv6 olarak geliyor onu duzelt
                    System.out.println("Message received: " + receivedMessage + " from " + packet.getAddress());
                    p2p.addElementToFoundList(packet.getAddress().toString(), receivedMessage);
                } else if (receivedMessage.startsWith("DOWNLOAD ")) {

                }
            }
            System.out.println("Close receiver socket");
        } catch (IOException e) {
            if (e.getMessage().equals("Socket closed"))
            	System.out.println("Close receiver socket");
            else
            	e.printStackTrace();
        }
    }
}
