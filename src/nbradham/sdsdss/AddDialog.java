package nbradham.sdsdss;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class AddDialog {

	private final JTextField nameField = new JTextField(25);
	private final DirectorySelector deckDS, sdDS;

	private final JDialog diag;

	AddDialog(JFrame mainFrame, Syncer syncer) {
		diag = new JDialog(mainFrame, true);
		diag.setLayout(new GridBagLayout());
		diag.setTitle("Add Game");
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.EAST;
		diag.add(new JLabel("Game Name:"), gc);
		gc.gridy = 1;
		diag.add(new JLabel("Local Save Directory:"), gc);
		gc.gridy = 2;
		diag.add(new JLabel("External Save Directory"), gc);
		gc.gridx = 1;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.WEST;
		gc.insets = new Insets(4, 4, 0, 0);
		JButton ok = new JButton("Add Game");
		deckDS = new DirectorySelector("Select LOCAL save directory.", diag);
		sdDS = new DirectorySelector("Select EXTERNAL save directory.", diag);
		Runnable okCheck = () -> {
			ok.setEnabled(!nameField.getText().isBlank() && deckDS.isDirSelected() && sdDS.isDirSelected());
		};
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				okCheck.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				okCheck.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				okCheck.run();
			}
		});
		deckDS.setOnSelect(okCheck);
		sdDS.setOnSelect(okCheck);
		diag.add(nameField, gc);
		gc.insets = Syncer.DEF_INSETS;
		gc.gridy = 1;
		diag.add(deckDS.getPane(), gc);
		gc.gridy = 2;
		diag.add(sdDS.getPane(), gc);
		gc.gridx = 0;
		gc.gridy = 3;
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridwidth = 2;
		ok.setEnabled(false);
		ok.addActionListener(ev -> {
			JComboBox<Game> gameCombo = syncer.getGameCombo();
			if (gameCombo.getItemAt(0) == Syncer.EMPTY_LIST)
				gameCombo.removeItemAt(0);
			gameCombo.addItem(new Game(nameField.getText(), deckDS.getFile(), sdDS.getFile()));
			syncer.getGameRemove().setEnabled(true);
			syncer.writeChanges();
			syncer.detectTransferDir();
			mainFrame.pack();
			diag.setVisible(false);
		});
		JPanel btnBar = new JPanel(new BorderLayout(50, 0));
		btnBar.add(ok, BorderLayout.CENTER);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(a -> diag.dispose());
		btnBar.add(cancel, BorderLayout.LINE_END);
		diag.add(btnBar, gc);
		diag.pack();
		diag.setMinimumSize(diag.getSize());
	}

	void clearAndShow() {
		nameField.setText("");
		deckDS.clear();
		sdDS.clear();
	}
}