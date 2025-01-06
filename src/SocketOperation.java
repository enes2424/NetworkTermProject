import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Paths;
import java.nio.file.Path;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.google.gson.Gson;

import java.security.NoSuchAlgorithmException;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.DefaultListModel;

public class SocketOperation {
    private static final int    			UDP_PORT = 9090;
    private static final int    			TCP_PORT = 8080;
    private static final String 			BROADCAST_ADDRESS = "255.255.255.255";
    private static final int				CHUNK_SIZE = 256 * 1024;
	public static Gson                		gson = new Gson();
	public static DefaultListModel<String>	listModel1;
	public static DefaultListModel<String>	listModel2;
    private ExecutorService 				threadPool = Executors.newCachedThreadPool();
    private final ReentrantLock 			uploadLock = new ReentrantLock();
    private final ReentrantLock 			runningLock = new ReentrantLock();
    private final ReentrantLock 			confirmLock = new ReentrantLock();
    private final ReentrantLock 			downloadLock = new ReentrantLock();
    private final ReentrantLock 			shareFolderLock = new ReentrantLock();
    private DatagramSocket	    			senderSocket;
    private DatagramSocket	    			udpReceiverSocket;
    private ServerSocket					tcpReceiverSocket;
    private byte[]              			buffer = new byte[65507];
    private DatagramPacket      			packet = new DatagramPacket(buffer, buffer.length);
    private boolean			    			running;
    private P2P				    			p2p;
    private int								uploadThreadNum = 0;
    private InetAddress						broadcastAddress;
    public ArrayList<Long>					uploadIDControlList = new ArrayList<>();
    public ArrayList<SmallInformation>		confirmIDControlList = new ArrayList<>();
    public ArrayList<byte[]>				downloadIDControlList = new ArrayList<>();

    public SocketOperation(P2P p2p) throws UnknownHostException {
    	broadcastAddress = InetAddress.getByName(BROADCAST_ADDRESS);
    	this.p2p = p2p;
    	runningLock.lock();
        this.running = false;
        runningLock.unlock();
    }

    public void connect() {
    	runningLock.lock();
    	this.running = true;
    	runningLock.unlock();
    	threadPool.execute(this::shareFolder);
    	threadPool.execute(this::udpReceiver);
    	threadPool.execute(this::tcpReceiver);
    }

