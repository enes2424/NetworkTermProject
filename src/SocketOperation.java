import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;

import java.nio.file.Paths;
import java.nio.file.Path;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import com.google.gson.Gson;

import java.security.NoSuchAlgorithmException;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

public class SocketOperation {
    private static final int    		UDP_PORT = 9090;
    private static final int    		TCP_PORT = 8080;
    private static final String 		BROADCAST_ADDRESS = "255.255.255.255";
    private static final int			CHUNK_SIZE = 256 * 1024;
    private final Lock 					uploadLock = new ReentrantLock();
    private final Lock 					confirmLock = new ReentrantLock();
    private final Lock 					downloadLock = new ReentrantLock();
    private DatagramSocket	    		senderSocket;
    private DatagramSocket	    		udpReceiverSocket;
    private ServerSocket				tcpReceiverSocket;
    private byte[]              		buffer = new byte[65535];
    private DatagramPacket      		packet = new DatagramPacket(buffer, buffer.length);
    private boolean			    		running;
    private Gson                		gson = new Gson();
    private P2P				    		p2p;
    private boolean						isTheSenderWorking = false;
    private int							uploadThreadNum = 0;
    public int							downloadThreadNum = 0;
    public ArrayList<Long>				uploadIDControlList = new ArrayList<>();
    public ArrayList<SmallInformation>	confirmIDControlList = new ArrayList<>();
    public ArrayList<byte[]>			downloadIDControlList = new ArrayList<>();

    public SocketOperation(P2P p2p) {
    	this.p2p = p2p;
        this.running = false;
    }

    public void connect() {
    	this.running = true;
    	new Thread(this::shareFolder).start();
    	new Thread(this::udpReceiver).start();
    	new Thread(this::tcpReceiver).start();
    }

