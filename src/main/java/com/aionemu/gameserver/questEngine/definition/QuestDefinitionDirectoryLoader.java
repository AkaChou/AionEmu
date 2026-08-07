package com.aionemu.gameserver.questEngine.definition;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Scans every packaged quest definition for compile and consistency checks. */
public final class QuestDefinitionDirectoryLoader {
	private static final String QUEST_DIRECTORY =
		"aion/data/static_data/quest_definition/quests";

	private QuestDefinitionDirectoryLoader() {
	}

	/**
	 * Scan and compile every {@code quests/<numericQuestId>.xml} resource.
	 * This is deliberately separate from production ownership: the directory also contains
	 * candidates that are not yet safe to install as live owners.
	 */
	public static QuestCatalog compile(ClassLoader loader) {
		Objects.requireNonNull(loader, "loader");
		URL directoryUrl = loader.getResource(QUEST_DIRECTORY);
		if (directoryUrl == null) {
			fail("QUEST_DIR_MISSING", QUEST_DIRECTORY);
		}

		List<String> resources = listResources(directoryUrl);
		if (resources.isEmpty()) {
			fail("QUEST_DIR_EMPTY", QUEST_DIRECTORY);
		}
		resources.sort(Comparator.comparingInt(QuestDefinitionDirectoryLoader::questId)
			.thenComparing(Comparator.naturalOrder()));

		List<CompiledQuestDefinition> definitions = QuestDefinitionCatalogManifest.compileResources(
			resources, loader, "QUEST_RESOURCE_MISSING", "QUEST_RESOURCE_READ_FAILED");
		for (int i = 0; i < resources.size(); i++) {
			String resource = resources.get(i);
			int filenameId = questId(resource);
			if (definitions.get(i).id() != filenameId) {
				fail("QUEST_FILENAME_ID_MISMATCH", resource + " expected=" + filenameId
					+ " actual=" + definitions.get(i).id());
			}
		}
		return new ImmutableQuestCatalog(definitions);
	}

	private static List<String> listResources(URL directoryUrl) {
		List<String> resources = new ArrayList<>();
		if ("file".equals(directoryUrl.getProtocol())) {
			try {
				File directory = new File(directoryUrl.toURI());
				if (!directory.isDirectory()) {
					fail("QUEST_DIR_UNREADABLE", QUEST_DIRECTORY);
				}
				File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".xml"));
				if (files == null) {
					fail("QUEST_DIR_UNREADABLE", QUEST_DIRECTORY);
				}
				for (File file : files) {
					resources.add(QUEST_DIRECTORY + "/" + file.getName());
				}
				return resources;
			} catch (URISyntaxException e) {
				fail("QUEST_DIR_BAD_URI", directoryUrl.toString());
			}
		}

		if ("jar".equals(directoryUrl.getProtocol())) {
			try {
				JarURLConnection connection = (JarURLConnection) directoryUrl.openConnection();
				connection.setUseCaches(false);
				try (JarFile jar = connection.getJarFile()) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String name = entry.getName();
						String prefix = QUEST_DIRECTORY + "/";
						if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".xml")
							&& name.substring(prefix.length()).indexOf('/') < 0) {
							resources.add(name);
						}
					}
				}
				return resources;
			} catch (IOException e) {
				fail("QUEST_DIR_READ_FAILED", QUEST_DIRECTORY);
			}
		}

		fail("QUEST_DIR_UNSUPPORTED_PROTOCOL", directoryUrl.getProtocol());
		return resources;
	}

	private static int questId(String resource) {
		String filename = resource.substring(resource.lastIndexOf('/') + 1, resource.length() - ".xml".length());
		try {
			return Integer.parseInt(filename);
		} catch (NumberFormatException e) {
			fail("QUEST_FILENAME_INVALID", resource);
			return 0;
		}
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
