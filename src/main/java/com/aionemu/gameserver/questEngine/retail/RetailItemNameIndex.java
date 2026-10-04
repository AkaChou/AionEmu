package com.aionemu.gameserver.questEngine.retail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物品 {@code name_desc} → item_id 索引。
 * <p>
 * 真端 quest.xml 用符号名（如 {@code quest_1002b}、{@code %Quest_L_magical_30a} 的成员）描述奖励与
 * 交付物，而服务端运行时按 item_id 工作；本索引与 {@link RetailNpcNameIndex} 同构，桥接两者。
 * Index from item {@code name_desc} to item ids, the item-side twin of {@link RetailNpcNameIndex}.
 */
public final class RetailItemNameIndex {

	private static final Pattern TEMPLATE = Pattern.compile("<item_template\\b[^>]*>");
	private static final Pattern NAME_DESC = Pattern.compile("name_desc=\"([^\"]*)\"");
	private static final Pattern ITEM_ID = Pattern.compile("\\bid=\"(\\d+)\"");
	private static final RetailItemNameIndex EMPTY = new RetailItemNameIndex(Map.of());

	/** 进程内装载缓存（DCL）：见 {@link #loadItemTemplates()}。 / Process-wide load cache (DCL): see {@link #loadItemTemplates()}. */
	private static volatile RetailItemNameIndex loaded;

	/** 空索引单例。 / Empty index singleton. */
	public static RetailItemNameIndex empty() {
		return EMPTY;
	}

	private final Map<String, Integer> byName;

	private RetailItemNameIndex(Map<String, Integer> byName) {
		this.byName = Map.copyOf(byName);
	}

	/** 按符号名解析 item_id（大小写不敏感）；未解析返回 null。 / Resolves an item id by symbolic name, or null. */
	public Integer resolve(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		if (key.chars().allMatch(Character::isDigit)) {
			return Integer.valueOf(key);
		}
		return byName.get(key);
	}

	public int size() {
		return byName.size();
	}

	/** 真端物品模板目录（含 {@code name_desc} 的 item_template 文件）。 / Retail item template directory. */
	public static final String ITEM_TEMPLATE_DIR = "/aion/data/static_data/items/item/";

	/**
	 * 装载真端物品模板目录下的全部物品名索引（file / jar 两种资源协议均可）。
	 * 原生车道与旧 retail 车道共用同一份装载路径，避免出现第二个物品名事实来源。
	 * <p>
	 * 装载结果按进程缓存（DCL）：overlay 构建期 {@code RetailQuestDriver.verifyProductionCoverage}
	 * 逐个构造家族 handler，每个 handler 首次使用都要这份索引；若不缓存，每次调用都会重扫
	 * 11 个分片（约 87MB 文本 + 正则匹配）。2026-10-04 JFR 实测：静态数据窗口 18.4s 中约 8.7s
	 * 消耗在 10 次以上重复全量扫描上，因此这里必须走缓存单例。
	 * Loads the item-name index from the retail item template directory (both file and jar
	 * resource protocols). The native lane and the legacy retail lane share this single load
	 * path so no second item-name fact source exists. The result is process-cached (DCL):
	 * the overlay build constructs family handlers one by one and each first use needs this
	 * index; without the cache every call re-scans all 11 shards (~87MB of text plus regex
	 * matching). The 2026-10-04 JFR showed ~8.7s of the 18.4s static-data window spent on
	 * 10+ repeated full scans, so this path must be cached.
	 */
	public static RetailItemNameIndex loadItemTemplates() throws IOException {
		RetailItemNameIndex local = loaded;
		if (local == null) {
			synchronized (RetailItemNameIndex.class) {
				local = loaded;
				if (local == null) {
					local = scanItemTemplates();
					loaded = local;
				}
			}
		}
		return local;
	}

	/**
	 * 未缓存的目录扫描；仅在 {@link #loadItemTemplates()} 的 DCL 临界区内调用。
	 * Uncached directory scan; called only inside the {@link #loadItemTemplates()} DCL.
	 */
	private static RetailItemNameIndex scanItemTemplates() throws IOException {
		List<String> names = listXmlNames(ITEM_TEMPLATE_DIR);
		List<InputStream> inputs = new ArrayList<>(names.size());
		for (String name : names) {
			InputStream input = RetailItemNameIndex.class.getResourceAsStream(ITEM_TEMPLATE_DIR + name);
			if (input == null) {
				throw new IOException("missing resource " + ITEM_TEMPLATE_DIR + name);
			}
			inputs.add(input);
		}
		return build(inputs);
	}

	private static List<String> listXmlNames(String dir) throws IOException {
		var url = RetailItemNameIndex.class.getResource(dir);
		if (url == null) {
			return List.of();
		}
		if ("file".equals(url.getProtocol())) {
			try {
				File[] files = new File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
				if (files == null) {
					return List.of();
				}
				return Arrays.stream(files).map(File::getName).sorted().toList();
			} catch (URISyntaxException e) {
				throw new IOException("bad resource dir " + dir, e);
			}
		}
		if ("jar".equals(url.getProtocol())) {
			try {
				var connection = (JarURLConnection) url.openConnection();
				connection.setUseCaches(false);
				try (var jar = connection.getJarFile()) {
					String prefix = dir.substring(1);
					List<String> names = new ArrayList<>();
					var entries = jar.entries();
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (name.startsWith(prefix) && name.endsWith(".xml")
								&& name.indexOf('/', prefix.length()) < 0) {
							names.add(name.substring(prefix.length()));
						}
					}
					Collections.sort(names);
					return names;
				}
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException("jar listing failed for " + dir, e);
			}
		}
		throw new IOException("unsupported resource protocol " + url.getProtocol());
	}

	/**
	 * 从若干物品模板文件流构建索引；同一 name_desc 重复出现时保留首个（真端同名同 id）。
	 * Builds the index from item template streams; the first id wins on duplicate names.
	 */
	public static RetailItemNameIndex build(Collection<InputStream> templates) throws IOException {
		Map<String, Integer> byName = new LinkedHashMap<>();
		try {
			for (InputStream input : templates) {
				String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				Matcher template = TEMPLATE.matcher(text);
				while (template.find()) {
					String tag = template.group();
					Matcher name = NAME_DESC.matcher(tag);
					Matcher id = ITEM_ID.matcher(tag);
					if (!name.find() || !id.find()) {
						continue;
					}
					byName.putIfAbsent(name.group(1).toLowerCase(Locale.ROOT), Integer.valueOf(id.group(1)));
				}
			}
		} finally {
			for (InputStream input : templates) {
				input.close();
			}
		}
		return new RetailItemNameIndex(byName);
	}

	/** 批量解析名字集合；未解析项原样返回。 / Resolves names, reporting unresolved ones verbatim. */
	public record Resolution(Set<String> unresolved, Map<String, Integer> resolved) {
	}
}
