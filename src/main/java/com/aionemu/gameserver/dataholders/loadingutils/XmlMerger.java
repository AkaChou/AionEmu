package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import static org.apache.commons.io.filefilter.FileFilterUtils.andFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.makeSVNAware;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.prefixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 将主 XML 中的 {@code import} 元素解析并合并为单一文档，供 JAXB 反序列化使用。
 * Resolves {@code import} elements in a master XML and merges them into one document for JAXB unmarshalling.
 * <p>
 * {@code import} 属性：
 * Import attributes:
 * <ul>
 *   <li>{@code file} — 必填，导入文件或目录路径 / required path to file or directory</li>
 *   <li>{@code skipRoot} — 可选，默认 false；为 true 时忽略被导入文件的根标签 / optional, default false; skip imported root tags when true</li>
 *   <li>{@code recursiveImport} — 可选，默认 true；目录导入时是否递归子目录 / optional, default true; recurse into subdirs when importing a directory</li>
 * </ul>
 *
 * @author Aquanox
 */
@Slf4j
public class XmlMerger {

	private final File baseDir;
	private final File sourceFile;
	private final File destFile;
	private final File metaDataFile;
	private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

	/**
	 * 创建 XmlMerger；基准目录默认为源文件所在目录。
	 * Creates an XmlMerger with base directory set to the source file's parent.
	 *
	 * source file
	 * destination file
	 */
	public XmlMerger(File source, File target) {
		this(source, target, source.getParentFile());
	}

	/**
	 * 创建 XmlMerger，并指定 import 解析的基准目录。
	 * Creates an XmlMerger with an explicit base directory for resolving imports.
	 *
	 * source file
	 * destination file
	 * root directory for relative import paths
	 */
	public XmlMerger(File source, File target, File baseDir) {
		this.baseDir = baseDir;

		this.sourceFile = source;
		this.destFile = target;

		this.metaDataFile = new File(target.getParent(), target.getName() + ".properties");
	}

	/**
	 * 若目标缺失或源/导入文件有变更则重建合并结果；否则保持不变。
	 * Rebuilds the merged document when missing or when sources/imports changed; otherwise no-op.
	 *
	 * @return 是否实际执行了更新 / whether an update was performed
	 * when the source file is missing。
	 * on XML processing errors
	 *
	 * @return
	 * @throws Exception 其它 I / O 或解析错误 / other I/O or parse errors
	 */
	public boolean process() throws Exception {
		log.debug("Processing " + sourceFile + " files into " + destFile);

		if (!sourceFile.exists()) {
			throw new FileNotFoundException("Source file " + sourceFile.getPath() + " not found.");
		}

		boolean needUpdate = false;

		if (!destFile.exists()) {
			log.debug("Dest file not found - creating new file");
			needUpdate = true;
		} else if (!metaDataFile.exists()) {
			log.debug("Meta file not found - creating new file");
			needUpdate = true;
		} else {
			log.debug("Dest file found - checking file modifications");
			needUpdate = checkFileModifications();
		}

		if (needUpdate) {
			log.debug("Modifications found. Updating...");
			try {
				doUpdate();
				return true;
			} catch (Exception e) {
				FileUtils.deleteQuietly(destFile);
				FileUtils.deleteQuietly(metaDataFile);
				throw e;
			}
		} else {
			log.debug("Files are up-to-date");
			return false;
		}
	}

	/**
	 * 检查源文件及全部 import 依赖是否相对缓存发生变更。
	 * Checks whether the source or any imported file changed relative to the cache.
	 *
	 * @return 若至少有一处变更则为 true / true if any included file was modified
	 * on I/O or parse errors。
	 */
	private boolean checkFileModifications() throws Exception {
		long destFileTime = destFile.lastModified();

		if (sourceFile.lastModified() > destFileTime) {
			log.debug("Source file was modified ");
			return true;
		}

		Properties metadata = restoreFileModifications(metaDataFile);

		if (metadata == null) { // new file or smth else.
			return true;
		}

		SAXParserFactory parserFactory = SAXParserFactory.newInstance();

		SAXParser parser = parserFactory.newSAXParser();

		TimeCheckerHandler handler = new TimeCheckerHandler(baseDir, metadata);

		parser.parse(sourceFile, handler);

		return handler.isModified();
	}

