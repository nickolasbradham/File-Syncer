package nbradham.sdsdss;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

final class Syncer {

	JFrame mainFrame = new JFrame("Save Syncer");

	private void start() {
		SwingUtilities.invokeLater(() -> {
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel gamePane = new JPanel();
			gamePane.setBorder(new TitledBorder("Game"));
			JComboBox<Game> gameCombo = new JComboBox<>(new Game[] { new Game("Select Game") });
			gamePane.add(gameCombo);
			JButton gameAdd = new JButton("+"), gameRemove = new JButton("-");

			gamePane.add(gameAdd);
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