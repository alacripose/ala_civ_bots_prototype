package dev.ensouled;

import java.io.IOException;
import java.nio.file.Files;

import dev.ensouled.persist.MindJournal;

/**
 * A mind cannot act without writing its decision ahead (§49.3), so a test that ticks one needs a
 * journal. This installs a throwaway one in the system temp directory — the same durable file the
 * server uses, so the test exercises the real write-ahead path rather than a stub.
 */
final class TestJournal {
	static MindJournal install(String label) throws IOException {
		MindJournal journal = new MindJournal(
				Files.createTempFile("ensouled-" + label.toLowerCase() + "-", ".journal").toFile());
		MindJournal.install(journal);
		return journal;
	}

	static void uninstall(MindJournal journal) {
		if (journal == null) {
			return;
		}
		MindJournal.uninstall(journal);
		try {
			journal.close();
		} catch (Exception ignored) {
			// A test journal that cannot close is still out of the way; the file is in temp.
		}
	}

	private TestJournal() {
	}
}
