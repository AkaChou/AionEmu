package com.aionemu.gameserver.cache;


import com.aionemu.boot.i18n.I18n;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.HTMLConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTML/XHTML 文件缓存：扫描目录、压缩空白并按国家码选择本地化路径。
 * HTML/XHTML file cache that scans directories, compacts whitespace and picks locale by country code.
 *
 * @author Layane, nbali, savormix, hex1r0, lord_rex
 */
@Slf4j
public final class HTMLCache {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<HTMLCache> instanceProvider;

	/**
	 * 仅接受目录或 {@code .xhtml} 文件。
	 * Accepts directories or {@code .xhtml} files only.
	 */
	private static final FileFilter HTML_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().endsWith(".xhtml");
		}
	};

	/**
	 * HTML 根目录。
	 * HTML root directory.
	 *
	 * @return 根目录 / root directory
	 */
	private static File htmlRoot() {
		return Config.dataFile(HTMLConfig.HTML_ROOT);
	}

	/**
	 * 非 Spring 环境下的单例持有者。
	 * Singleton holder for non-Spring fallback.
	 */
	private static final class SingletonHolder {

		private static final HTMLCache INSTANCE = new HTMLCache();
	}

	/**
	 * 获取缓存实例（优先 Spring provider）。
	 * Returns cache instance (prefers Spring provider).
	 *
	 * @return 缓存实例 / cache instance
	 */
	public static HTMLCache getInstance() {
		ObjectProvider<HTMLCache> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<HTMLCache> provider) {
		instanceProvider = provider;
	}

	/**
	 * 相对路径 → HTML 内容。
	 * Relative path → HTML content.
	 */
	private Map<String, String> cache = new LinkedHashMap<String, String>(16000);

	/**
	 * 已加载文件数。
	 * Loaded file count.
	 */
	private int loadedFiles;

	/**
	 * 缓存字符总长度。
	 * Total cached character size.
	 */
	private int size;

	/**
	 * 构造并首次加载缓存。
	 * Constructs and performs the initial load.
	 */
	public HTMLCache() {
		reload(false);
	}

	/**
	 * 重新加载缓存；可选删除磁盘缓存文件后从目录重建。
	 * Reloads cache; optionally deletes the on-disk cache file and rebuilds from directories.
	 *
	 * @param deleteCacheFile 是否删除磁盘缓存 / whether to delete on-disk cache
	 */
	@SuppressWarnings("unchecked")
	public synchronized void reload(boolean deleteCacheFile) {
		cache.clear();
		loadedFiles = 0;
		size = 0;

		final File cacheFile = getCacheFile();

		if (deleteCacheFile && cacheFile.exists()) {
			log.info(I18n.get("log.a5092abd3e6f"));

			cacheFile.delete();
		}
		log.info(I18n.get("log.c7a3e61e3ad9"));

		if (cacheFile.exists()) {
			log.info(I18n.get("log.f60605c6b6c2"));

			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(getCacheFile())));

				cache = (Map<String, String>) ois.readObject();

				for (String html : cache.values()) {
					loadedFiles++;
					size += html.length();
				}
			} catch (Exception e) {
				log.warn(I18n.get("log.da39a3ee5e6b", e));

				reload(true);
				return;
			} finally {
				IOUtils.closeQuietly(ois);
			}
		} else {
			parseDir(htmlRoot());
		}

		log.info(String.valueOf(this));

		if (cacheFile.exists()) {
			log.info(I18n.get("log.a9b5faea9e61"));
		} else {
			log.info(I18n.get("log.5582d814a952"));

			final StringBuilder sb = new StringBuilder(8192);

			for (Entry<String, String> entry : cache.entrySet()) {
				try {
					final String oldHtml = entry.getValue();
					final String newHtml = compactHtml(sb, oldHtml);

					size -= oldHtml.length();
					size += newHtml.length();

					entry.setValue(newHtml);
				} catch (RuntimeException e) {
					log.warn(I18n.get("log.948e032dffdf", entry.getKey(), e));
				}
			}
			log.info(String.valueOf(this));
		}

		if (!cacheFile.exists()) {
			log.info(I18n.get("log.6a3fffecfa57"));

			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getCacheFile())));

				oos.writeObject(cache);
			} catch (IOException e) {
				log.warn(I18n.get("log.da39a3ee5e6b", e));
			} finally {
				IOUtils.closeQuietly(oos);
			}
		}
	}

	/**
	 * 磁盘缓存文件路径。
	 * On-disk cache file path.
	 *
	 * @return 缓存文件 / cache file
	 */
	private File getCacheFile() {
		return Config.cacheFile(HTMLConfig.HTML_CACHE_FILE + ".i18n");
	}

	/**
	 * 需要压缩空白的标签片段表。
	 * Tag fragments compacted for whitespace.
	 */
	private static final String[] TAGS_TO_COMPACT;

	static {
		final String[] tagsToCompact = { "html", "title", "body", "br", "br1", "p", "table", "tr", "td" };

		final List<String> list = new ArrayList<String>();

		for (String tag : tagsToCompact) {
			list.add("<" + tag + ">");
			list.add("</" + tag + ">");
			list.add("<" + tag + "/>");
			list.add("<" + tag + " />");
		}

		final List<String> list2 = new ArrayList<String>();

		for (String tag : list) {
			list2.add(tag);
			list2.add(tag + " ");
			list2.add(" " + tag);
		}
		TAGS_TO_COMPACT = list2.toArray(new String[list.size()]);
	}

	/**
	 * 压缩 HTML 空白与标签周围空格。
	 * Compacts HTML whitespace and spaces around tags.
	 *
	 * @param sb 可复用缓冲 / reusable buffer
	 * @param html 原始 HTML / raw HTML
	 * @return 压缩后的 HTML / compacted HTML
	 */
	private String compactHtml(StringBuilder sb, String html) {
		sb.setLength(0);
		sb.append(html);

		for (int i = 0; i < sb.length(); i++) {
			if (Character.isWhitespace(sb.charAt(i))) {
				sb.setCharAt(i, ' ');
			}
		}
		replaceAll(sb, "  ", " ");

		replaceAll(sb, "< ", "<");
		replaceAll(sb, " >", ">");

		for (int i = 0; i < TAGS_TO_COMPACT.length; i += 3) {
			replaceAll(sb, TAGS_TO_COMPACT[i + 1], TAGS_TO_COMPACT[i]);
			replaceAll(sb, TAGS_TO_COMPACT[i + 2], TAGS_TO_COMPACT[i]);
		}
		replaceAll(sb, "  ", " ");

		// String.trim() without additional garbage
		int fromIndex = 0;
		int toIndex = sb.length();

		while (fromIndex < toIndex && sb.charAt(fromIndex) == ' ') {
			fromIndex++;
		}

		while (fromIndex < toIndex && sb.charAt(toIndex - 1) == ' ') {
			toIndex--;
		}
		return sb.substring(fromIndex, toIndex);
	}

	/**
	 * 在 {@link StringBuilder} 中替换全部匹配。
	 * Replaces all matches inside a {@link StringBuilder}.
	 *
	 * @param sb 缓冲 / buffer
	 * @param pattern 查找模式 / pattern
	 * @param value 替换内容 / replacement
	 */
	private void replaceAll(StringBuilder sb, String pattern, String value) {
		for (int index = 0; (index = sb.indexOf(pattern, index)) != -1;) {
			sb.replace(index, index + pattern.length(), value);
		}
	}

	/**
	 * 重新解析指定路径并刷新缓存条目。
	 * Re-parses the given path and refreshes cache entries.
	 *
	 * @param f 文件或目录 / file or directory
	 */
	public void reloadPath(File f) {
		parseDir(f);

		log.info(I18n.get("log.46cc82445a43"));
	}

	/**
	 * 递归解析目录下全部可加载 HTML。
	 * Recursively parses all loadable HTML under a directory.
	 *
	 * @param dir 目录 / directory
	 */
	public void parseDir(File dir) {
		File[] files = dir.listFiles(HTML_FILTER);
		if (files == null) {
			log.warn(I18n.get("log.7a9db1673317", dir.getPath()));
			return;
		}
		for (File file : files) {
			if (!file.isDirectory()) {
				loadFile(file);
			} else {
				parseDir(file);
			}
		}
	}

	/**
	 * 加载单个 HTML 文件到缓存。
	 * Loads a single HTML file into the cache.
	 *
	 * @param file 文件 / file
	 * @return 内容；不可加载时返回 null / content, or null when not loadable
	 */
	public String loadFile(File file) {
		if (isLoadable(file)) {
			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(file));
				byte[] raw = new byte[bis.available()];
				bis.read(raw);

				String content = new String(raw, HTMLConfig.HTML_ENCODING);
				String relpath = getRelativePath(htmlRoot(), file);

				size += content.length();

				String oldContent = cache.get(relpath);
				if (oldContent == null) {
					loadedFiles++;
				} else {
					size -= oldContent.length();
				}
				cache.put(relpath, content);

				return content;
			} catch (Exception e) {
				log.warn(I18n.get("log.c8b1dfe19197", e));
			} finally {
				IOUtils.closeQuietly(bis);
			}
		}
		return null;
	}

	/**
	 * 按服务器国家码解析本地化路径并取 HTML。
	 * Resolves the localized path by server country code and returns HTML.
	 *
	 * @param path 相对路径 / relative path
	 * @return HTML 内容或 null / HTML or null
	 */
	public String getHTML(String path) {
		return cache.get(localizedPath(path, GSConfig.SERVER_COUNTRY_CODE));
	}

	/**
	 * 国家码非 5 时将 {@code .xhtml} 映射为 {@code .en.xhtml}。
	 * Maps {@code .xhtml} to {@code .en.xhtml} when country code is not 5.
	 *
	 * @param path 相对路径 / relative path
	 * @param countryCode 国家码 / country code
	 * @return 本地化路径 / localized path
	 */
	static String localizedPath(String path, int countryCode) {
		if (path == null || countryCode == 5 || path.endsWith(".en.xhtml")) {
			return path;
		}
		int extension = path.lastIndexOf(".xhtml");
		return extension < 0 ? path : path.substring(0, extension) + ".en.xhtml";
	}

	/**
	 * 文件是否可加载。
	 * Whether the file is loadable.
	 *
	 * @param file 文件 / file
	 * @return 是否可加载 / loadable
	 */
	private boolean isLoadable(File file) {
		return file.exists() && !file.isDirectory() && HTML_FILTER.accept(file);
	}

	/**
	 * 本地化路径是否存在于缓存。
	 * Whether the localized path exists in cache.
	 *
	 * @param path 相对路径 / relative path
	 * @return 是否存在 / whether present
	 */
	public boolean pathExists(String path) {
		return cache.containsKey(localizedPath(path, GSConfig.SERVER_COUNTRY_CODE));
	}

	/**
	 * 缓存摘要。
	 * Cache summary.
	 *
	 * @return 摘要字符串 / summary string
	 */
	@Override
	public String toString() {
		return I18n.get("log.4649a5706d31", String.format("%.3f", (float) size / 1024), loadedFiles);
	}

	/**
	 * 计算文件相对 base 的 URI 路径。
	 * Computes the URI path of file relative to base.
	 *
	 * @param base 基准目录 / base directory
	 * @param file 文件 / file
	 * @return 相对路径 / relative path
	 */
	public static String getRelativePath(File base, File file) {
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}
}
