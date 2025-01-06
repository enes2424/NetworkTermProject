import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class FolderSelection extends JFrame {
    private static final long serialVersionUID = 1L;
	private JList<String> folderList;
    private DefaultListModel<String> listModel;

    public FolderSelection(DefaultListModel<String> listModel, List<String> folders) {
        setTitle("Select Folder");
        setSize(400, 500);
        setLocationRelativeTo(null);

        this.listModel = new DefaultListModel<>();
        folderList = new JList<>(this.listModel);
        JScrollPane listScrollPane = new JScrollPane(folderList);

        for (String folder : folders)
            this.listModel.addElement(folder);

        add(listScrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedFolder = folderList.getSelectedValue();
                if (selectedFolder != null) {
                    for (int i = 0; i < listModel.size(); i++) {
                        if (listModel.getElementAt(i).startsWith(selectedFolder))
                            listModel.remove(i--);
                    }
                    listModel.addElement(selectedFolder);
                }
                dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }
}
