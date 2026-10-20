package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.QuestDataResources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * 世界实刷 npc id 集（懒加载一次）：扫 {@code static_data/spawns/**} 与 {@code static_data/town_spawns/**}
 * 的 {@code npc_id="N"}。消费方是原版元数据编译器的掉落死 id 修复——原版模板 id 可能已退役、
 * 同显示名兄弟 id 才是实刷体（2631 实证：213775 已退役、实刷 236924）。
 * <p>
 * The set of world-spawned npc ids (loaded lazily once) by scanning {@code npc_id="N"} across
 * {@code static_data/spawns/**} and {@code static_data/town_spawns/**}. Consumers use it for the
 * retail metadata compiler's dead-id drop repair: a retail template id may be retired while a
 * same-display-name sibling id is the live spawn (proven by quest 2631).
 */
public final class RetailSpawnedNpcIds {

	private static final List<String> SPAWN_DIRS = List.of(
		"/aion/data/static_data/spawns/", "/aion/data/static_data/town_spawns/");
	private static final ThreadLocal<XMLInputFactory> INPUT_FACTORY = ThreadLocal.withInitial(() -> {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		return factory;
	});

	private static volatile Set<Integer> loaded;

	private RetailSpawnedNpcIds() {
	}

	/** 实刷 id 集；资源缺失时为空集（消费方退回精确解析）。 / Spawned ids; empty when absent (callers fall back to exact resolution). */
	public static Set<Integer> load() {
		Set<Integer> ids = loaded;
		if (ids == null) {
			synchronized (RetailSpawnedNpcIds.class) {
				ids = loaded;
				if (ids == null) {
					ids = scan();
					loaded = ids;
				}
			}
		}
		return ids;
	}

	private static Set<Integer> scan() {
		Set<Integer> ids = new HashSet<>();
		for (String dir : SPAWN_DIRS) {
			try {
				scanDir(dir, ids);
			} catch (IOException ignored) {
				// 目录缺失 = 该部署无此类刷怪数据；空集让消费方保持精确解析。
				// A missing directory just means no such spawn data; consumers keep exact resolution.
			}
		}
		return Set.copyOf(ids);
	}

	private static void scanDir(String dir, Set<Integer> ids) throws IOException {
		var url = QuestDataResources.url(dir);
		if (url == null) {
			return;
		}
		if ("file".equals(url.getProtocol())) {
			try (var paths = Files.walk(Path.of(URI.create(url.toExternalForm().replace(" ", "%20"))))) {
				for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
						.filter(path -> path.getFileName().toString().endsWith(".xml"))::iterator) {
						try (InputStream input = Files.newInputStream(path)) {
							collect(input, ids);
						}
				}
			}
			return;
		}
		// jar 协议：按目录前缀递归枚举（与 RetailQuestDriver.listXmlNames 同思路，递归到任意深度）。
		// Jar protocol: recursive prefix enumeration (same idea as RetailQuestDriver.listXmlNames).
		if ("jar".equals(url.getProtocol())) {
			var connection = (java.net.JarURLConnection) url.openConnection();
			connection.setUseCaches(false);
			try (var jar = connection.getJarFile()) {
				String prefix = dir.substring(1);
				var entries = jar.entries();
				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.startsWith(prefix) && name.endsWith(".xml")) {
						try (var input = jar.getInputStream(jar.getEntry(name))) {
								collect(input, ids);
						}
					}
				}
			}
			return;
		}
		throw new IOException("unsupported resource protocol " + url.getProtocol());
	}

	private static void collect(InputStream input, Set<Integer> ids) throws IOException {
		try {
			XMLStreamReader reader = INPUT_FACTORY.get().createXMLStreamReader(input);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				String npcId = reader.getAttributeValue(null, "npc_id");
				if (npcId != null && !npcId.isEmpty() && npcId.chars().allMatch(Character::isDigit)) {
					ids.add(Integer.parseInt(npcId));
				}
			}
			reader.close();
		} catch (XMLStreamException e) {
			throw new IOException("malformed spawn stream", e);
		}
	}
}
