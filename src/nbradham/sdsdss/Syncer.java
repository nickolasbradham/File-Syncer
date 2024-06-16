package nbradham.sdsdss;

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
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class Syncer {

	private static final Game EMPTY_LIST = new Game("You need to add a game >>", null, null);

	JFrame mainFrame = new JFrame("Save Syncer");

	private void start() {
		SwingUtilities.invokeLater(() -> {
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel gamePane = new JPanel();
			gamePane.setBorder(new TitledBorder("Game"));
			JComboBox<Game> gameCombo = new JComboBox<>(new Game[] { EMPTY_LIST });
			gamePane.add(gameCombo);
			JButton gameAdd = new JButton("+"), gameRemove = new JButton("-");
			gameAdd.addActionListener(e -> {
				JDialog diag = new JDialog(mainFrame, true);
				diag.setLayout(new GridBagLayout());
				diag.setTitle("Add Game");
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = gbc.gridy = 0;
				gbc.anchor = GridBagConstraints.EAST;
				diag.add(new JLabel("Game Name:"), gbc);
				gbc.gridy = 1;
				diag.add(new JLabel("Deck Save Directory:"), gbc);
				gbc.gridy = 2;
				diag.add(new JLabel("SD Save Directory"), gbc);
				gbc.gridx = 1;
				gbc.gridy = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(4, 4, 0, 0);
				JTextField nameField = new JTextField(25);
				DirectorySelector deckDS = new DirectorySelector("Select DECK save directory.", diag),
						sdDS = new DirectorySelector("Select SD card save directory.", diag);
				JButton ok = new JButton("Add Game");
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
				diag.add(nameField, gbc);
				gbc.insets = new Insets(0, 0, 0, 0);
				gbc.gridy = 1;
				diag.add(deckDS.getPane(), gbc);
				gbc.gridy = 2;
				diag.add(sdDS.getPane(), gbc);
				gbc.gridx = 0;
				gbc.gridy = 3;
				gbc.anchor = GridBagConstraints.CENTER;
				gbc.gridwidth = 2;
				ok.setEnabled(false);
				ok.addActionListener(ev -> {
					if (gameCombo.getItemAt(0) == EMPTY_LIST)
						gameCombo.removeItemAt(0);
					gameCombo.addItem(new Game(nameField.getText(), deckDS.getFile(), sdDS.getFile()));
					gameRemove.setEnabled(true);
					diag.dispose();
				});
				diag.add(ok, gbc);
				diag.pack();
				diag.setVisible(true);
			});
			gamePane.add(gameAdd);
			gameRemove.setEnabled(false);
			gameRemove.addActionListener(e -> {
				gameCombo.removeItemAt(gameCombo.getSelectedIndex());
				if (gameCombo.getItemCount() == 0) {
					gameCombo.addItem(EMPTY_LIST);
					gameRemove.setEnabled(false);
				}
			});
			gamePane.add(gameRemove);
			mainFrame.add(gamePane);
			mainFrame.pack();
			mainFrame.setVisible(true);
		});
	}

	public static void main(String[] args) {
		new Syncer().start();
	}
}