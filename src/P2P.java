import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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

public class P2P extends JFrame {
	private static final long serialVersionUID = 1L;
	private UDPFlooding 	peer = new UDPFlooding(this);
	private JMenuBar		menuBar = new JMenuBar();
	private JMenu			filesMenu = new JMenu("Files");
	private JMenuItem		connectMenuItem = new JMenuItem("Connect");
	private JMenuItem		disconnectMenuItem = new JMenuItem("Disconnect");
	private JMenuItem		exitMenuItem = new JMenuItem("Exit");
	private JMenu			helpMenu = new JMenu("Help");
	private JMenuItem		aboutMenuItem = new JMenuItem("About");
	private ImageIcon		icon = new ImageIcon(new ImageIcon("src/EnesMahmutATES.png").getImage().getScaledInstance(80, 100, Image.SCALE_SMOOTH));
	private Handler			handler = new Handler();
	private JLabel 			statusLabel = new JLabel();
	private ImageIcon 		greenCircle = new ImageIcon("src/green_circle.png");
	private ImageIcon 		redCircle = new ImageIcon("src/red_circle.png");
	private JLabel			sharedFolder = new JLabel("Root of the P2P shared folder:");
	private JLabel			destinationFolder = new JLabel("Destination folder:");
	private JTextField		textField1 = new JTextField();
	private JTextField		textField2 = new JTextField();
	private JButton			set1Button = new JButton("Set");
	private JButton			set2Button = new JButton("Set");
	private JFileChooser	folderChooser = new JFileChooser();
	private boolean			isConnect = false;
	private JLabel			foundFiles = new JLabel("Found files:");
	private DefaultListModel<String>	listModel;
    private JList<String>				list;
    private JScrollPane					scrollPane; 
	
	public static void main(String[] args) {
		P2P frame = new P2P();
		frame.setVisible(true);
	}
	
	public P2P() {
		super("P2P");
		
		setSize(500, 650);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		
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
		
		set1Button.setFont(new Font("Tahoma", Font.BOLD, 10));
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
		
		set2Button.setFont(new Font("Tahoma", Font.BOLD, 10));
		set2Button.setBounds(420, 100, 50, 25);
		add(set2Button);
		
		foundFiles.setFont(new Font("Tahoma", Font.BOLD, 13));
		foundFiles.setBounds(20, 130, 500, 25);
		add(foundFiles);
		
		listModel = new DefaultListModel<>();
        list = new JList<>(listModel);

        scrollPane = new JScrollPane(list);
        scrollPane.setBounds(20, 160, 450, 150);
        add(scrollPane);
        
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedItem = list.getSelectedValue();
                    if (selectedItem != null) {
                        System.out.println("Tiklanan dosya: " + selectedItem);
                    }
                }
            }
        });
	}
	
	public	void addElementToFoundList(String file) {
		listModel.addElement(file);
	}
	
	private class Handler implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == exitMenuItem)
				System.exit(0);
			else if (event.getSource() == connectMenuItem) {
				if (!isConnect) {
					isConnect = true;
		            statusLabel.setIcon(greenCircle);
		            peer.connect(textField1.getText());
				}
			}
	        else if (event.getSource() == disconnectMenuItem) {
	        	if (isConnect) {
		        	isConnect = false;
		            statusLabel.setIcon(redCircle);
		            peer.disconnect();
	        	}
	        }
			else if (event.getSource() == aboutMenuItem)
				JOptionPane.showMessageDialog(P2P.this, "Name : Enes Mahmut\n"
						+ "Surname : ATES\n"
						+ "School Number : 20200702008\n"
						+ "Email : enesmahmut.ates@std.yeditepe.edu.tr", "Developer Information", JOptionPane.INFORMATION_MESSAGE, icon);
		}	
	}
}
