import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;

/**
 * 使用生产任务图编译器逐成员验证候选 XML，并输出稳定的机器可读结果。
 * Validates candidate XML member by member with the production quest graph compiler and emits stable machine-readable results.
 */
public final class QuestGraphCompilerGate {

	/** 禁止实例化静态门禁工具。 / Prevents instantiation of the static gate utility. */
	private QuestGraphCompilerGate() {
	}

	/**
	 * 编译输入目录中的每个 XML；成员失败会被编码到输出中，不会阻断其他成员。
	 * Compiles every XML in the input directory; member failures are encoded in output without blocking other members.
	 *
	 * @param args schema path and member directory / schema 路径与成员目录
	 * @throws Exception if gate infrastructure cannot read its inputs / 门禁基础设施无法读取输入
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: QuestGraphCompilerGate <schema> <member-directory>");
			System.exit(2);
		}
		Path schema = Path.of(args[0]).toAbsolutePath().normalize();
		Path directory = Path.of(args[1]).toAbsolutePath().normalize();
		if (!Files.isRegularFile(schema) || !Files.isDirectory(directory)) {
			throw new IllegalArgumentException("Compiler gate inputs do not exist");
		}

		List<Path> files;
		try (var stream = Files.list(directory)) {
			files = stream.filter(path -> path.getFileName().toString().endsWith(".xml"))
				.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
		}
		for (Path file : files) {
			compileMember(file, schema);
		}
	}

	/**
	 * 编译单个候选并输出 OK 或包含完整异常链的 FAIL 记录。
	 * Compiles one candidate and emits either OK or a FAIL record containing the full exception chain.
	 */
	private static void compileMember(Path file, Path schema) {
		try {
			CompiledQuestGraphData data = QuestGraphCompiler.load(file, schema, referencesDeclaredIn(file));
			if (data.graphs().size() != 1) {
				throw new IllegalArgumentException("Expected exactly one quest graph, got " + data.graphs().size());
			}
			long transitions = data.graphs().values().stream().flatMap(graph -> graph.nodes().values().stream())
				.flatMap(node -> node.transitions().stream()).count();
			System.out.printf("OK\t%s\t%d\t%d%n", file.getFileName(), data.graphs().size(), transitions);
		} catch (Exception error) {
			String encoded = Base64.getEncoder().encodeToString(causeMessages(error).getBytes(StandardCharsets.UTF_8));
			System.out.printf("FAIL\t%s\t%s%n", file.getFileName(), encoded);
		}
	}

	/**
	 * 从候选 XML 收集编译器引用；静态数据存在性由 Python 引用闭包门禁独立证明。
	 * Collects compiler references from candidate XML; the Python reference-closure gate independently proves static-data existence.
	 */
	private static QuestGraphCompiler.References referencesDeclaredIn(Path file) throws Exception {
		Set<Integer> questIds = new HashSet<>();
		Set<Integer> npcIds = new HashSet<>();
		Set<Integer> itemIds = new HashSet<>();
		Set<Integer> titleIds = new HashSet<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (var stream = Files.newInputStream(file)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT) {
						continue;
					}
					addIntegerAttribute(reader, "quest_id", questIds);
					addIntegerAttribute(reader, "npc_id", npcIds);
					addIntegerAttribute(reader, "item_id", itemIds);
					addIntegerAttribute(reader, "title_id", titleIds);
				}
			} finally {
				reader.close();
			}
		}
		return new QuestGraphCompiler.References(questIds, npcIds, itemIds, titleIds);
	}

	/** 添加存在的整数 XML 属性。 / Adds a present integer XML attribute. */
	private static void addIntegerAttribute(XMLStreamReader reader, String name, Set<Integer> target) {
		String value = reader.getAttributeValue(null, name);
		if (value != null) {
			target.add(Integer.parseInt(value));
		}
	}

	/** 返回去重且保持因果顺序的异常消息。 / Returns deduplicated exception messages in causal order. */
	private static String causeMessages(Throwable error) {
		List<String> messages = new ArrayList<>();
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && !message.isBlank() && !messages.contains(message)) {
				messages.add(message.replace('\t', ' ').replace('\n', ' '));
			}
		}
		return String.join(" | ", messages);
	}
}
