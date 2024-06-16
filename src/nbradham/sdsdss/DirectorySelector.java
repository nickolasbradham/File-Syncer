package nbradham.sdsdss;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

final class DirectorySelector {

	private final JPanel pane = new JPanel();
	private final JTextField field;

	DirectorySelector(String defaultText) {
		(field = new JTextField(defaultText)).setEditable(false);
		pane.add(field);

		JButton browse = new JButton("Browse");
		pane.add(browse);
	}
	
	JPanel getPane() {
		return pane;
	}
}