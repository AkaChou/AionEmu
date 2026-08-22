package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * 离线模板分片写出器：把大型模板 XML 按目标分片数均衡字节大小，
 * 供资源目录直接加载。
 * Offline template shard writer: splits large template XML into a requested number of source files
 * with balanced byte sizes for direct resource loading.
 *
 * <p>运行时加载器不会调用此类，也不会在启动时写入缓存；它仅用于离线生成或维护
 * 已提交的源分片。
 * The runtime loader never calls this class and never writes a cache at startup; it is only for
 * offline generation or maintenance of committed source shards.
 *
 * <p>分片文件命名为 {@code {filePrefix}_{startId}_{endId}.xml}，其中 startId/endId 是该分片内
 * 数值最小和最大的模板 ID（例如 {@code item_template_10000_20000.xml}）。
 * 分片写入临时文件后原子改名，全部成功后才清理旧分片，
 * 避免中途失败留下新旧混杂的分片集合。
 * Shard files are named {@code {filePrefix}_{startId}_{endId}.xml} where startId/endId are the numeric
 * minimum/maximum template ids in the shard (e.g. {@code item_template_10000_20000.xml}).
 * Shards are written to temp files and atomically renamed, and
 * stale shards are cleaned only after every write succeeded, so a failed run never leaves a mixed set.
 */
final class TemplateShardWriter {

	/** 分片文件名模式：{prefix}_{数字}_{数字}.xml / shard file pattern: {prefix}_{digits}_{digits}.xml */
	private static final String SHARD_FILE_FORMAT = "%s_%s_%s.xml";

	private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();
	private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();

	private TemplateShardWriter() {
	}

