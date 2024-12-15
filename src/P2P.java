import java.util.HashMap;
import java.util.Map;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.io.File;
import java.io.IOException;

import java.net.InetAddress;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;



public class P2P extends JFrame {
	private static final long			serialVersionUID = 1L;
	private static String				selfAddress;
	private UDPFlooding 				peer = new UDPFlooding(this);
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
	private JLabel						foundFiles = new JLabel("Found files:");
	private DefaultListModel<String>	listModel = new DefaultListModel<>();
    private JList<String>				list = new JList<>(listModel);
    private JScrollPane					scrollPane;
	private P2P							self = this;

	static {
		try { 
			selfAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (IOException e) {
            e.printStackTrace();
			System.exit(1);
        }
	}

	public static void main(String[] args) {
		new P2P("P2P " + selfAddress);
	}
	
	private void windowClosing() {
		if (!isConnect) {
			System.out.println("Closing operation is being performed...");
			System.exit(0);
		}
		int response = JOptionPane.showConfirmDialog(self, 
			"Are you sure you want to close the application?", 
			"Close", 
			JOptionPane.YES_NO_OPTION);
	
		if (response == JOptionPane.YES_OPTION) {
			peer.disconnect();
			System.out.println("Closing operation is being performed...");
			System.exit(0);
		} else
			System.out.println("Closing operation was canceled.");
	}	

	public P2P(String name) {
		super(name);
		
		setSize(500, 650);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                self.windowClosing();
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
		
		foundFiles.setFont(new Font("Tahoma", Font.BOLD, 13));
		foundFiles.setBounds(20, 130, 500, 25);
		add(foundFiles);

        scrollPane = new JScrollPane(list);
        scrollPane.setBounds(20, 160, 450, 150);
        add(scrollPane);
        
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedItem = list.getSelectedValue();
                    if (selectedItem != null)
                        System.out.println("Tiklanan dosya: " + selectedItem);
                }
            }
        });

		textField1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controlValid1();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controlValid1();
            }

			@Override
            public void changedUpdate(DocumentEvent e) {
                controlValid1();
            }
        });
	
		textField2.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controlValid2();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                controlValid2();
            }

			@Override
            public void changedUpdate(DocumentEvent e) {
                controlValid2();
            }
        });

		setVisible(true);
	}
	
	private boolean controlValid1() {
		File directory = new File(textField1.getText());
		if (directory.exists() && directory.isDirectory()) {
			textField1.setForeground(Color.BLACK);
			return true;
		}
		textField1.setForeground(Color.RED);
		return false;
	}
	
	private boolean controlValid2() {
		File directory = new File(textField2.getText());
		if (directory.exists() && directory.isDirectory()) {
			textField2.setForeground(Color.BLACK);
			return true;
		}
		textField2.setForeground(Color.RED);
		return false;
	}

	public	String getMessage() {
		if (controlValid1() == false)
			return "";
		return textField1.getText();
	}

	public	void addElementToFoundList(String address, String receivedMessage) {
		if (address.substring(1).equals(selfAddress))
			return ;
		if (receivedMessage.equals("")) {
			for (int i = 0; i < listModel.getSize(); i++)
				if (listModel.getElementAt(i).contains(address))
					listModel.remove(i--);
			return ;
		}
		String[]	files = receivedMessage.split(",");
		int			size = files.length;
		int			index = 0;
		for (int i = 0; i < listModel.getSize(); i++) {
			if (listModel.getElementAt(i).contains(address)) {
				if (index == size)
					listModel.remove(i--);
				else
					listModel.set(i, files[index++] + " from " + address);
			}
		}
		for (int i = index; i < size; i++)
			listModel.addElement(files[i] + " from " + address);
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
			}
	        else if (event.getSource() == disconnectMenuItem) {
	        	if (isConnect) {
		        	isConnect = false;
		            statusLabel.setIcon(redCircle);
					listModel.clear();
		            peer.disconnect();
	        	}
	        }
			else if (event.getSource() == aboutMenuItem)
				JOptionPane.showMessageDialog(P2P.this, "Name : Enes Mahmut\nSurname : ATES\nSchool Number : 20200702008\n"
						+ "Email : enesmahmut.ates@std.yeditepe.edu.tr", "Developer Information", JOptionPane.INFORMATION_MESSAGE, icon);
		}	
	}
}
