package nbradham.sdsdss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Queue;
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

	private static final Insets IN_DEF = new Insets(0, 0, 0, 0), IN_TEXT = new Insets(4, 4, 0, 0);
	private static final Game EMPTY_LIST = new Game("You need to add a game >>", null, null);
	private static final File CONFIG = new File("sync.cfg");

	private final JComboBox<Game> gameCombo = new JComboBox<>();
	private final JButton gameRemove = new JButton("-"), go = new JButton("Transfer");
	private final JRadioButton toDeck = new JRadioButton("Copy from External. Overwrite Local"),
			toSD = new JRadioButton("Copy from Local. Overwrite External");

	JFrame mainFrame = new JFrame("Save Syncer");

	private void start() {
		SwingUtilities.invokeLater(() -> {
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.setLayout(new GridBagLayout());
			JButton gameAdd = new JButton("+");
			JTextField stat = new JTextField("Ready.");
			stat.setEditable(false);
			go.setEnabled(false);
			go.addActionListener(e -> {
				setTransferEnabled(false);
				stat.setText("Working...");
				new Thread(() -> {
					Game g = (Game) gameCombo.getSelectedItem();
					boolean toLoc = toDeck.isSelected();
					Queue<FileOp> q = new LinkedList<>();
					q.offer(new FileOp(toLoc ? g.sdDir() : g.deckDir(), toLoc ? g.deckDir() : g.sdDir()));
					while (!q.isEmpty()) {
						FileOp fo = q.poll();
						File src = fo.src(), dest = fo.dest();
						if (src.isDirectory()) {
							dest.mkdirs();
							for (File f : src.listFiles())
								q.offer(new FileOp(f, new File(dest, f.getName())));
						} else
							try {
								Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
					}
					Toolkit.getDefaultToolkit().beep();
					SwingUtilities.invokeLater(() -> stat.setText("Done!"));
				}).start();
			});
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.EAST;
			mainFrame.add(new JLabel("Select Game:"), gbc);
			gbc.gridy = 2;
			mainFrame.add(go, gbc);
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.NORTHEAST;
			mainFrame.add(new JLabel("Overwrite Direction:"), gbc);
			JPanel gamePane = new JPanel(), dirPane = new JPanel();
			try {
				Scanner scan = new Scanner(CONFIG).useDelimiter("\n");
				while (scan.hasNext())
					gameCombo.addItem(new Game(scan.next().strip(), new File(scan.next().strip()), new File(scan.next().strip())));
				scan.close();
			} catch (FileNotFoundException e) {
			}
			checkEmptyList();
			gameCombo.addActionListener(e -> detectTransferDir());
			gamePane.add(gameCombo);
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
				gc.insets = IN_TEXT;
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
				gc.insets = IN_DEF;
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
					detectTransferDir();
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
				setTransferEnabled(gameCombo.getItemAt(0) != EMPTY_LIST);
				mainFrame.pack();
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
			gbc.gridy = 2;
			gbc.insets = IN_TEXT;
			mainFrame.add(stat, gbc);
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
			if (gameCombo.getItemAt(0) != EMPTY_LIST)
				for (byte i = 0; i < gameCombo.getItemCount(); ++i) {
					Game g = gameCombo.getItemAt(i);
					fos.println(g.name().strip());
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
		boolean enable = g != null && g != EMPTY_LIST;
		setTransferEnabled(enable);
		if (enable)
			if (getLastModified(g.deckDir()) > getLastModified(g.sdDir()))
				toSD.setSelected(true);
			else
				toDeck.setSelected(true);
	}

	private void setTransferEnabled(boolean enabled) {
		toDeck.setEnabled(enabled);
		toSD.setEnabled(enabled);
		go.setEnabled(enabled);
	}

	private static long getLastModified(File dir) {
		long latest = -1;
		Queue<File> q = new LinkedList<>();
		q.offer(dir);
		while(!q.isEmpty()) {
			File f = q.poll();
			if(f.isDirectory())
				for(File t : f.listFiles())
					q.offer(t);
			else
				latest = Math.max(latest, f.lastModified());
		}
		return latest;
	}

	public static void main(String[] args) {
		new Syncer().start();
	}
}