    public void	disconnect() {
        try {
        	sendMessage("FOUND ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    	this.running = false;
    	senderSocket.close();
    	udpReceiverSocket.close();
    	try {
    		tcpReceiverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	public void addIDControlList() {
		confirmLock.lock();
		confirmIDControlList.add(null);
		confirmLock.unlock();
		downloadLock.lock();
		downloadIDControlList.add(null);
		downloadLock.unlock();
	}

	private static boolean createFile(String basePath, String relativePath) {
        try {
        	File file = new File(basePath, relativePath);
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void download(String destinationFolder, int index, long totalByte, String message, int id) throws IOException, InterruptedException {
    	long downloadedBytes = 0;
    	long readedBytes = 0;
		@SuppressWarnings("unchecked")
		List<List<String>> data = gson.fromJson(message, List.class);
    	List<String> paths = data.get(0);
    	List<String> bytes = data.get(1);
    	SmallInformation si;
    	int indexOfPaths = 0;
    	long targetByteCount = Long.parseLong(bytes.get(0));
    	if (!createFile(destinationFolder, paths.get(0))) {
    		JOptionPane.showMessageDialog(p2p, "An error occurred while downloading!", "Download Cancelled", JOptionPane.ERROR_MESSAGE);
    		return;
    	}

    	FileOutputStream fos = new FileOutputStream(Paths.get(destinationFolder, paths.get(0)).toFile());

    	int 		control;
    	int			targetID;
    	String 		targetAddress;
 
    	x: while (downloadedBytes != totalByte) {
    		confirmLock.lock();
	    	confirmIDControlList.set(id, null);
	    	confirmLock.unlock();
	    	sendMessage("CONTROL " + downloadedBytes + ";" + id + ";" + message);
	    	control = 0;
	    	while (true) {
	    		if (!this.running)
	    			throw new IOException();
	    		confirmLock.lock();
	    		si = confirmIDControlList.get(id);
	    		confirmLock.unlock();
	    		if (si != null) {
	    			if (si.getDowloadedBytes() == downloadedBytes)
	    				break;
	    			else {
	    				confirmLock.lock();
	    				confirmIDControlList.set(id, null);
	    				confirmLock.unlock();
	    			}
	    		}
	    		if (++control == 20)
	    			continue x;
	    		Thread.sleep(300);
	    	}
	    	targetID = si.getNum();
	    	targetAddress = si.getAddress();
			System.out.println("REQUEST " + downloadedBytes + ";" + targetID + " to " + targetAddress);
    		if (!this.running)
    			throw new IOException();
	    	if (!sendPrivateMessage("REQUEST " + downloadedBytes + ";" + targetID, targetAddress))
	    		continue x;
	    	control = 0;
	    	downloadLock.lock();
	    	while (downloadIDControlList.get(id) == null) {
	    		downloadLock.unlock();
	    		if (++control == 30)
	    			continue x;
	    		Thread.sleep(300);
	    		downloadLock.lock();
	    	}
	    	byte[]	buffer = downloadIDControlList.get(id);
	    	downloadLock.unlock();
	    	if (readedBytes + buffer.length > targetByteCount) {
		    	while (true) {
		    		fos.write(buffer, 0, (int) (targetByteCount - readedBytes));
		    		fos.close();
		    		if (++indexOfPaths == paths.size())
		    			break;
		        	if (!createFile(destinationFolder, paths.get(indexOfPaths))) {
		        		JOptionPane.showMessageDialog(p2p, "An error occurred while downloading", "Download Cancelled", JOptionPane.ERROR_MESSAGE);
		        		return;
		        	}
		        	fos = new FileOutputStream(Paths.get(destinationFolder, paths.get(indexOfPaths)).toFile());
		        	long newTargetByteCount = Long.parseLong(bytes.get(indexOfPaths));
		        	if (buffer.length - (targetByteCount - readedBytes) <= newTargetByteCount) {
		        		fos.write(buffer, (int) (targetByteCount - readedBytes), (int) (buffer.length - (targetByteCount - readedBytes)));
			        	readedBytes = (buffer.length - (targetByteCount - readedBytes));
			        	targetByteCount = newTargetByteCount;
			        	break;
		        	}
		        	buffer = Arrays.copyOfRange(buffer, (int) (targetByteCount - readedBytes), (int) buffer.length);
		        	readedBytes = 0;
		        	targetByteCount = newTargetByteCount;
		    	}
	    	} else {
	    		fos.write(buffer, 0, buffer.length);
	    		readedBytes += buffer.length;
	    	}
	    	downloadedBytes += buffer.length;
	    	p2p.setPercentage(index, (1.0 * downloadedBytes) / totalByte);
	    	downloadLock.lock();
	    	downloadIDControlList.set(id, null);
	    	downloadLock.unlock();
    	}
    	fos.close();
    }

    private void sendMessage(String message) throws IOException {
    	byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(BROADCAST_ADDRESS), UDP_PORT);

        senderSocket.send(packet);
    }
    
    private boolean sendPrivateMessage(String message, String address) throws IOException {
    	Socket socket = null;
    	try {
    		System.out.println("PRIVATE " + message + " to " + address);
	    	socket = new Socket(address, TCP_PORT);
	        OutputStream outputStream = socket.getOutputStream();
	        PrintWriter writer = new PrintWriter(outputStream, true);

	        writer.print(message);
	        writer.flush();
	        writer.close();
    	} catch (IOException e) {
    		if (socket != null)
    			socket.close();
    		return false;
    	}
		socket.close();
        return true;
    }

    private void shareFolder() {
    	isTheSenderWorking = true;
        try {
        	senderSocket = new DatagramSocket();
        	senderSocket.setBroadcast(true);

            x: while (running) {
                String sharedFolder = p2p.getSharedFolder();
                if (!sharedFolder.equals("")) {
                    File folder = new File(sharedFolder);
                    Path baseFolderPath = folder.toPath();
                    File[] files = folder.listFiles();
                    StringBuilder sb = new StringBuilder();
                    for (File file : files) {
                        sb.append(baseFolderPath.relativize(file.toPath()).toString());
                        sb.append(',');
                    }
                    StringBuilder sb1 = new StringBuilder();
                    StringBuilder sb2 = new StringBuilder();
                    for (File file : files) {
                        try {
                            String jsonData = gson.toJson(FolderOperation.getAllFileInformations(baseFolderPath, file));
                            sb2.append(jsonData);
                            sb2.append('|');
                            sb1.append(FolderOperation.totalnumOfBytes);
                        	sb1.append(',');
                        } catch (IOException | NoSuchAlgorithmException e) {
                            continue x;
                        }
                    }
                    sb.append(';');
                    sb.append(sb1);
                    sb.append(';');
                    sb.append(sb2);
                    sendMessage("FOUND " + sb.toString());
                } else
                	sendMessage("FOUND ");
                for (int i = 0; running && i < 10; i++)
                	Thread.sleep(300);
            }
        } catch (IOException | InterruptedException e) {
        	if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
        }
        isTheSenderWorking = false;
    }
    
    private void tcpReceiver() {
    	try {
    		Socket					clientSocket;
    		InputStream				inputStream;
    		ByteArrayOutputStream	byteArrayOutputStream;
    		String					receivedMessage;
    		String[]				receivedMessageSplit;
    		
    		tcpReceiverSocket = new ServerSocket(TCP_PORT);

    		while (running) {
    			clientSocket = tcpReceiverSocket.accept();

    	        inputStream = clientSocket.getInputStream();
    	        byteArrayOutputStream = new ByteArrayOutputStream();

    	        byte[] buffer = new byte[1024];
    	        int bytesRead;

    	        while ((bytesRead = inputStream.read(buffer)) != -1)
    	        	byteArrayOutputStream.write(buffer, 0, bytesRead);
    	        
    	        buffer = byteArrayOutputStream.toByteArray();
    	        if (buffer[0] == 'D') {
    	        	int index = 9;
    	        	int id = 0;
    	        	while (buffer[index] != ' ')
    	        		id = id * 10 + buffer[index++] - '0';
    	        	downloadLock.lock();
                	downloadIDControlList.set(id, Arrays.copyOfRange(buffer, index + 1, buffer.length)); 
                	downloadLock.unlock();
                	continue;
    	        }
    	        receivedMessage = byteArrayOutputStream.toString();
    			clientSocket.close();
    			if (receivedMessage.startsWith("CONFIRM ")) {
                	receivedMessage = receivedMessage.substring(8);
                	receivedMessageSplit = receivedMessage.split(";");
                	int id = Integer.parseInt(receivedMessageSplit[1]);
                	confirmLock.lock();
                	if (confirmIDControlList.get(id) == null)
                		confirmIDControlList.set(id, new SmallInformation(clientSocket.getInetAddress().toString().substring(1), Integer.parseInt(receivedMessageSplit[2]), Long.parseLong(receivedMessageSplit[0])));
                	confirmLock.unlock();
    			} else if (receivedMessage.startsWith("REQUEST ")) {
                	receivedMessage = receivedMessage.substring(8);
                	System.out.println("From " + clientSocket.getInetAddress().toString().substring(1) + " " + receivedMessage);
                	receivedMessageSplit = receivedMessage.split(";");
                	uploadLock.lock();
                	uploadIDControlList.set(Integer.parseInt(receivedMessageSplit[1]), Long.parseLong(receivedMessageSplit[0]));
                	uploadLock.unlock();
                }
    		}
		} catch (IOException e) {
			if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
		}
    }

    private void udpReceiver() {
        try {
        	udpReceiverSocket = new DatagramSocket(UDP_PORT);

            x: while (running) {
            	udpReceiverSocket.receive(packet);
            	String address = packet.getAddress().toString().substring(1);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                if (p2p.isMessageOwner(address))
                	continue;
                if (receivedMessage.startsWith("FOUND ")) {
                    receivedMessage = receivedMessage.substring(6);
                    p2p.addElementToFoundList(address, receivedMessage);
                } else if (receivedMessage.startsWith("CONTROL ")) {
                    receivedMessage = receivedMessage.substring(8);
                    String sharedFolder = p2p.getSharedFolder();
                    if (sharedFolder.equals(""))
                    	continue ;
                    running = false;
                    while (isTheSenderWorking)
                    	Thread.sleep(300);
                    senderSocket.close();
                    File folder = new File(sharedFolder);
                    Path baseFolderPath = folder.toPath();
                    File[] files = folder.listFiles();
                    String[] receivedMessageSplit = receivedMessage.split(";");
                    for (File file : files) {
                        try {
                        	List<List<String>> getAllFileInformations = FolderOperation.getAllFileInformations(baseFolderPath, file);
                            if (getAllFileInformations.get(2).equals(gson.fromJson(receivedMessageSplit[2], List.class).get(2))) {
                            	ArrayList<byte[]>	bytes = FolderOperation.bytes;
                            	running = true;
                            	uploadLock.lock();
                            	uploadIDControlList.add(-1L);
                            	uploadLock.unlock();
                            	new Thread(this::shareFolder).start();
                            	new Thread(() -> upload(address, getAllFileInformations, bytes, receivedMessageSplit[0], receivedMessageSplit[1], uploadThreadNum++)).start();
                            	continue x;
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                            System.err.println("An error occurred: " + e.getMessage());
                        }
                    }
                    running = true;
                	new Thread(this::shareFolder).start();
                }
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
        } catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

	private void upload(String address, List<List<String>> getAllFileInformations, ArrayList<byte[]> bytes, String downloadedBytes, String targetID, int id) {
		try {
			int control = 0;
			while (senderSocket.isClosed()) {
				if (control == 40)
					return ;
				control++;
	    		Thread.sleep(300);
			}
			if (!sendPrivateMessage("CONFIRM " + downloadedBytes + ";" + targetID + ";" + id, address))
				return ;
			control = 0;
			uploadLock.lock();
			while (uploadIDControlList.get(id) == -1L) {
				uploadLock.unlock();
				if (control == 40)
					return ;
				control++;
	    		Thread.sleep(300);
	    		uploadLock.lock();
			}
			System.out.println("DOWNLOAD " + downloadedBytes + " from " + P2P.selfAddress);
			long	requestFirstBytesIndex = uploadIDControlList.get(id);
			uploadLock.unlock();
			String	message = "DOWNLOAD " + targetID + " ";
			byte[]	buffer = message.getBytes();
			int		len = buffer.length;
			int		size = 0;
			int		copiedLen;

			for (byte[] b : bytes) {
				if (requestFirstBytesIndex >= b.length)
					requestFirstBytesIndex -= b.length;
				else {
					copiedLen = (int) Math.min(b.length - requestFirstBytesIndex, CHUNK_SIZE - size);
					buffer = Arrays.copyOf(buffer, len + size + copiedLen);
					System.arraycopy(b, (int) requestFirstBytesIndex, buffer, size + len, copiedLen);
					size += copiedLen;
					if (size == CHUNK_SIZE)
						break;
					requestFirstBytesIndex = 0;
				}
			}

			Socket socket = new Socket(address, TCP_PORT);
	        OutputStream outputStream = socket.getOutputStream();

	        outputStream.write(buffer);
            outputStream.flush();
	        socket.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return ;
		}
	}
}



class SmallInformation {
	private String		address;
	private Long 		dowloadedBytes;
	private int			num;

	public SmallInformation(String address, int num, Long dowloadedBytes) {
		this.num = num;
		this.address = address;
		this.dowloadedBytes = dowloadedBytes;
	}
	
	public int getNum() {
		return num;
	}

	public String getAddress() {
		return address;
	}

	public Long getDowloadedBytes() {
		return dowloadedBytes;
	}
}