	/**
	 * 分片是否已是最新：存在分片且没有任何源文件比分片新。
	 * Whether shards are up to date: shards exist and no source file is newer than any shard.
	 *
	 * @param sources 源文件列表 / source files
	 * @param shardDir 分片目录 / shard directory
	 * @param filePrefix 分片文件名前缀 / shard file-name prefix
	 * @return 最新为 true / {@code true} when up to date
	 */
	static boolean isUpToDate(List<File> sources, File shardDir, String filePrefix) {
		List<File> shards = listShards(shardDir, filePrefix);
		if (shards.isEmpty()) {
			return false;
		}
		long newestSource = 0;
		for (File source : sources) {
			newestSource = Math.max(newestSource, source.lastModified());
		}
		for (File shard : shards) {
			if (shard.lastModified() < newestSource) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 列出指定前缀的全部分片文件（按名称排序）。
	 * Lists all shard files for the given prefix, sorted by name.
	 *
	 * @param shardDir 分片目录 / shard directory
	 * @param filePrefix 分片文件名前缀 / shard file-name prefix
	 * @return 分片文件列表 / shard files
	 */
	static List<File> listShards(File shardDir, String filePrefix) {
		File[] shards = shardDir.listFiles((dir, name) -> isShardFile(name, filePrefix));
		if (shards == null) {
			return List.of();
		}
		List<File> result = new ArrayList<>(List.of(shards));
		result.sort(Comparator.comparing(File::getName));
		return result;
	}

	/**
	 * 读取源文件后按模板 ID 排序，再按 XML 字节大小切分为不重叠的均衡分片文件。
 * Reads source files, sorts target templates by ID, then splits them into non-overlapping shards
 * with balanced XML byte sizes.
	 *
	 * @param sources 按加载顺序排列的源文件 / source files in load order
	 * @param rootElement 分片根元素名 / shard root element name
	 * @param targetElement 目标模板元素名 / target template element name
	 * @param idAttribute 模板 ID 属性名 / template id attribute name
	 * @param shardDir 分片输出目录 / shard output directory
	 * @param filePrefix 分片文件名前缀 / shard file-name prefix
	 * @param shardCount 目标分片数 / requested shard count
	 * @return 写出的分片文件（有序）/ written shard files in order
	 * @throws IOException 读源或写分片失败 / on source read or shard write failure
	 * @throws XMLStreamException 源 XML 解析失败 / on source XML parse failure
	 */
	static List<File> writeShards(List<File> sources, String rootElement, String targetElement, String idAttribute,
			File shardDir, String filePrefix, int shardCount) throws IOException, XMLStreamException {
		if (shardCount < 1) {
			throw new IllegalArgumentException("shardCount must be positive: " + shardCount);
		}
		makeDirectory(shardDir);
		QName idName = new QName(idAttribute);
		Set<String> writtenNames = new HashSet<>();
		List<File> written = new ArrayList<>();
		List<TemplateEntry> templates = new ArrayList<>();
		int sourceOrder = 0;
		for (File source : sources) {
			try (var input = new BufferedInputStream(new FileInputStream(source))) {
				XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(input);
				try {
					while (reader.hasNext()) {
						XMLEvent event = reader.nextEvent();
						if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(targetElement)) {
							StartElement start = event.asStartElement();
							String id = templateId(start, idName, source);
							String xml = serializeElement(reader, start);
							templates.add(new TemplateEntry(numericId(id), sourceOrder++, xml,
								 xml.getBytes(StandardCharsets.UTF_8).length));
						}
					}
				} finally {
					reader.close();
				}
			}
		}
		templates.sort(Comparator.comparingLong(TemplateEntry::id).thenComparingInt(TemplateEntry::sourceOrder));
		if (templates.isEmpty()) {
			deleteStaleShards(shardDir, filePrefix, writtenNames);
			return written;
		}
		List<TemplateGroup> groups = groupById(templates);
		int actualShardCount = Math.min(shardCount, groups.size());
		int start = 0;
		long remainingBytes = groups.stream().mapToLong(TemplateGroup::byteSize).sum();
		for (int shardIndex = 0; shardIndex < actualShardCount; shardIndex++) {
			int remainingShards = actualShardCount - shardIndex;
			int end = chooseShardEnd(groups, start, remainingShards, remainingBytes);
			ShardBuffer buffer = new ShardBuffer(rootElement);
			long shardBytes = 0;
			for (int groupIndex = start; groupIndex < end; groupIndex++) {
				TemplateGroup group = groups.get(groupIndex);
				for (TemplateEntry template : group.entries()) {
					buffer.add(template.id(), template.xml());
				}
				shardBytes += group.byteSize();
			}
			written.add(buffer.write(shardDir, filePrefix, writtenNames));
			remainingBytes -= shardBytes;
			start = end;
		}
		deleteStaleShards(shardDir, filePrefix, writtenNames);
		return written;
	}

	/**
	 * 选择最接近剩余平均字节数的边界，并将重复 ID 保持在同一分片。
	 * Chooses the boundary nearest the remaining average byte size and keeps duplicate IDs together.
	 */
	private static int chooseShardEnd(List<TemplateGroup> groups, int start, int remainingShards, long remainingBytes) {
		int last = groups.size();
		if (remainingShards == 1) {
			return last;
		}
		long target = Math.max(1L, Math.round((double) remainingBytes / remainingShards));
		int maxEnd = last - remainingShards + 1;
		long accumulated = 0;
		long bestDifference = Long.MAX_VALUE;
		int bestEnd = start + 1;
		for (int end = start + 1; end <= maxEnd; end++) {
			accumulated += groups.get(end - 1).byteSize();
			long difference = Math.abs(accumulated - target);
			if (difference < bestDifference) {
				bestDifference = difference;
				bestEnd = end;
			}
			if (accumulated >= target) {
				break;
			}
		}
		return bestEnd;
	}

	/**
	 * 将同一模板 ID 的记录归并为不可拆分的组，保证重复 ID 不会跨分片边界。
	 * Groups records with the same template ID so duplicate IDs never cross a shard boundary.
	 */
	private static List<TemplateGroup> groupById(List<TemplateEntry> templates) {
		List<TemplateGroup> groups = new ArrayList<>();
		int start = 0;
		while (start < templates.size()) {
			int end = start + 1;
			long bytes = templates.get(start).byteSize();
			while (end < templates.size() && templates.get(end).id() == templates.get(start).id()) {
				bytes += templates.get(end).byteSize();
				end++;
			}
			groups.add(new TemplateGroup(List.copyOf(templates.subList(start, end)), bytes));
			start = end;
		}
		return groups;
	}

	static boolean isShardFile(String fileName, String filePrefix) {
		return Pattern.compile(Pattern.quote(filePrefix) + "_\\d+_\\d+\\.xml").matcher(fileName).matches();
	}

	/**
	 * 提取模板 ID 属性值；缺失时快速失败，避免产出无法命名的分片。
	 * Extracts the template id attribute; fails fast when missing so no unnamed shard can be produced.
	 */
	private static String templateId(StartElement element, QName idName, File source) throws XMLStreamException {
		Attribute attribute = element.getAttributeByName(idName);
		if (attribute == null) {
			throw new XMLStreamException("Element " + element.getName() + " in " + source.getPath()
					+ " is missing id attribute " + idName.getLocalPart());
		}
		return attribute.getValue();
	}

	private static long numericId(String id) {
		try {
			return Long.parseLong(id);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Template id must be numeric for shard naming: " + id, e);
		}
	}

	/**
	 * 序列化从 startEvent 开始的完整子树（含起止事件），返回 XML 文本。
	 * Serializes the full subtree starting at startEvent (inclusive) and returns it as XML text.
	 */
	private static String serializeElement(XMLEventReader reader, StartElement startEvent) throws XMLStreamException {
		StringWriter stringWriter = new StringWriter(2048);
		XMLEventWriter eventWriter = OUTPUT_FACTORY.createXMLEventWriter(stringWriter);
		try {
			int depth = 0;
			XMLEvent event = startEvent;
			while (true) {
				eventWriter.add(event);
				if (event.isStartElement()) {
					depth++;
				} else if (event.isEndElement() && --depth == 0) {
					return stringWriter.toString();
				}
				event = reader.nextEvent();
			}
		} finally {
			eventWriter.close();
		}
	}

	/** 创建目录（若缺失）/ creates the directory when missing */
	private static void makeDirectory(File dir) throws IOException {
		if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
			throw new IOException("Failed to create shard directory " + dir.getPath());
		}
	}