	/**
	 * 处理源文件，将全部 {@code import} 标签替换为对应文件内容并写出目标。
	 * Processes the source file, replacing every {@code import} tag with imported content.
	 *
	 * on event read/write errors。
	 * if the destination cannot be created or written。
	 */
	private void doUpdate() throws XMLStreamException, IOException {
		XMLEventReader reader = null;
		XMLEventWriter writer = null;

		Properties metadata = new Properties();

		try {
			writer = outputFactory.createXMLEventWriter(new BufferedWriter(new FileWriter(destFile, false)));
			reader = inputFactory.createXMLEventReader(new FileReader(sourceFile));

			while (reader.hasNext()) {
				final XMLEvent xmlEvent = reader.nextEvent();

				if (xmlEvent.isStartElement() && isImportQName(xmlEvent.asStartElement().getName())) {
					processImportElement(xmlEvent.asStartElement(), writer, metadata);
					continue;
				}

				if (xmlEvent.isEndElement() && isImportQName(xmlEvent.asEndElement().getName())) {
					continue;
				}

				if (xmlEvent instanceof Comment) { // skip comments.
					continue;
				}

				if (xmlEvent.isCharacters()) { // skip whitespaces.
					if (xmlEvent.asCharacters().isWhiteSpace() || xmlEvent.asCharacters().isIgnorableWhiteSpace()) { // 跳过 / skip
						// 空白字符。 / whitespaces.
						continue;
					}
				}
				writer.add(xmlEvent);

				if (xmlEvent.isStartDocument()) {
					writer.add(eventFactory.createComment("\nThis file is machine-generated. DO NOT MODIFY IT!\n"));
				}
			}
			storeFileModifications(metadata, metaDataFile);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ignored) {
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private boolean isImportQName(QName name) {
		return "import".equals(name.getLocalPart());
	}

	private static final QName qNameFile = new QName("file");
	private static final QName qNameSkipRoot = new QName("skipRoot");
	/**
	 * 目录导入时是否递归子目录，默认 true。
	 * When importing a directory, recurse into subdirectories; default true.
	 */
	private static final QName qNameRecursiveImport = new QName("recursiveImport");

	/**
	 * 处理单个 {@code import} 元素，将其替换为对应文件/目录内容。
	 * Processes one {@code import} element and replaces it with the imported file or directory content.
	 *
	 * import start element
	 * @param writer 目标写入器 / destination writer
	 * @param metadata 文件哈希元数据 / file-hash metadata
	 * on event writing errors。
	 * if an imported file is missing or unreadable。
	 */
	private void processImportElement(StartElement element, XMLEventWriter writer, Properties metadata)
			throws XMLStreamException, IOException {
		File file = new File(baseDir,
				getAttributeValue(element, qNameFile, null, "Attribute 'file' is missing or empty."));

		if (!file.exists()) {
			throw new FileNotFoundException("Missing file to import:" + file.getPath());
		}

		boolean skipRoot = Boolean.valueOf(getAttributeValue(element, qNameSkipRoot, "false", null));
		boolean recImport = Boolean.valueOf(getAttributeValue(element, qNameRecursiveImport, "true", null));

		if (file.isFile()) {
			importFile(file, skipRoot, writer, metadata);
		} else {
			log.debug("Processing dir " + file);

			Collection<File> files = listFiles(file, recImport);

			for (File childFile : files) {
				importFile(childFile, skipRoot, writer, metadata);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static Collection<File> listFiles(File root, boolean recursive) {
		IOFileFilter dirFilter = recursive ? makeSVNAware(HiddenFileFilter.VISIBLE) : null;

		return FileUtils.listFiles(root,
				andFileFilter(andFileFilter(notFileFilter(prefixFileFilter("new")), suffixFileFilter(".xml")),
						HiddenFileFilter.VISIBLE),
				dirFilter);
	}

	/**
	 * 从 {@link StartElement} 提取属性值。
	 * Extracts an attribute value from a {@link StartElement} event.
	 *
	 * start element
	 * attribute QName
	 * @param def 默认值，可为 null / default value, or null if required
	 * @param onErrorMessage 缺失且无默认值时的错误信息 / error message when missing and no default
	 * attribute value
	 * if the attribute is missing and no default is set。
	 */
	private String getAttributeValue(StartElement element, QName name, String def, String onErrorMessage)
			throws XMLStreamException {
		Attribute attribute = element.getAttributeByName(name);

		if (attribute == null) {
			if (def == null) {
				throw new XMLStreamException(onErrorMessage, element.getLocation());
			}
			return def;
		}
		return attribute.getValue();
	}

	/**
	 * 读取指定文件的全部 XML 事件并写入目标 writer。
	 * Reads all XML events from the given file and writes them to the destination writer.
	 *
	 * @param file 要导入的文件 / file to import
	 * @param skipRoot 是否跳过根标签 / whether to skip the root element
	 * @param writer 目标写入器 / destination writer
	 * @param metadata 文件哈希元数据 / file-hash metadata
	 * on event read/write errors。
	 * if the file cannot be opened for reading。
	 */
	private void importFile(File file, boolean skipRoot, XMLEventWriter writer, Properties metadata)
			throws XMLStreamException, IOException {
		log.debug("Appending file " + file);
		metadata.setProperty(file.getPath(), makeHash(file));

		XMLEventReader reader = null;

		try {
			reader = inputFactory.createXMLEventReader(new FileReader(file));
			QName firstTagQName = null;
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				// 跳过文档开头与结尾。 / skip start and end of document.
				if (event.isStartDocument() || event.isEndDocument()) {
					continue;
				}
				// 跳过全部注释。 / skip all comments.
				if (event instanceof Comment) {
					continue;
				}
				// 跳过空白与所有可忽略空白。 / skip white-spaces and all ignoreable white-spaces.
				if (event.isCharacters()) {
					if (event.asCharacters().isWhiteSpace() || event.asCharacters().isIgnorableWhiteSpace()) {
						continue;
					}
				}
				// 修改导入文件的根标签。 / modify root-tag of imported file.
				if (firstTagQName == null && event.isStartElement()) {
					firstTagQName = event.asStartElement().getName();

					if (skipRoot) {
						continue;
					} else {
						StartElement old = event.asStartElement();

						event = eventFactory.createStartElement(old.getName(), old.getAttributes(), null);
					}
				}

				// 若跳过了 root——也跳过 root 结束。 / if root was skipped - skip root end too.
				if (event.isEndElement() && skipRoot && event.asEndElement().getName().equals(firstTagQName)) {
					continue;
				}

				// 最后——写入标签 / finally - write tag
				writer.add(event);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * 检测 import 依赖是否变更的 SAX 处理器。
	 * SAX handler that detects whether import dependencies have changed.
	 */
	private static class TimeCheckerHandler extends DefaultHandler {

		private File basedir;
		private Properties metadata;
		private boolean isModified = false;
		private Locator locator;

		private TimeCheckerHandler(File basedir, Properties metadata) {
			this.basedir = basedir;
			this.metadata = metadata;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (isModified || !"import".equals(qName)) {
				return;
			}

			String value = attributes.getValue(qNameFile.getLocalPart());

			if (value == null) {
				throw new SAXParseException("Attribute 'file' is missing", locator);
			}

			File file = new File(basedir, value);

			if (!file.exists()) { // noinspection ThrowableInstanceNeverThrown
				throw new SAXParseException("Imported file not found. file=" + file.getPath(), locator);
			}

			if (file.isFile() && checkFile(file)) { // if file - just check it.
				isModified = true;
				return;
			}

			if (file.isDirectory()) { // otherwise check all files inside
				String rec = attributes.getValue(qNameRecursiveImport.getLocalPart());
				Collection<File> files = listFiles(file, rec == null ? true : Boolean.valueOf(rec));

				for (File childFile : files) {
					if (checkFile(childFile)) {
						isModified = true;
						return;
					}
				}
			}
		}

		private boolean checkFile(File file) {
			String data = metadata.getProperty(file.getPath());

			if (data == null) { // file was added.
				return true;
			}

			try {
				String hash = makeHash(file);

				if (!data.equals(hash)) { // file|dir was changed.
					return true;
				}
			} catch (IOException e) {
				log.warn(I18n.get("log.20b451dc6e2b", file.getPath(), locator.getLineNumber(), locator.getColumnNumber(), e), e);
				return true;// was modified.
			}
			return false;
		}

		/**
		 * 是否检测到修改。
		 * Whether a modification was detected.
		 *
		 * @return true 表示已修改 / true if modified
		 */
		public boolean isModified() {
			return isModified;
		}
	}

	private Properties restoreFileModifications(File file) {
		if (!file.exists() || !file.isFile()) {
			return null;
		}
		FileReader reader = null;

		try {
			Properties props = new Properties();
			reader = new FileReader(file);
			props.load(reader);
			return props;
		} catch (IOException e) { // properties
			log.debug("File modfications restoring error. ", e);
			return null;
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	private void storeFileModifications(Properties props, File file) throws IOException {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, false);
			props.store(writer, " This file is machine-generated. DO NOT EDIT!");
		} catch (IOException e) {
			log.error(I18n.get("log.f1db332ef22f"));
			throw e;
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/**
	 * 基于文件内容生成唯一标识（CRC32）。
	 * Builds a unique content identifier for the file (CRC32).
	 *
	 * @param file 待校验文件，不可为 null / file to checksum, must not be null
	 * @return 字符串标识 / string identifier
	 * if an I/O error occurs reading the file。
	 */
	private static String makeHash(File file) throws IOException {
		return String.valueOf(FileUtils.checksumCRC32(file));
	}
}
