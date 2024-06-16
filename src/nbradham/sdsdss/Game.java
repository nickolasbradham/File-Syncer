package nbradham.sdsdss;

import java.io.File;

record Game(String name, File deckDir, File sdDir) {

	@Override
	public String toString() {
		return name;
	}
}