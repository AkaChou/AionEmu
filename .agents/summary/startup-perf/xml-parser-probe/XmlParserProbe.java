import com.sun.management.ThreadMXBean;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 静态数据 XML 解析器探针：在同一份真实数据上比较 JDK 内置解析器与 Woodstox。
 * Static-data XML parser probe: compares the JDK built-in parsers with Woodstox on the real files.
 *
 * <p>该探针只读取数据文件，不启动服务端、不写仓库内容。多数场景通过 JVM 系统属性切换解析器
 * （{@code -Djavax.xml.parsers.SAXParserFactory} / {@code -Djavax.xml.stream.XMLInputFactory}），
 * 因为生产代码走的就是 JAXP 的 {@code newInstance()/newFactory()} 查找路径；这一点与"只加依赖
 * 还是必须改代码"的结论直接相关。{@code jaxb-item-explicit} 场景用显式构造的 XMLReader 做同进程
 * 交替 A/B，排除系统属性机制本身的干扰。
 *
 * <p>This probe is read-only: it never starts the server and never writes to the repository. Most
 * scenarios switch the parser through JVM system properties because production code resolves parsers
 * through the JAXP {@code newInstance()/newFactory()} lookup path; that is exactly the evidence needed
 * to decide between "dependency only" and "code change required". The {@code jaxb-item-explicit}
 * scenario interleaves explicitly constructed XMLReaders in one JVM as a control.
 *
 * <p>用法 / Usage:
 * <pre>
 *   java -cp &lt;classpath&gt; XmlParserProbe.java jaxb-item &lt;item-shard.xml&gt; [warmup] [iterations]
 *   java -cp &lt;classpath&gt; XmlParserProbe.java jaxb-npc &lt;npc-shard.xml&gt; [warmup] [iterations]
 *   java -cp &lt;classpath&gt; XmlParserProbe.java stax-mappings &lt;npc-ai.xml&gt; [warmup] [iterations]
 *   java -cp &lt;classpath&gt; XmlParserProbe.java sax-skill-part &lt;skill_templates_part_XXX.xml&gt; [warmup] [iterations]
 *   java -cp &lt;classpath&gt; XmlParserProbe.java jaxb-item-explicit &lt;item-shard.xml&gt; [warmup] [iterations]
 * </pre>
 */
public final class XmlParserProbe {

	private static final int DEFAULT_WARMUP = 2;
	private static final int DEFAULT_ITERATIONS = 5;
	private static final String DATAHOLDERS = "com.aionemu.gameserver.dataholders.";
	private static final String LOADINGUTILS = "com.aionemu.gameserver.dataholders.loadingutils.";
	private static final String WOODSTOX_SAX_FACTORY = "com.ctc.wstx.sax.WstxSAXParserFactory";

	/**
	 * 单次解析操作。One parse operation.
	 */
	@FunctionalInterface
	private interface Op {
		Object run() throws Exception;
	}

	/**
	 * 延迟创建 XMLReader（生产代码每个分片新建一个）。Lazily creates an XMLReader (production creates one per part).
	 */
	@FunctionalInterface
	private interface ReaderSupplier {
		XMLReader get() throws Exception;
	}

