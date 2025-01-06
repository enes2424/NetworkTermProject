import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public class P2P extends JFrame {
	private static final long			serialVersionUID = 1L;
	public static String				selfAddress;
	private static List<String>			selfAddresses = new ArrayList<>();
	private static String				name = "P2P";
	private SocketOperation 			peer;
	private JMenuBar					menuBar = new JMenuBar();
	private JMenu						filesMenu = new JMenu("Files");
	private JMenuItem					connectMenuItem = new JMenuItem("Connect");
	private JMenuItem					disconnectMenuItem = new JMenuItem("Disconnect");
	private JMenuItem					exitMenuItem = new JMenuItem("Exit");
	private JMenu						helpMenu = new JMenu("Help");
	private JMenuItem					aboutMenuItem = new JMenuItem("About");
	private ImageIcon					icon = new ImageIcon(new ImageIcon("images/EnesMahmutATES.png").getImage().getScaledInstance(80, 100, Image.SCALE_SMOOTH));
	private Handler						handler = new Handler();
	private JLabel 						statusLabel = new JLabel();
	private ImageIcon 					greenCircle = new ImageIcon("images/green_circle.png");
	private ImageIcon 					redCircle = new ImageIcon("images/red_circle.png");
	private JLabel						sharedFolder = new JLabel("Root of the P2P shared folder:");
	private JLabel						destinationFolder = new JLabel("Destination folder:");
	private JTextField					textField1 = new JTextField();
	private JTextField					textField2 = new JTextField();
	private JButton						set1Button = new JButton("Set");
	private JButton						set2Button = new JButton("Set");
	private JFileChooser				folderChooser = new JFileChooser();
	private boolean						isConnect = false;
	private JLabel						folderExclusion = new JLabel("Folder exclusion:");
	private DefaultListModel<String>	listModel1 = new DefaultListModel<>();
    private JList<String>				list1 = new JList<>(listModel1);
	private JScrollPane					scrollPane1 = new JScrollPane(list1);
	private JButton						add1Button = new JButton("Add");
	private JButton						del1Button = new JButton("Del");
	private JLabel						excludeFilesMachingTheseMasks = new JLabel("Exclude files maching these masks:");
	private DefaultListModel<String>	listModel2 = new DefaultListModel<>();
    private JList<String>				list2 = new JList<>(listModel2);
	private JScrollPane					scrollPane2 = new JScrollPane(list2);
	private JButton						add2Button = new JButton("Add");
	private JButton						del2Button = new JButton("Del");
	private JLabel						downloadingFiles = new JLabel("Downloading files:");
	private DefaultListModel<String>	listModel3 = new DefaultListModel<>();
	private JScrollPane					scrollPane3 = new JScrollPane(new JList<>(listModel3));
	private JLabel						foundFiles = new JLabel("Found files:");
	private DefaultListModel<String>	listModel4 = new DefaultListModel<>();
    private JList<String>				list3 = new JList<>(listModel4);
	private JScrollPane					scrollPane4 = new JScrollPane(list3);
	private List<String>				listFileInfo = new ArrayList<>();
	private	List<Long>					listByte = new ArrayList<>();
	private ExecutorService 			threadPool = Executors.newCachedThreadPool();

	static {
		try {
			Enumeration<NetworkInterface>	networkInterfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface				networkInterface;
			Enumeration<InetAddress>		inetAddresses;
            InetAddress						inetAddress;
            boolean							isSelfAddress;

            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp())
                	continue;

                inetAddresses = networkInterface.getInetAddresses();
                isSelfAddress = true;
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
	                    selfAddresses.add(inetAddress.getHostAddress());
	                    if (isSelfAddress) {
	                    	selfAddress = inetAddress.getHostAddress();
	                    	isSelfAddress = false;
	                    }
                    }
                }
            }
		} catch (IOException e) {
            e.printStackTrace();
			System.exit(1);
        }
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 1)
			name = args[0];
		try {
			new P2P(name + " " + selfAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private void windowClosing() {
		if (!isConnect)
			System.exit(0);
		int response = JOptionPane.showConfirmDialog(P2P.this,
			"Are you sure you want to close the application?",
			"Close",
			JOptionPane.YES_NO_OPTION);

		if (response == JOptionPane.YES_OPTION) {
			peer.disconnect();
			System.exit(0);
		}
	}

	public P2P(String name) throws UnknownHostException {
		super(name);

		peer = new SocketOperation(this);

		setSize(575, 800);
		//setSize(515, 750);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setLayout(null);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                P2P.this.windowClosing();
            }
        });

		setJMenuBar(menuBar);

		menuBar.add(filesMenu);
		filesMenu.add(connectMenuItem);
		filesMenu.add(disconnectMenuItem);
		filesMenu.add(exitMenuItem);

		menuBar.add(helpMenu);
		helpMenu.add(aboutMenuItem);

		connectMenuItem.addActionListener(handler);
		disconnectMenuItem.addActionListener(handler);
		exitMenuItem.addActionListener(handler);
		aboutMenuItem.addActionListener(handler);

		statusLabel.setBounds(435, 10, 20, 20);
	    statusLabel.setIcon(redCircle);
	    add(statusLabel);

		sharedFolder.setFont(new Font("Tahoma", Font.BOLD, 13));
		sharedFolder.setBounds(20, 10, 500, 25);
		add(sharedFolder);

		textField1.setBounds(20, 40, 390, 25);
		add(textField1);

		set1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = folderChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = folderChooser.getSelectedFile();
                    textField1.setText(selectedFolder.getAbsolutePath());
                }
			}
		});

		set1Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		set1Button.setBounds(420, 40, 50, 25);
		add(set1Button);

		destinationFolder.setFont(new Font("Tahoma", Font.BOLD, 13));
		destinationFolder.setBounds(20, 70, 500, 25);
		add(destinationFolder);

		textField2.setBounds(20, 100, 390, 25);
		add(textField2);

		set2Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = folderChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFolder = folderChooser.getSelectedFile();
                    textField2.setText(selectedFolder.getAbsolutePath());
                }
			}
		});

		set2Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		set2Button.setBounds(420, 100, 50, 25);
		add(set2Button);

		FolderOperation.listModel1 = listModel1;
		SocketOperation.listModel1 = listModel1;
		SocketOperation.listModel2 = listModel2;

		folderExclusion.setFont(new Font("Tahoma", Font.BOLD, 13));
		folderExclusion.setBounds(20, 130, 200, 25);
		add(folderExclusion);

		scrollPane1.setBounds(20, 160, 150, 120);
		add(scrollPane1);

		add1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controlValid(textField1)) {
					File directory = new File(textField1.getText());
					Path baseFolderPath = directory.toPath();
					List<String> folders = FolderOperation.getAllFolders(listModel1, baseFolderPath, directory);
					new FolderSelection(listModel1, folders);
				} else
					JOptionPane.showMessageDialog(P2P.this, "No suitable shared folder found!", "Folder Exclusion Cancelled", JOptionPane.WARNING_MESSAGE);
			}
		});

		add1Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		add1Button.setBounds(175, 180, 55, 30);
		add(add1Button);

		del1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selectedIndexToDelete = list1.getSelectedIndex();
				if (selectedIndexToDelete != -1)
					listModel1.remove(selectedIndexToDelete);
			}
		});

		del1Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		del1Button.setBounds(175, 220, 55, 30);
		add(del1Button);

		excludeFilesMachingTheseMasks.setFont(new Font("Tahoma", Font.BOLD, 13));
		excludeFilesMachingTheseMasks.setBounds(235, 130, 300, 25);
		add(excludeFilesMachingTheseMasks);

		scrollPane2.setBounds(235, 160, 200, 120);
		add(scrollPane2);

		add2Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String mask = JOptionPane.showInputDialog(P2P.this, "Enter the mask to exclude:", "Mask", JOptionPane.PLAIN_MESSAGE);
				if (mask != null && !mask.equals("") && !listModel2.contains(mask)) {
					listModel2.addElement(mask);
				}
			}
		});

		add2Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		add2Button.setBounds(440, 180, 55, 30);
		add(add2Button);

		del2Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selectedIndexToDelete = list2.getSelectedIndex();
				if (selectedIndexToDelete != -1)
					listModel2.remove(selectedIndexToDelete);
			}
		});

		del2Button.setFont(new Font("Tahoma", Font.BOLD, 9));
		del2Button.setBounds(440, 220, 55, 30);
		add(del2Button);


		downloadingFiles.setFont(new Font("Tahoma", Font.BOLD, 13));
		downloadingFiles.setBounds(20, 290, 500, 25);
		add(downloadingFiles);

		scrollPane3.setBounds(20, 320, 450, 150);
		add(scrollPane3);

		foundFiles.setFont(new Font("Tahoma", Font.BOLD, 13));
		foundFiles.setBounds(20, 480, 500, 25);
		add(foundFiles);

        scrollPane4.setBounds(20, 510, 450, 150);
        add(scrollPane4);

        list3.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String	selectedItem = list3.getSelectedValue();
					int		selectedIndex = list3.getSelectedIndex();
                    if (selectedItem == null || selectedIndex == -1)
						return ;
					selectedItem = selectedItem.substring(0, selectedItem.indexOf(" from "));
					if (controlValid(textField2)) {
						int response = JOptionPane.showConfirmDialog(P2P.this,
							"Do you want to download file " + selectedItem + "?",
							"Download",
							JOptionPane.YES_NO_OPTION);

						if (response == JOptionPane.YES_OPTION) {
							listModel3.addElement(selectedItem + "   %0");
							peer.addIDControlList();
							String	text = textField2.getText();
							int		index = listModel3.size() - 1;
							long	totalByte = listByte.get(selectedIndex);
							String	fileInfo = listFileInfo.get(selectedIndex);
							threadPool.execute(() -> {
								try {
									peer.download(text, index, totalByte, fileInfo);
								} catch (IOException | InterruptedException err) {
									JOptionPane.showMessageDialog(P2P.this, "Connection is broken!", "Download Cancelled", JOptionPane.WARNING_MESSAGE);
									err.printStackTrace();
								}
							});
						}
					} else
						JOptionPane.showMessageDialog(P2P.this, "No suitable destination folder found!", "Download Cancelled", JOptionPane.WARNING_MESSAGE);
					list3.clearSelection();
                }
            }
        });

		textField1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
				listModel1.clear();
                controlValid(textField1);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
				listModel1.clear();
                controlValid(textField1);
            }

			@Override
            public void changedUpdate(DocumentEvent e) {
				listModel1.clear();
                controlValid(textField1);
            }
        });

		textField2.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controlValid(textField2);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controlValid(textField2);
            }

			@Override
            public void changedUpdate(DocumentEvent e) {
                controlValid(textField2);
            }
        });

		setVisible(true);
	}

	private boolean controlValid(JTextField textField) {
		File directory = new File(textField.getText());
		if (directory.exists() && directory.isDirectory()) {
			textField.setForeground(Color.BLACK);
			return true;
		}
		textField.setForeground(Color.RED);
		return false;
	}

	public	String getSharedFolder() {
		if (controlValid(textField1) == false)
			return "";
		return textField1.getText();
	}

	public static String formatBytes(double size) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = 0;

        while (size >= 1024 && index < units.length - 1) {
            size /= 1024.0;
            index++;
        }

        return String.format("%.2f %s", size, units[index]);
	}

	public boolean isMessageOwner(String address) {
		if (!selfAddresses.contains(address))
			return false;
		if (!address.equals(selfAddress))
			setTitle(name + address);
		return true;
	}

	public	void addElementToFoundList(String address, String receivedMessage) {
		SwingUtilities.invokeLater(() -> {
			if (receivedMessage.equals("")) {
				for (int i = 0; i < listModel4.getSize(); i++) {
					if (listModel4.getElementAt(i).contains(address)) {
						listFileInfo.remove(i);
						listByte.remove(i);
						listModel4.remove(i--);
					}
				}
				return ;
			}
			String[]			filesAndJsons = receivedMessage.split(";");
			String[]			files = filesAndJsons[0].split(",");
			String[]			bytes = filesAndJsons[1].split(",");
			String[]			jsons = filesAndJsons[2].split("\\|");
			ArrayList<String>	paths;
        	ArrayList<String>	numOfBytes;
        	ArrayList<String>	hashes;
			long				totalByte;
			int					size = files.length;
			int					index = 0;


			x: for (int i = 0; i < listModel4.size(); i++) {
				if (listModel4.getElementAt(i).contains(address)) {
					if (index == size) {
						listFileInfo.remove(i);
						listByte.remove(i);
						listModel4.remove(i--);
					} else {
						for (int j = 0; j < listModel2.size(); j++) {
							String filename = files[index].substring(files[index].lastIndexOf('/') + 1);
							if (FolderOperation.isMatchedFileAndMask(filename, listModel2.getElementAt(j))) {
								index++;
								i--;
								continue x;
							}
						}
						totalByte = Long.parseLong(bytes[index]);
						if (listModel2.size() == 0) {
							listFileInfo.set(i, jsons[index]);
							listByte.set(i, totalByte);
							listModel4.set(i, files[index++] + " " + formatBytes(totalByte) + " from " + address);
							continue x;
						}
						@SuppressWarnings("unchecked")
						List<List<String>>	data = SocketOperation.gson.fromJson(jsons[index], List.class);
						paths = new ArrayList<>(data.get(0));
        				numOfBytes = new ArrayList<>(data.get(1));
        				hashes = new ArrayList<>(data.get(2));
						for (int j = 0; j < paths.size(); j++) {
							String filename = paths.get(j).substring(paths.get(j).lastIndexOf('/') + 1);
							for (int k = 0; k < listModel2.size(); k++)
								if (FolderOperation.isMatchedFileAndMask(filename, listModel2.getElementAt(k))) {
									paths.remove(j);
									totalByte -= Long.parseLong(numOfBytes.get(j));
									numOfBytes.remove(j);
									hashes.remove(j);
									j--;
									break;
								}
						}
						if (totalByte == 0) {
							index++;
							i--;
							continue x;
						}
						listFileInfo.set(i, SocketOperation.gson.toJson(List.of(paths, numOfBytes, hashes)));
						listByte.set(i, totalByte);
						listModel4.set(i, files[index++] + " " + formatBytes(totalByte) + " from " + address);
					}
				}
			}


			for (int i = index; i < size; i++) {
				if (listModel2.size() == 0) {
					listFileInfo.add(jsons[i]);
					listByte.add(totalByte = Long.parseLong(bytes[i]));
					listModel4.addElement(files[i] + " " + formatBytes(totalByte) + " from " + address);
					continue;
				}
				@SuppressWarnings("unchecked")
				List<List<String>>	data = SocketOperation.gson.fromJson(jsons[i], List.class);
				paths = new ArrayList<>(data.get(0));
        		numOfBytes = new ArrayList<>(data.get(1));
        		hashes = new ArrayList<>(data.get(2));
				totalByte = Long.parseLong(bytes[i]);
				for (int j = 0; j < paths.size(); j++) {
					String filename = paths.get(j).substring(paths.get(j).lastIndexOf('/') + 1);
					for (int k = 0; k < listModel2.size(); k++)
						if (FolderOperation.isMatchedFileAndMask(filename, listModel2.getElementAt(k))) {
							paths.remove(j);
							totalByte -= Long.parseLong(numOfBytes.get(j));
							numOfBytes.remove(j);
							hashes.remove(j);
							j--;
							break;
						}
				}
				if (totalByte == 0)
					continue;
				listFileInfo.add(jsons[i]);
				listByte.add(totalByte);
				listModel4.addElement(files[i] + " " + formatBytes(totalByte) + " from " + address);
			}
		});
	}

	private class Handler implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == exitMenuItem)
				windowClosing();
			else if (event.getSource() == connectMenuItem) {
				if (!isConnect) {
					isConnect = true;
		            statusLabel.setIcon(greenCircle);
					peer.connect();
				}
			} else if (event.getSource() == disconnectMenuItem) {
	        	if (isConnect) {
		        	isConnect = false;
		            statusLabel.setIcon(redCircle);
					listModel4.clear();
		            peer.disconnect();
	        	}
	        } else if (event.getSource() == aboutMenuItem)
				JOptionPane.showMessageDialog(P2P.this, "Name : Enes Mahmut\nSurname : ATES\nSchool Number : 20200702008\n"
						+ "Email : enesmahmut.ates@std.yeditepe.edu.tr", "Developer Information", JOptionPane.INFORMATION_MESSAGE, icon);
		}
	}

	public void setPercentage(int index, double l) {
		String item = listModel3.get(index);
		listModel3.set(index, String.format("%s %.2f",  item.substring(0, item.indexOf('%') + 1), l * 100));
	}
}
