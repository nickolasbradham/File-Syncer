package nbradham.sdsdss;

import java.awt.Component;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

final class DirectorySelector {

	private final JPanel pane = new JPanel();
	private final JTextField field = new JTextField(20);
	private final JFileChooser jfc = new JFileChooser();
	private Runnable onSel;

	DirectorySelector(String jfcTitle, Component parent) {
		field.setEditable(false);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle(jfcTitle);
		pane.add(field);
		JButton browse = new JButton("Browse");
		browse.addActionListener(e -> {
			if (jfc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
				field.setText(jfc.getSelectedFile().toString());
				onSel.run();
			}
		});
		pane.add(browse);
	}

	JPanel getPane() {
		return pane;
	}

	void setOnSelect(Runnable onSelect) {
		onSel = onSelect;
	}

	boolean isDirSelected() {
		return jfc.getSelectedFile() != null;
	}

	File getFile() {
		return jfc.getSelectedFile();
	}
}