	/**
	 * 删除不属于本次写入集合的旧分片与遗留临时文件。
	 * Deletes old shards and leftover temp files that are not part of the new written set.
	 */
	private static void deleteStaleShards(File shardDir, String filePrefix, Set<String> writtenNames) {
		File[] files = shardDir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			String name = file.getName();
			if (name.endsWith(".tmp") || isShardFile(name, filePrefix) && !writtenNames.contains(name)) {
				FileUtils.deleteQuietly(file);
			}
		}
	}

	/** 单个模板及其序列化字节大小 / one template and its serialized byte size */
	private record TemplateEntry(long id, int sourceOrder, String xml, int byteSize) {
	}

	/** 按模板 ID 归组的不可拆分记录 / indivisible records grouped by template ID */
	private record TemplateGroup(List<TemplateEntry> entries, long byteSize) {
	}

	/**
	 * 单个分片的写出缓冲，按模板追加后原子写盘。
	 * Write buffer atomically persisted after appending templates.
	 */
	private static final class ShardBuffer {

		private final String rootElement;
		private final StringBuilder xml = new StringBuilder(2 << 21);
		private long minId = Long.MAX_VALUE;
		private long maxId = Long.MIN_VALUE;

		private ShardBuffer(String rootElement) {
			this.rootElement = rootElement;
		}

		private void add(long numericId, String elementXml) {
			minId = Math.min(minId, numericId);
			maxId = Math.max(maxId, numericId);
			xml.append(elementXml).append('\n');
		}

		/**
		 * 写出分片：先写临时文件再原子改名，文件名含数值最小/最大模板 ID。
		 * Writes the shard to a temp file then atomically renames it; the name carries the numeric minimum and maximum ids.
		 */
		private File write(File shardDir, String filePrefix, Set<String> writtenNames) throws IOException {
			String name = SHARD_FILE_FORMAT.formatted(filePrefix, minId, maxId);
			if (!writtenNames.add(name)) {
				throw new IllegalStateException("Duplicate shard file name " + name + " in " + shardDir.getPath()
						+ "; check the source for duplicated template ids at the shard boundary");
			}
			File target = new File(shardDir, name);
			File temp = new File(shardDir, name + ".tmp");
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),
				StandardCharsets.UTF_8))) {
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writer.write('<');
				writer.write(rootElement);
				writer.write(">\n");
				writer.write(xml.toString());
				writer.write("</");
				writer.write(rootElement);
				writer.write(">\n");
			}
			Files.move(temp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			return target;
		}
	}
}
