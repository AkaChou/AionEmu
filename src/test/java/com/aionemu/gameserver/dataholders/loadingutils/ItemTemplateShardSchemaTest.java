package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 物品模板源数据的 schema 门禁：全部分片与自定义覆盖文件都必须符合 {@code item_templates.xsd}。
 * <p>为什么在测试期而不是加载期：生产的 unmarshaller 明确不装 schema——这一点由
 * {@code XmlDataLoaderTest#staticDataUnmarshallerDoesNotInstallSynchronousSchemaValidation}
 * 用 {@code assertNull(unmarshaller.getSchema())} 锁死。校验因此是一次性的离线成本，
 * 不进入每次启动。
 * <p>补的是两层之间的缺口：{@code static_data.xsd} 虽然 include 了 {@code item_templates.xsd}，
 * 但既有测试只证明 schema 能编译、只校验主入口文件（那份文件只有分区声明列表，不含任何
 * 物品模板数据），因此 11 个分片此前既不在运行时校验、也不在测试期校验。
 * <p>Schema gate for item template source data: every shard and the custom override file must
 * satisfy {@code item_templates.xsd}.
 * <p>Why test-time rather than load-time: production unmarshallers deliberately install no schema —
 * locked by {@code assertNull(unmarshaller.getSchema())} in {@code XmlDataLoaderTest}. Validation
 * is therefore an offline one-off cost that stays out of every startup.
 * <p>It fills the gap between the two layers: although {@code static_data.xsd} includes
 * {@code item_templates.xsd}, the existing tests only proved the schema compiles and only
 * validated the entry-point document (a section list carrying no item template data), so the 11
 * shards were validated neither at runtime nor in tests.
 */
class ItemTemplateShardSchemaTest {

	/** 与 {@code XmlDataLoader.ITEM_SHARD_DIR} 同源（生产走 `aion.game.data.dir`，测试固定仓库路径）。
	 *  Same source as {@code XmlDataLoader.ITEM_SHARD_DIR}; production resolves it through
	 *  `aion.game.data.dir` while the test pins the repository path. */
	private static final Path SHARD_DIR = Path.of("src/main/resources/aion/data/static_data/items/item");

	/**
	 * 与 {@code XmlDataLoader.ITEM_SHARD_PATTERN} 保持一致：生产换扩展名或改命名规则时，
	 * 这里必须同步，否则门禁会静默地一个分片也匹配不到（下方 {@code assertFalse} 会拦下这种情况）。
	 * <p>Kept in step with {@code XmlDataLoader.ITEM_SHARD_PATTERN}: if production changes the
	 * extension or naming scheme this must follow, otherwise the gate would silently match zero
	 * shards — which the {@code assertFalse} below catches.
	 */
	private static final Pattern SHARD_PATTERN = Pattern.compile("item_template_(\\d+)_(\\d+)\\.xml");

	private static final Path SCHEMA_FILE = SHARD_DIR.resolve("item_templates.xsd");

	/** 自定义覆盖文件由 {@code loadItemData} 以同一 unmarshaller 读入，同样受 schema 约束。
	 *  Read by {@code loadItemData} through the same unmarshaller, so it is bound by the schema too. */
	private static final Path CUSTOM_OVERRIDE = Path.of(
		"src/main/resources/aion/data/static_data/items/item_template_custom.xml");

	/** Schema 编译一次并共享：它本身线程安全，而每个 Validator 各建各的。
	 *  Compiled once and shared — the schema is thread safe, each validator is not. */
	private static final Schema SCHEMA = compileSchema();

	private static Schema compileSchema() {
		try {
			return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(SCHEMA_FILE.toFile());
		} catch (Exception e) {
			throw new IllegalStateException("cannot compile " + SCHEMA_FILE, e);
		}
	}

	@Test
	void everyShardValidatesAgainstSchema() throws IOException {
		List<Path> shards = listShards();
		assertFalse(shards.isEmpty(), "no item template shard matching " + SHARD_PATTERN.pattern()
			+ " under " + SHARD_DIR);
		for (Path shard : shards) {
			assertDoesNotThrow(() -> SCHEMA.newValidator().validate(new StreamSource(shard.toFile())),
				() -> "shard violates item_templates.xsd: " + shard.getFileName());
		}
	}

	/**
	 * 覆盖文件**允许为空**：{@code <item_templates/>} 不带子元素表示"当前没有自定义物品"，
	 * 是它刻意支持的正常状态（文件头注释写明它不会被自动改写或删除）。
	 * <p>{@code item_templates.xsd} 对 {@code item_template} 要求 {@code minOccurs="1"}，
	 * 那条约束是为**分片**定的——空分片意味着切分出了问题——对覆盖文件不成立。
	 * 因此这里只在文件确实携带条目时才校验；空文件跳过而非静默通过。
	 * <p>已知的不一致：该文件声明了 {@code xsi:noNamespaceSchemaLocation}，却（在空状态下）
	 * 不符合自己声明的 schema。该声明从不生效（JAXB 不读它），因此这个矛盾此前无人发现。
	 * <p>The override file is **allowed to be empty**: an {@code <item_templates/>} with no
	 * children means "no custom items", a state it deliberately supports. The schema's
	 * {@code minOccurs="1"} on {@code item_template} is written for **shards** — an empty shard
	 * means the split went wrong — and does not hold here. Validation therefore runs only when the
	 * file actually carries entries; an empty file is skipped rather than silently passing.
	 * <p>Known inconsistency: the file declares {@code xsi:noNamespaceSchemaLocation} yet (while
	 * empty) does not satisfy the schema it names. The declaration never takes effect — JAXB does
	 * not read it — which is why the contradiction went unnoticed.
	 */
	@Test
	void customOverrideValidatesAgainstSchemaWhenPopulated() throws Exception {
		assumeTrue(hasItemTemplate(CUSTOM_OVERRIDE),
			"custom override carries no entries; nothing to validate");
		assertDoesNotThrow(() -> SCHEMA.newValidator().validate(new StreamSource(CUSTOM_OVERRIDE.toFile())),
			() -> "custom override violates item_templates.xsd: " + CUSTOM_OVERRIDE.getFileName());
	}

	/** 用 SAX 扫一遍是否存在 {@code item_template} 元素；注释里的示例不会被报告，不必额外排除。
	 *  SAX-scans for an {@code item_template} element; commented-out examples are never reported,
	 *  so no extra filtering is needed. */
	private static boolean hasItemTemplate(Path file) throws Exception {
		boolean[] found = {false};
		SAXParserFactory.newInstance().newSAXParser().parse(file.toFile(), new DefaultHandler() {
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) {
				if ("item_template".equals(localName == null || localName.isEmpty() ? qName : localName)) {
					found[0] = true;
				}
			}
		});
		return found[0];
	}

	private static List<Path> listShards() throws IOException {
		try (Stream<Path> entries = Files.list(SHARD_DIR)) {
			return entries
				.filter(path -> SHARD_PATTERN.matcher(path.getFileName().toString()).matches())
				.sorted()
				.toList();
		}
	}
}
