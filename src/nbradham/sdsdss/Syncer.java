package nbradham.sdsdss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class Syncer {

	private static final Game EMPTY_LIST = new Game("You need to add a game >>", null, null);
	private static final File CONFIG = new File("sync.cfg");

	private final JComboBox<Game> gameCombo = new JComboBox<>();
	private final JButton gameRemove = new JButton("-");
	private final JRadioButton toDeck = new JRadioButton("From External to Deck"),
			toSD = new JRadioButton("From Deck to External");

	JFrame mainFrame = new JFrame("Save Syncer");

	private void start() {
		SwingUtilities.invokeLater(() -> {
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.EAST;
			mainFrame.add(new JLabel("Select Game:"), gbc);
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.NORTHEAST;
			mainFrame.add(new JLabel("Transfer Direction:"), gbc);
			JPanel gamePane = new JPanel(), dirPane = new JPanel();
			try {
				Scanner scan = new Scanner(CONFIG).useDelimiter("\n");
				while (scan.hasNext())
					gameCombo.addItem(new Game(scan.next(), new File(scan.next()), new File(scan.next())));
				scan.close();
			} catch (FileNotFoundException e) {
			}
			checkEmptyList();
			gamePane.add(gameCombo);
			JButton gameAdd = new JButton("+");
			gameAdd.addActionListener(e -> {
				JDialog diag = new JDialog(mainFrame, true);
				diag.setLayout(new GridBagLayout());
				diag.setTitle("Add Game");
				GridBagConstraints gc = new GridBagConstraints();
				gc.gridx = 0;
				gc.gridy = 0;
				gc.anchor = GridBagConstraints.EAST;
				diag.add(new JLabel("Game Name:"), gc);
				gc.gridy = 1;
				diag.add(new JLabel("Deck Save Directory:"), gc);
				gc.gridy = 2;
				diag.add(new JLabel("External Save Directory"), gc);
				gc.gridx = 1;
				gc.gridy = 0;
				gc.anchor = GridBagConstraints.WEST;
				gc.insets = new Insets(4, 4, 0, 0);
				JTextField nameField = new JTextField(25);
				DirectorySelector deckDS = new DirectorySelector("Select DECK save directory.", diag),
						sdDS = new DirectorySelector("Select EXTERNAL save directory.", diag);
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
				diag.add(nameField, gc);
				gc.insets = new Insets(0, 0, 0, 0);
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
					if (gameCombo.getItemAt(0) == EMPTY_LIST)
						gameCombo.removeItemAt(0);
					gameCombo.addItem(new Game(nameField.getText(), deckDS.getFile(), sdDS.getFile()));
					gameRemove.setEnabled(true);
					writeChanges();
					mainFrame.pack();
					diag.dispose();
				});
				diag.add(ok, gc);
				diag.pack();
				diag.setVisible(true);
			});
			gamePane.add(gameAdd);
			gameRemove.addActionListener(e -> {
				gameCombo.removeItemAt(gameCombo.getSelectedIndex());
				checkEmptyList();
				writeChanges();
			});
			gamePane.add(gameRemove);
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			mainFrame.add(gamePane, gbc);
			dirPane.setLayout(new BoxLayout(dirPane, BoxLayout.PAGE_AXIS));
			detectTransferDir();
			ButtonGroup bg = new ButtonGroup();
			bg.add(toDeck);
			bg.add(toSD);
			dirPane.add(toDeck);
			dirPane.add(toSD);
			gbc.gridy = 1;
			mainFrame.add(dirPane, gbc);
			mainFrame.pack();
			mainFrame.setVisible(true);
		});
	}

	private void checkEmptyList() {
		if (gameCombo.getItemCount() == 0) {
			gameCombo.addItem(EMPTY_LIST);
			gameRemove.setEnabled(false);
		}
	}

	private void writeChanges() {
		try {
			PrintWriter fos = new PrintWriter(CONFIG);
			for (byte i = 0; i < gameCombo.getItemCount(); ++i) {
				Game g = gameCombo.getItemAt(i);
				fos.println(g.name());
				fos.println(g.deckDir());
				fos.println(g.sdDir());
			}
			fos.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	private void detectTransferDir() {
		Game g = (Game) gameCombo.getSelectedItem();
		if (g == EMPTY_LIST) {
			toSD.setEnabled(false);
			toDeck.setEnabled(false);
		} else {
			toSD.setEnabled(true);
			toDeck.setEnabled(true);
			if (getLastModified(g.deckDir()) > getLastModified(g.sdDir()))
				toSD.setSelected(true);
			else
				toDeck.setSelected(true);
		}
	}

	private static long getLastModified(File dir) {
		long latest = -1;
		System.out.println(dir);
		File[] files = dir.listFiles();
		if (files != null)
			for (File f : files)
				latest = Math.max(latest, f.lastModified());
		return latest;
	}

	public static void main(String[] args) {
		new Syncer().start();
	}
}