package nbradham.sdsdss;

import java.awt.BorderLayout;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

final class Syncer {

	static final Insets DEF_INSETS = new Insets(5, 5, 5, 5);
	static final Game EMPTY_LIST = new Game("You need to add a game >>", null, null);

	private static final File CONFIG = new File("sync.cfg");

	private final JComboBox<Game> gameCombo = new JComboBox<>();
	private final JButton gameRemove = new JButton("-"), go = new JButton("Transfer");
	private final JRadioButton toDeck = new JRadioButton("Copy from External. Overwrite Local"),
			toSD = new JRadioButton("Copy from Local. Overwrite External");

	JFrame mainFrame = new JFrame("Save Syncer");

	private AddDialog diag;

	void writeChanges() {
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

	void detectTransferDir() {
		Game g = (Game) gameCombo.getSelectedItem();
		boolean enable = g != null && g != EMPTY_LIST;
		setTransferEnabled(enable);
		if (enable)
			if (getLastModified(g.deckDir()) > getLastModified(g.sdDir()))
				toSD.setSelected(true);
			else
				toDeck.setSelected(true);
	}

	private void start() {
		SwingUtilities.invokeLater(() -> {
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.setLayout(new GridBagLayout());
			JButton gameAdd = new JButton("+");
			JTextField stat = new JTextField("Ready.", 13);
			stat.setEditable(false);
			stat.setFocusable(false);
			go.setEnabled(false);
			go.addActionListener(e -> {
				setTransferEnabled(false);
				SwingUtilities.invokeLater(() -> stat.setText("Working..."));
				new Thread(() -> {
					Game g = (Game) gameCombo.getSelectedItem();
					boolean toLoc = toDeck.isSelected();
					FileOp rootDir = new FileOp(toLoc ? g.sdDir() : g.deckDir(), toLoc ? g.deckDir() : g.sdDir());
					SwingUtilities.invokeLater(() -> stat.setText("Clearing destination..."));
					Queue<File> del = new LinkedList<>();
					for (File f : rootDir.dest().listFiles())
						del.offer(f);
					while (!del.isEmpty()) {
						File f = del.poll();
						if (!f.delete()) {
							for (File cf : f.listFiles())
								del.offer(cf);
							del.offer(f);
						}
					}
					SwingUtilities.invokeLater(() -> stat.setText("Copying from source..."));
					Queue<FileOp> q = new LinkedList<>();
					q.offer(rootDir);
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
			gbc.insets = DEF_INSETS;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.EAST;
			mainFrame.add(new JLabel("Select Game:"), gbc);
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.NORTHEAST;
			mainFrame.add(new JLabel("Overwrite Direction:"), gbc);
			JPanel gamePane = new JPanel(), dirPane = new JPanel();
			try {
				Scanner scan = new Scanner(CONFIG).useDelimiter("\n");
				while (scan.hasNext())
					gameCombo.addItem(new Game(scan.next().strip(), new File(scan.next().strip()),
							new File(scan.next().strip())));
				scan.close();
			} catch (FileNotFoundException e) {
			}
			checkEmptyList();
			gameCombo.addActionListener(e -> detectTransferDir());
			gamePane.add(gameCombo);
			gameAdd.addActionListener(e -> {
				if(diag == null)
					diag = new AddDialog(mainFrame, this);
				else
					diag.clearAndShow();
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
			gbc.insets = DEF_INSETS;
			mainFrame.add(dirPane, gbc);
			JPanel actBar = new JPanel(new BorderLayout(10, 0));
			actBar.add(go, BorderLayout.CENTER);
			actBar.add(stat, BorderLayout.LINE_START);
			JButton exit = new JButton("Exit");
			exit.addActionListener(e -> mainFrame.dispose());
			actBar.add(exit, BorderLayout.LINE_END);
			gbc.gridwidth = 2;
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			mainFrame.add(actBar, gbc);
			mainFrame.pack();
			mainFrame.setMinimumSize(mainFrame.getSize());
			mainFrame.setVisible(true);
		});
	}

	private void checkEmptyList() {
		if (gameCombo.getItemCount() == 0) {
			gameCombo.addItem(EMPTY_LIST);
			gameRemove.setEnabled(false);
		}
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
		while (!q.isEmpty()) {
			File f = q.poll();
			if (f.isDirectory())
				for (File t : f.listFiles())
					q.offer(t);
			else
				try {
					latest = Math.max(latest, Math.max(f.lastModified(),
							Files.readAttributes(f.toPath(), BasicFileAttributes.class).creationTime().toMillis()));
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return latest;
	}

	public static void main(String[] args) {
		new Syncer().start();
	}

	JComboBox<Game> getGameCombo() {
		return gameCombo;
	}

	JButton getGameRemove() {
		return gameRemove;
	}
}