	private record Stats(long wallNanos, long cpuNanos, long allocatedBytes) {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			usage();
			System.exit(2);
		}
		String scenario = args[0];
		File file = new File(args[1]);
		int warmup = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_WARMUP;
		int iterations = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_ITERATIONS;
		if (!file.isFile()) {
			System.err.println("ERROR file_not_found=" + file.getAbsolutePath());
			System.exit(2);
		}
		System.out.printf(Locale.ROOT,
			"INFO java=%s scenario=%s file=%s bytes=%d warmup=%d iterations=%d%n",
			System.getProperty("java.version"), scenario, file.getPath(), file.length(), warmup, iterations);
		switch (scenario) {
			case "jaxb-item" -> runJaxb("ItemData", file, warmup, iterations);
			case "jaxb-npc" -> runJaxb("NpcData", file, warmup, iterations);
			case "jaxb-item-reader" -> runJaxbReader("ItemData", file, warmup, iterations, false);
			case "jaxb-item-buffered-reader" -> runJaxbReader("ItemData", file, warmup, iterations, true);
			case "jaxb-npc-reader" -> runJaxbReader("NpcData", file, warmup, iterations, false);
			case "stax-mappings" -> runStaxMappings(file, warmup, iterations);
			case "sax-skill-part" -> runSaxSkillPart(file, warmup, iterations);
			case "jaxb-item-explicit" -> runExplicitAb(file, warmup, iterations);
			default -> {
				usage();
				System.exit(2);
			}
		}
	}

	private static void usage() {
		System.err.println("usage: XmlParserProbe <jaxb-item|jaxb-npc|jaxb-item-reader|jaxb-item-buffered-reader|jaxb-npc-reader|stax-mappings|sax-skill-part|jaxb-item-explicit> <file> [warmup] [iterations]");
	}

	/**
	 * 镜像 {@code XmlDataLoader.unmarshalShard}：共享 JAXBContext + 每次新建 Unmarshaller + 缓冲字节流直读。
	 * Mirrors {@code XmlDataLoader.unmarshalShard}: shared JAXBContext, fresh Unmarshaller per shard, raw buffered stream.
	 */
	private static void runJaxb(String modelSimpleName, File file, int warmup, int iterations) throws Exception {
		Class<?> model = Class.forName(DATAHOLDERS + modelSimpleName);
		JAXBContext context = JAXBContext.newInstance(model);
		printParserImplementations();
		measure("jaxb:" + modelSimpleName, warmup, iterations, () -> {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
				return unmarshaller.unmarshal(input);
			}
		});
	}

	/**
	 * 对照实验：把字节流换成已解码的 Reader（InputStreamReader / BufferedReader），
	 * 观察 Xerces {@code UTF8Reader} 字符解码是否是其热点（JFR 显示占静态数据样本 50%）。
	 * Control experiment: feed an already-decoded Reader (InputStreamReader / BufferedReader) instead of
	 * the raw byte stream, to test whether the Xerces {@code UTF8Reader} decode path is the hot spot
	 * (JFR attributes ~50% of static-data samples to it).
	 */
	private static void runJaxbReader(String modelSimpleName, File file, int warmup, int iterations,
			boolean buffered) throws Exception {
		Class<?> model = Class.forName(DATAHOLDERS + modelSimpleName);
		JAXBContext context = JAXBContext.newInstance(model);
		printParserImplementations();
		String label = "jaxb-reader:" + modelSimpleName + (buffered ? ":buffered" : ":plain");
		measure(label, warmup, iterations, () -> {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			try (InputStream input = new BufferedInputStream(new FileInputStream(file), 1 << 16);
					Reader reader = buffered
						? new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8), 1 << 16)
						: new InputStreamReader(input, StandardCharsets.UTF_8)) {
				return unmarshaller.unmarshal(reader);
			}
		});
	}

	/**
	 * 反射调用生产方法 {@code RetailAiDefinitionLoader.loadMappings(File)}（npc-ai.xml 单遍 StAX 扫描）。
	 * Reflectively calls the production {@code RetailAiDefinitionLoader.loadMappings(File)} (single StAX pass over npc-ai.xml).
	 */
	private static void runStaxMappings(File file, int warmup, int iterations) throws Exception {
		Method loadMappings = Class.forName(LOADINGUTILS + "RetailAiDefinitionLoader")
			.getDeclaredMethod("loadMappings", File.class);
		loadMappings.setAccessible(true);
		printParserImplementations();
		measure("stax:loadMappings", warmup, iterations, () -> loadMappings.invoke(null, file));
	}

	/**
	 * 反射调用生产方法 {@code SkillDefinitionLoader.createPartReader()}：先给出 feature 兼容性判定，
	 * 再在可用 reader 上做 SAX 扫描吞吐测量。
	 * Reflectively calls production {@code SkillDefinitionLoader.createPartReader()}: reports the feature
	 * compatibility verdict first, then measures SAX scan throughput on whatever reader is usable.
	 */
	private static void runSaxSkillPart(File file, int warmup, int iterations) throws Exception {
		Method createPartReader = Class.forName(LOADINGUTILS + "SkillDefinitionLoader")
			.getDeclaredMethod("createPartReader");
		createPartReader.setAccessible(true);
		ReaderSupplier readers;
		try {
			XMLReader reader = (XMLReader) createPartReader.invoke(null);
			System.out.println("INFO feature_compat=ok reader=" + reader.getClass().getName());
			readers = () -> (XMLReader) createPartReader.invoke(null);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			System.out.println("WARN feature_compat=fail type=" + cause.getClass().getName()
				+ " message=" + cause.getMessage());
			System.out.println("INFO fallback=namespace-aware-only");
			readers = XmlParserProbe::fallbackReader;
		}
		ReaderSupplier supplier = readers;
		measure("sax:skill-part-scan:" + file.getName(), warmup, iterations, () -> {
			XMLReader reader = supplier.get();
			ElementCounter counter = new ElementCounter();
			reader.setContentHandler(counter);
			try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
				reader.parse(new InputSource(input));
			}
			return counter.elements;
		});
	}

	/**
	 * 同进程交替 A/B：JAXB + 显式 XMLReader（JDK 默认 vs Woodstox），排除属性查找带来的差异。
	 * Same-JVM interleaved A/B: JAXB with explicitly built XMLReaders (JDK default vs Woodstox).
	 */
	private static void runExplicitAb(File file, int warmup, int iterations) throws Exception {
		Class<?> model = Class.forName(DATAHOLDERS + "ItemData");
		JAXBContext context = JAXBContext.newInstance(model);
		System.out.println("INFO variant=jdk reader=" + defaultSaxFactory().newSAXParser().getXMLReader().getClass().getName());
		System.out.println("INFO variant=woodstox reader=" + woodstoxSaxFactory().newSAXParser().getXMLReader().getClass().getName());
		Op jdkOp = saxSourceOp(context, file, XmlParserProbe::defaultSaxFactory);
		Op woodstoxOp = saxSourceOp(context, file, XmlParserProbe::woodstoxSaxFactory);
		List<Stats> jdkStats = new ArrayList<>();
		List<Stats> woodstoxStats = new ArrayList<>();
		for (int round = 0; round < warmup + iterations; round++) {
			boolean record = round >= warmup;
			if ((round & 1) == 0) {
				collect(jdkStats, jdkOp, record);
				collect(woodstoxStats, woodstoxOp, record);
			}
			else {
				collect(woodstoxStats, woodstoxOp, record);
				collect(jdkStats, jdkOp, record);
			}
		}
		report("jaxb-item-explicit:jdk", jdkStats);
		report("jaxb-item-explicit:woodstox", woodstoxStats);
	}

	private static Op saxSourceOp(JAXBContext context, File file, java.util.function.Supplier<SAXParserFactory> factories) {
		return () -> {
			SAXParserFactory factory = factories.get();
			factory.setNamespaceAware(true);
			XMLReader reader = factory.newSAXParser().getXMLReader();
			InputSource source = new InputSource(new BufferedInputStream(new FileInputStream(file)));
			source.setSystemId(file.toURI().toString());
			return context.createUnmarshaller().unmarshal(new SAXSource(reader, source));
		};
	}

	private static SAXParserFactory defaultSaxFactory() {
		return SAXParserFactory.newInstance();
	}

	private static SAXParserFactory woodstoxSaxFactory() {
		try {
			return (SAXParserFactory) Class.forName(WOODSTOX_SAX_FACTORY).getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Woodstox is not on the classpath: " + e, e);
		}
	}

	private static XMLReader fallbackReader() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newSAXParser().getXMLReader();
	}

	private static void printParserImplementations() {
		System.out.println("INFO impl.sax=" + SAXParserFactory.newInstance().getClass().getName());
		System.out.println("INFO impl.stax=" + XMLInputFactory.newFactory().getClass().getName());
	}

	private static void measure(String label, int warmup, int iterations, Op op) throws Exception {
		for (int i = 0; i < warmup; i++) {
			collect(null, op, false);
		}
		List<Stats> measured = new ArrayList<>(iterations);
		for (int i = 0; i < iterations; i++) {
			collect(measured, op, true);
		}
		report(label, measured);
	}

	private static void collect(List<Stats> target, Op op, boolean record) throws Exception {
		ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		bean.setThreadAllocatedMemoryEnabled(true);
		System.gc();
		long cpuStart = bean.getCurrentThreadCpuTime();
		long allocStart = bean.getCurrentThreadAllocatedBytes();
		long wallStart = System.nanoTime();
		Object result = op.run();
		long wall = System.nanoTime() - wallStart;
		long cpu = bean.getCurrentThreadCpuTime() - cpuStart;
		long alloc = bean.getCurrentThreadAllocatedBytes() - allocStart;
		if (result == null) {
			throw new IllegalStateException("scenario returned null");
		}
		if (record && target != null) {
			target.add(new Stats(wall, cpu, alloc));
		}
	}

	private static void report(String label, List<Stats> measured) {
		double[] wall = new double[measured.size()];
		double[] cpu = new double[measured.size()];
		double[] alloc = new double[measured.size()];
		for (int i = 0; i < measured.size(); i++) {
			Stats stats = measured.get(i);
			wall[i] = stats.wallNanos() / 1_000_000.0;
			cpu[i] = stats.cpuNanos() / 1_000_000.0;
			alloc[i] = stats.allocatedBytes() / (1024.0 * 1024.0);
		}
		System.out.printf(Locale.ROOT,
			"RESULT label=%s iterations=%d median_wall_ms=%.1f min_wall_ms=%.1f median_cpu_ms=%.1f median_alloc_mb=%.1f wall_ms=%s%n",
			label, measured.size(), median(wall), minimum(wall), median(cpu), median(alloc), format(wall));
	}

	private static String format(double[] values) {
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(String.format(Locale.ROOT, "%.1f", values[i]));
		}
		return builder.append(']').toString();
	}

	private static double median(double[] values) {
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		int middle = sorted.length / 2;
		return (sorted.length & 1) == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2.0;
	}

	private static double minimum(double[] values) {
		double result = Double.MAX_VALUE;
		for (double value : values) {
			result = Math.min(result, value);
		}
		return result;
	}

	/**
	 * 只统计元素数量的 SAX 处理器。SAX handler that only counts elements.
	 */
	private static final class ElementCounter extends DefaultHandler {
		private long elements;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			elements++;
		}
	}
}