    public void	disconnect() {
        try {
        	sendMessage("FOUND ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        runningLock.lock();
    	this.running = false;
    	runningLock.unlock();
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

    public void download(String destinationFolder, int id, long totalByte, String fileInfo) throws IOException, InterruptedException {
    	long downloadedBytes = 0;
    	long readedBytes = 0;
		@SuppressWarnings("unchecked")
		List<List<String>> data = gson.fromJson(fileInfo, List.class);
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
    	InetAddress	targetAddress;
		ArrayList<String> filter = new ArrayList<>();
		for (int i = 0; i < listModel2.size(); i++)
			filter.add(listModel2.get(i));
    	x: while (downloadedBytes != totalByte) {
    		confirmLock.lock();
	    	confirmIDControlList.set(id, null);
	    	confirmLock.unlock();
    		runningLock.lock();
    		if (!this.running) {
    			runningLock.unlock();
    			throw new IOException();
    		}
	    	try {
	    		sendMessage("CONTROL " + downloadedBytes + ";" + id + ";" + fileInfo + ";" + gson.toJson(filter));
	    		runningLock.unlock();
	    	} catch(IOException err) {
	    		runningLock.unlock();
	    		throw new IOException();
	    	}
	    	control = 0;
	    	while (true) {
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

    		runningLock.lock();
    		if (!this.running) {
    			runningLock.unlock();
    			throw new IOException();
    		}
    		try {
    			sendPrivateMessage("REQUEST " + downloadedBytes + ";" + targetID, targetAddress);
    			runningLock.unlock();
    		} catch(IOException err) {
    			runningLock.unlock();
    			throw new IOException();
    		}
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
	    	if (buffer.length > targetByteCount - readedBytes) {
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
					downloadedBytes += (targetByteCount - readedBytes);
		        	buffer = Arrays.copyOfRange(buffer, (int) (targetByteCount - readedBytes), (int) buffer.length);
		        	readedBytes = 0;
		        	targetByteCount = newTargetByteCount;
		    	}
	    	} else {
	    		fos.write(buffer, 0, buffer.length);
	    		readedBytes += buffer.length;
	    	}
	    	downloadedBytes += buffer.length;
	    	p2p.setPercentage(id, (1.0 * downloadedBytes) / totalByte);
	    	downloadLock.lock();
	    	downloadIDControlList.set(id, null);
	    	downloadLock.unlock();
    	}
    	fos.close();
    }

    private void sendMessage(String message) throws IOException {
    	byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
        		broadcastAddress , UDP_PORT);

        senderSocket.send(packet);
    }

    private void sendPrivateMessage(String message, InetAddress address) throws IOException {
    	byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                address, UDP_PORT);

        senderSocket.send(packet);
    }

    private void shareFolder() {
        try {
        	senderSocket = new DatagramSocket();
        	senderSocket.setBroadcast(true);

        	runningLock.lock();
            x: while (running) {
            	runningLock.unlock();
            	shareFolderLock.lock();
                String sharedFolder = p2p.getSharedFolder();
                if (!sharedFolder.equals("")) {
                    File			folder = new File(sharedFolder);
                    Path			baseFolderPath = folder.toPath();
                    List<File>		files = FolderOperation.getAllFilesAndFolders(folder);
                    StringBuilder	sb = new StringBuilder();
					StringBuilder	sb1 = new StringBuilder();
                    StringBuilder	sb2 = new StringBuilder();
                    StringBuilder	sb3 = new StringBuilder();
					String			path;
                    y: for (File file : files) {
						path = baseFolderPath.relativize(file.toPath()).toString();
						for (int i = 0; i < listModel1.size(); i++)
							if (path.equals(listModel1.getElementAt(i)) || path.startsWith(listModel1.getElementAt(i) + "/"))
								continue y;
                        try {
                            String jsonData = gson.toJson(FolderOperation.getAllFileInformations(baseFolderPath, file, null));
							sb1.append(baseFolderPath.relativize(file.toPath()).toString());
                        	sb1.append(',');
                            sb2.append(FolderOperation.totalnumOfBytes);
                        	sb2.append(',');
							sb3.append(jsonData);
                            sb3.append('|');
                        } catch (IOException | NoSuchAlgorithmException e) {
                        	shareFolderLock.unlock();
                        	runningLock.lock();
                            continue x;
                        }
                    }
					sb.append(sb1);
                    sb.append(';');
                    sb.append(sb2);
                    sb.append(';');
                    sb.append(sb3);
                    sendMessage("FOUND " + sb.toString());
                } else
                	sendMessage("FOUND ");
                shareFolderLock.unlock();
                runningLock.lock();
                for (int i = 0; running && i < 10; i++) {
                	runningLock.unlock();
                	Thread.sleep(300);
                	runningLock.lock();
                }
            }
            runningLock.unlock();
        } catch (IOException | InterruptedException e) {
			if (runningLock.isLocked())
        		runningLock.unlock();
			if (shareFolderLock.isLocked())
				shareFolderLock.unlock();
        	if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
        }
    }

    private void tcpReceiver() {
    	try {
    		Socket					clientSocket;
    		InputStream				inputStream;
    		ByteArrayOutputStream	byteArrayOutputStream;

    		tcpReceiverSocket = new ServerSocket(TCP_PORT);
    		runningLock.lock();
    		while (running) {
    			runningLock.unlock();
    			clientSocket = tcpReceiverSocket.accept();

    	        inputStream = clientSocket.getInputStream();

    	        byteArrayOutputStream = new ByteArrayOutputStream();

    	        byte[] buffer = new byte[1024];
    	        int bytesRead;

    	        while ((bytesRead = inputStream.read(buffer)) != -1)
    	        	byteArrayOutputStream.write(buffer, 0, bytesRead);
    			clientSocket.close();
    	        buffer = byteArrayOutputStream.toByteArray();
    	        int index = 0;
    	        int id = 0;
    	        while (buffer[index] != ' ')
    	        	id = id * 10 + buffer[index++] - '0';
    	        downloadLock.lock();
                downloadIDControlList.set(id, Arrays.copyOfRange(buffer, index + 1, buffer.length));
                downloadLock.unlock();
                runningLock.lock();
    		}
    		runningLock.unlock();
		} catch (IOException e) {
			if (runningLock.isLocked())
				runningLock.unlock();
			if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
		}
    }

    private void udpReceiver() {
        try {
			udpReceiverSocket = new DatagramSocket(UDP_PORT);

			runningLock.lock();
            x: while (running) {
            	runningLock.unlock();
				udpReceiverSocket.receive(packet);
            	String address = packet.getAddress().toString().substring(1);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                if (p2p.isMessageOwner(address)) {
                	runningLock.lock();
                	continue;
                }
                if (receivedMessage.startsWith("FOUND ")) {
                    receivedMessage = receivedMessage.substring(6);
                    p2p.addElementToFoundList(address, receivedMessage);
                } else if (receivedMessage.startsWith("CONTROL ")) {
                    receivedMessage = receivedMessage.substring(8);
                    String sharedFolder = p2p.getSharedFolder();
                    if (sharedFolder.equals("")) {
                    	runningLock.lock();
                    	continue ;
                    }
                    shareFolderLock.lock();
                    File		folder = new File(sharedFolder);
                    Path		baseFolderPath = folder.toPath();
                    List<File>	files = FolderOperation.getAllFilesAndFolders(folder);
                    String[]	receivedMessageSplit = receivedMessage.split(";");
					String		path;
					@SuppressWarnings("unchecked")
					ArrayList<String>	filter = gson.fromJson(receivedMessageSplit[3], ArrayList.class);
                    y: for (File file : files) {
						path = baseFolderPath.relativize(file.toPath()).toString();
						for (int i = 0; i < listModel1.size(); i++)
							if (path.equals(listModel1.getElementAt(i)) || path.startsWith(listModel1.getElementAt(i) + "/"))
								continue y;
                        try {
                        	List<List<String>> getAllFileInformations = FolderOperation.getAllFileInformations(baseFolderPath, file, filter);
                            if (getAllFileInformations.get(2).equals(gson.fromJson(receivedMessageSplit[2], List.class).get(2))) {
                            	ArrayList<byte[]>	bytes = FolderOperation.bytes;
                            	uploadLock.lock();
                            	uploadIDControlList.add(-1L);
                            	uploadLock.unlock();
                            	shareFolderLock.unlock();
                            	InetAddress	addr = packet.getAddress();
								int	threadNum = uploadThreadNum++;
                            	threadPool.execute(() -> upload(addr, getAllFileInformations, bytes, receivedMessageSplit[0], receivedMessageSplit[1], threadNum));
                            	runningLock.lock();
								continue x;
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                        	e.printStackTrace();
                        }
                    }
                    shareFolderLock.unlock();
                } else if (receivedMessage.startsWith("CONFIRM ")) {
                	receivedMessage = receivedMessage.substring(8);
                	String[] receivedMessageSplit = receivedMessage.split(";");
                	int id = Integer.parseInt(receivedMessageSplit[1]);
                	confirmLock.lock();
                	if (confirmIDControlList.get(id) == null)
                		confirmIDControlList.set(id, new SmallInformation(packet.getAddress(), Integer.parseInt(receivedMessageSplit[2]), Long.parseLong(receivedMessageSplit[0])));
                	confirmLock.unlock();
    			} else if (receivedMessage.startsWith("REQUEST ")) {
                	receivedMessage = receivedMessage.substring(8);
                	String[] receivedMessageSplit = receivedMessage.split(";");
                	uploadLock.lock();
                	uploadIDControlList.set(Integer.parseInt(receivedMessageSplit[1]), Long.parseLong(receivedMessageSplit[0]));
                	uploadLock.unlock();
                }
                runningLock.lock();
            }
            runningLock.unlock();
        } catch (IOException e) {
			if (runningLock.isLocked())
        		runningLock.unlock();
			if (shareFolderLock.isLocked())
        		shareFolderLock.unlock();
            if (!e.getMessage().equals("Socket closed"))
            	e.printStackTrace();
        }
    }

	private void upload(InetAddress address, List<List<String>> getAllFileInformations, ArrayList<byte[]> bytes, String downloadedBytes, String targetID, int id) {
		try {
			sendPrivateMessage("CONFIRM " + downloadedBytes + ";" + targetID + ";" + id, address);
			uploadLock.lock();
			int control = 0;
			while (uploadIDControlList.get(id) == -1L) {
				uploadLock.unlock();
				if (control++ == 40)
					return ;
	    		Thread.sleep(300);
	    		uploadLock.lock();
			}
			long	requestFirstBytesIndex = uploadIDControlList.get(id);
			uploadLock.unlock();
			String	message = targetID + " ";
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
	private InetAddress	address;
	private Long 		dowloadedBytes;
	private int			num;

	public SmallInformation(InetAddress address, int num, Long dowloadedBytes) {
		this.num = num;
		this.address = address;
		this.dowloadedBytes = dowloadedBytes;
	}

	public int getNum() {
		return num;
	}

	public InetAddress getAddress() {
		return address;
	}

	public Long getDowloadedBytes() {
		return dowloadedBytes;
	}
}
