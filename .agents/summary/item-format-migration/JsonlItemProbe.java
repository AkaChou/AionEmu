import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.sun.management.ThreadMXBean;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 物品模板存储格式探针：在同一分片上对比 JAXB/XML、纯 XML 扫描、纯 JSONL 解析、
 * JSONL+反射绑定四种路径的墙钟/CPU/分配量。
 *
 * Item-template storage-format probe: compares JAXB/XML, raw XML scan, raw JSONL parse and
 * JSONL+reflective binding on the same shard, reporting wall clock, thread CPU and allocation.
 *
 * 场景 / Scenarios:
 *   jaxb-item       JAXB unmarshal -> ItemData（与 XmlParserProbe 基线一致）
 *   sax-scan-item   SAX 扫描 XML，只累加属性数（XML 词法层下限）
 *   jsonl-parse     解析紧凑 JSONL，只解出 (索引, 值) 对（JSONL 解析层下限）
 *   jsonl-item      解析紧凑 JSONL + JAXB 注解驱动绑定到 ItemTemplate
 *
 * 只读数据文件；不启动服务端；不写回仓库。
 * Read-only: never starts the server, never writes back into the repository.
 */
public class JsonlItemProbe {

	private static final int DEFAULT_WARMUP = 2;
	private static final int DEFAULT_ITERATIONS = 5;
	private static final String ITEM_DATA = "com.aionemu.gameserver.dataholders.ItemData";
	/** 未解析路径只报告一次，避免污染多轮输出。 / Report unresolved paths once, not once per round. */
	private static final java.util.concurrent.atomic.AtomicBoolean unresolvedLogged =
		new java.util.concurrent.atomic.AtomicBoolean();

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("usage: JsonlItemProbe <scenario> <file> [warmup] [iterations]");
			System.exit(2);
		}
		String scenario = args[0];
		File file = new File(args[1]);

		if ("verify".equals(scenario)) {
			System.out.printf(Locale.ROOT, "INFO java=%s scenario=%s xml=%s%n",
				System.getProperty("java.version"), scenario, file.getPath());
			verify(file, args.length > 2 ? new File(args[2]) : null);
			return;
		}

		int warmup = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_WARMUP;
		int iterations = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_ITERATIONS;

		System.out.printf(Locale.ROOT, "INFO java=%s scenario=%s file=%s bytes=%d warmup=%d iterations=%d%n",
			System.getProperty("java.version"), scenario, file.getPath(), file.length(), warmup, iterations);

		switch (scenario) {
			case "jaxb-item" -> runJaxbItem(file, warmup, iterations);
			case "sax-scan-item" -> measure("sax-scan-item", warmup, iterations, () -> saxScan(file));
			case "jsonl-parse" -> measure("jsonl-parse", warmup, iterations, () -> jsonlParse(file));
			case "jsonl-item" -> measure("jsonl-item", warmup, iterations, () -> jsonlBind(file));
			case "batch-jaxb" -> measure("batch-jaxb", warmup, iterations, () -> batchJaxb(file));
			case "batch-jsonl" -> measure("batch-jsonl", warmup, iterations, () -> batchJsonl(file));
			case "jsonl-dict" -> measure("jsonl-dict", warmup, iterations, () -> jsonlDict(file));
			case "jsonl-dict-parse" -> measure("jsonl-dict-parse", warmup, iterations, () -> jsonlDictParse(file));
			case "batch-jsonl-dict" -> measure("batch-jsonl-dict", warmup, iterations, () -> batchJsonlDict(file));
			case "jsonl-dict-stream" -> measure("jsonl-dict-stream", warmup, iterations, () -> jsonlDictStream(file));
			case "batch-jsonl-dict-stream" -> measure("batch-jsonl-dict-stream", warmup, iterations,
				() -> batchJsonlDictStream(file));
			case "parallel-jaxb" -> measureParallel("parallel-jaxb", warmup, iterations, () -> parallelJaxb(file));
			case "parallel-jsonl" -> measureParallel("parallel-jsonl", warmup, iterations, () -> parallelJsonl(file));
			case "jsonl-plain" -> measure("jsonl-plain", warmup, iterations, () -> jsonlPlainBind(file));
			case "jsonl-plain-parse" -> measure("jsonl-plain-parse", warmup, iterations, () -> jsonlPlainParse(file));
			case "batch-jsonl-plain" -> measure("batch-jsonl-plain", warmup, iterations, () -> batchJsonlPlain(file));
			case "parallel-jsonl-plain" -> measureParallel("parallel-jsonl-plain", warmup, iterations,
				() -> parallelJsonlPlain(file));
			case "cold-jaxb" -> measureCold(file, args.length > 2 ? Integer.parseInt(args[2]) : 3, "jaxb");
			case "cold-jsonl" -> measureCold(file, args.length > 2 ? Integer.parseInt(args[2]) : 3, "dict");
			case "cold-jsonl-plain" -> measureCold(file, args.length > 2 ? Integer.parseInt(args[2]) : 3, "plain");
			case "count" -> {
				long a = batchJsonl(new File("/tmp/item-shards-jsonl"));
				long b = batchJsonlDict(new File("/tmp/item-shards-dict"));
				long c = batchJsonlDictStream(new File("/tmp/item-shards-dict"));
				long d = batchJsonlPlain(new File("/tmp/item-shards-plain"));
				System.out.printf(Locale.ROOT,
					"COUNT single-dict=%d double-dict=%d streaming=%d plain=%d (expect 128632)%n", a, b, c, d);
			}
			default -> {
				System.err.println("unknown scenario: " + scenario);
				System.exit(2);
			}
		}
	}

	// ------------------------------------------------------------------ JAXB 基线 / baseline

	private static void runJaxbItem(File file, int warmup, int iterations) throws Exception {
		Class<?> model = Class.forName(ITEM_DATA);
		JAXBContext context = JAXBContext.newInstance(model);
		measure("jaxb-item", warmup, iterations, () -> {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			try (InputStream input = new BufferedInputStream(new FileInputStream(file), 1 << 16)) {
				return unmarshaller.unmarshal(input);
			}
		});
	}

	// ------------------------------------------------------------------ XML / JSONL 解析层 / parse only

	/** 纯扫描计数器：只累加属性数，不构建对象。 / Scan-only counter: accumulates attribute count, builds nothing. */
	static final class CountingHandler extends DefaultHandler {
		long attributes;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {
			attributes += atts.getLength();
		}
	}

	private static long saxScan(File file) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(false);
		CountingHandler handler = new CountingHandler();
		try (InputStream input = new BufferedInputStream(new FileInputStream(file), 1 << 16)) {
			factory.newSAXParser().parse(input, handler);
		}
		return handler.attributes;
	}

	private static long jsonlParse(File file) throws Exception {
		JsonArrays.Row row = new JsonArrays.Row();
		long pairs = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String line = reader.readLine(); // 键表行 / key table line
			if (line == null) {
				throw new IllegalStateException("empty JSONL: " + file);
			}
			JsonArrays.readKeys(line);
			while ((line = reader.readLine()) != null) {
				pairs += JsonArrays.parseRow(line, row);
			}
		}
		return pairs;
	}

	// ------------------------------------------------------------------ JSONL 完整路径 / full path

	private static long jsonlBind(File file) throws Exception {
		List<ItemTemplate> templates = bindAll(file);
		// 等价 ItemData.afterUnmarshal 的索引构建 / index building equivalent to ItemData.afterUnmarshal
		Map<Integer, ItemTemplate> byId = new HashMap<>(templates.size() * 2);
		Map<String, ItemTemplate> byName = new HashMap<>(templates.size() * 2);
		for (ItemTemplate template : templates) {
			byId.put(template.getTemplateId(), template);
			String name = template.getName();
			if (name != null && !name.isBlank()) {
				byName.putIfAbsent(name.toLowerCase(Locale.ROOT), template);
			}
		}
		return byId.size() + byName.size();
	}

	/** 完整走一遍 JSONL 解析 + 绑定，返回模板列表。 / Full JSONL parse + bind pass, returning the template list. */
	static List<ItemTemplate> bindAll(File file) throws Exception {
		Binder binder = new Binder();
		JsonArrays.Row row = new JsonArrays.Row();
		List<ItemTemplate> templates = new ArrayList<>(16384);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String line = reader.readLine();
			if (line == null) {
				throw new IllegalStateException("empty JSONL: " + file);
			}
			String[] keys = JsonArrays.readKeys(line);
			List<String> unresolved = binder.precompile(keys);
			if (!unresolved.isEmpty() && unresolvedLogged.compareAndSet(false, true)) {
				System.out.println("INFO unresolved_paths=" + unresolved.size() + " " + unresolved);
			}
			while ((line = reader.readLine()) != null) {
				int count = JsonArrays.parseRow(line, row);
				ItemTemplate template = new ItemTemplate();
				binder.bind(template, row, count);
				binder.finish(template);
				templates.add(template);
			}
		}
		return templates;
	}

	// ------------------------------------------------------------------ 并行时序 / production-like parallelism

	/**
	 * 模拟 `XmlDataLoader.loadItemData` 的并行行为：固定池（生产 `STATIC_DATA_POOL` = CPU + 5）并发提交全部分片。
	 * 并行下线程级 CPU 无意义，改用**进程级 CPU**（`getProcessCpuTime`，含 JIT/GC 线程），
	 * 分配量由各工作线程自计后汇总。
	 *
	 * Mirrors `XmlDataLoader.loadItemData`: a fixed pool (production `STATIC_DATA_POOL` = CPU + 5)
	 * takes every shard concurrently. Thread CPU is meaningless under parallelism, so this arm
	 * reports **process CPU** (`getProcessCpuTime`, which includes JIT and GC threads); allocation is
	 * self-counted per worker and summed.
	 */
	private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() + 5;

	private static long processCpuNanos(java.lang.management.OperatingSystemMXBean os) {
		return os instanceof com.sun.management.OperatingSystemMXBean sun ? sun.getProcessCpuTime() : 0L;
	}

	private static void measureParallel(String label, int warmup, int iterations, Op op) throws Exception {
		java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		for (int i = 0; i < warmup; i++) {
			op.run();
		}
		List<double[]> measured = new ArrayList<>(iterations);
		for (int i = 0; i < iterations; i++) {
			System.gc();
			long cpuStart = processCpuNanos(os);
			long wallStart = System.nanoTime();
			long allocated = (Long) op.run();
			long wall = System.nanoTime() - wallStart;
			long cpu = processCpuNanos(os) - cpuStart;
			measured.add(new double[] {wall / 1_000_000.0, cpu / 1_000_000.0, allocated / (1024.0 * 1024.0)});
		}
		double[] wall = new double[measured.size()];
		double[] cpu = new double[measured.size()];
		double[] alloc = new double[measured.size()];
		for (int i = 0; i < measured.size(); i++) {
			wall[i] = measured.get(i)[0];
			cpu[i] = measured.get(i)[1];
			alloc[i] = measured.get(i)[2];
		}
		System.out.printf(Locale.ROOT,
			"RESULT label=%s iterations=%d pool=%d median_wall_ms=%.1f min_wall_ms=%.1f median_process_cpu_ms=%.1f median_alloc_mb=%.1f wall_ms=%s%n",
			label, measured.size(), POOL_SIZE, median(wall), minimum(wall), median(cpu), median(alloc), format(wall));
	}

	/** 并行 arm 的 JAXB 上下文缓存，对应生产的 `XmlDataLoader.SHARD_CONTEXTS`（首轮构建，后续复用）。
	 *  Context cache mirroring production's `XmlDataLoader.SHARD_CONTEXTS` (built once, reused). */
	private static final Map<Class<?>, JAXBContext> CONTEXT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private static long parallelJaxb(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".xml");
		Class<?> model = Class.forName(ITEM_DATA);
		JAXBContext context = CONTEXT_CACHE.computeIfAbsent(model, type -> {
			try {
				return JAXBContext.newInstance(type);
			} catch (jakarta.xml.bind.JAXBException e) {
				throw new IllegalStateException(e);
			}
		});
		Method size = model.getMethod("size");
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(POOL_SIZE);
		try {
			List<java.util.concurrent.Future<long[]>> futures = new ArrayList<>(shards.length);
			for (File shard : shards) {
				futures.add(pool.submit(() -> {
					ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
					long allocStart = thread.getCurrentThreadAllocatedBytes();
					Unmarshaller unmarshaller = context.createUnmarshaller();
					long count;
					try (InputStream input = new BufferedInputStream(new FileInputStream(shard), 1 << 16)) {
						count = (int) size.invoke(unmarshaller.unmarshal(input));
					}
					return new long[] {count, thread.getCurrentThreadAllocatedBytes() - allocStart};
				}));
			}
			long allocated = 0;
			for (java.util.concurrent.Future<long[]> future : futures) {
				allocated += future.get()[1];
			}
			return allocated;
		} finally {
			pool.shutdown();
		}
	}

	private static long parallelJsonl(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(POOL_SIZE);
		try {
			List<java.util.concurrent.Future<long[]>> futures = new ArrayList<>(shards.length);
			for (File shard : shards) {
				futures.add(pool.submit(() -> {
					ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
					long allocStart = thread.getCurrentThreadAllocatedBytes();
					int count = bindAllDictStreaming(shard).size();
					return new long[] {count, thread.getCurrentThreadAllocatedBytes() - allocStart};
				}));
			}
			long allocated = 0;
			for (java.util.concurrent.Future<long[]> future : futures) {
				allocated += future.get()[1];
			}
			return allocated;
		} finally {
			pool.shutdown();
		}
	}

	// ------------------------------------------------------------------ 冷启动对照 / cold-start comparison

	/**
	 * 首轮冷启动对照：独立 JVM、warmup=0，并同步记账类加载数与 JIT 编译时间。
	 *
	 * 公平性要点：生产的首次加载把 `JAXBContext` 构建包含在静态数据阶段内（`XmlDataLoader.unmarshalShard`
	 * 首次调用 `sharedJaxbContext` 时构建），因此 JAXB 侧必须用 `batchJaxbFull` 把 context 计入；
	 * JSONL 侧的绑定器元数据构建本来就在测量内。
	 *
	 * First-round cold comparison: fresh JVM, warmup=0, with class-load count and JIT compilation
	 * time booked alongside. Fairness note: production's first load pays JAXBContext construction
	 * inside the static-data phase (`XmlDataLoader.unmarshalShard` builds it on first
	 * `sharedJaxbContext` call), so the JAXB arm must include it via `batchJaxbFull`; the JSONL arm
	 * already builds its binder metadata inside the measurement.
	 */
	private static void measureCold(File dir, int rounds, String arm) throws Exception {
		java.lang.management.ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
		java.lang.management.CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();
		ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		thread.setThreadAllocatedMemoryEnabled(true);
		for (int round = 1; round <= rounds; round++) {
			long classStart = classLoading.getTotalLoadedClassCount();
			long jitStart = compilationTime(compilation);
			long cpuStart = thread.getCurrentThreadCpuTime();
			long allocStart = thread.getCurrentThreadAllocatedBytes();
			long wallStart = System.nanoTime();
			long result = switch (arm) {
				case "jaxb" -> batchJaxbFull(dir);
				case "dict" -> batchJsonlDictStream(dir);
				case "plain" -> batchJsonlPlain(dir);
				default -> throw new IllegalArgumentException("unknown cold arm: " + arm);
			};
			long wall = System.nanoTime() - wallStart;
			long cpu = thread.getCurrentThreadCpuTime() - cpuStart;
			long alloc = thread.getCurrentThreadAllocatedBytes() - allocStart;
			long classes = classLoading.getTotalLoadedClassCount() - classStart;
			long jit = compilationTime(compilation) - jitStart;
			System.out.printf(Locale.ROOT,
				"COLD arm=%s round=%d templates=%d wall_ms=%.1f cpu_ms=%.1f alloc_mb=%.1f classes_loaded=%d jit_ms=%d%n",
				arm, round, result, wall / 1_000_000.0, cpu / 1_000_000.0,
				alloc / (1024.0 * 1024.0), classes, jit);
		}
	}

	private static long compilationTime(java.lang.management.CompilationMXBean compilation) {
		if (compilation == null) {
			return 0L;
		}
		try {
			return compilation.isCompilationTimeMonitoringSupported() ? compilation.getTotalCompilationTime() : 0L;
		} catch (UnsupportedOperationException e) {
			return 0L;
		}
	}

	/** 与 `batchJaxb` 相同，但把 `JAXBContext` 构建计入测量（模拟生产首次加载）。
	 *  Same as `batchJaxb` but with `JAXBContext` construction inside the measurement (production cold load). */
	private static long batchJaxbFull(File dir) throws Exception {
		Class<?> model = Class.forName(ITEM_DATA);
		JAXBContext context = JAXBContext.newInstance(model);
		Method size = model.getMethod("size");
		long total = 0;
		for (File shard : shardsOf(dir, ".xml")) {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			try (InputStream input = new BufferedInputStream(new FileInputStream(shard), 1 << 16)) {
				total += (int) size.invoke(unmarshaller.unmarshal(input));
			}
		}
		return total;
	}

	// ------------------------------------------------------------------ 双字典路径 / double-dictionary path

	/**
	 * 双字典路径：首行同时携带路径字典与值字典，行内只有整数索引。
	 * 值不再逐条新建 String——全体记录共享值字典里的实例，这是分配量的主要优化点。
	 *
	 * Double-dictionary path: the header carries both a path dictionary and a value dictionary,
	 * rows contain integer indices only. Values are no longer allocated per record: every record
	 * shares the instances held by the value dictionary, which is the main allocation saving.
	 */

	/** 一次性读入整个文件为 char[]，避免逐行 readLine 产生的行字符串。
	 *  Reads the whole file into a char[] to avoid one line String per readLine. */
	private static char[] readFully(File file) throws Exception {
		char[] buffer = new char[(int) file.length() + 16];
		int filled = 0;
		try (java.io.Reader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			int read;
			while (filled < buffer.length && (read = reader.read(buffer, filled, buffer.length - filled)) > 0) {
				filled += read;
			}
		}
		return filled == buffer.length ? buffer : Arrays.copyOf(buffer, filled);
	}

	private static int indexOf(char[] text, char needle, int from) {
		return indexOf(text, needle, from, text.length);
	}

	private static int indexOf(char[] text, char needle, int from, int to) {
		for (int i = from; i < to; i++) {
			if (text[i] == needle) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 流式版本：只把字典行读成 String（一行，不可避免），数据行在固定缓冲里扫描。
	 * `bindAllDict` 的实测分配里有 6.6MB/分片 来自整文件 char[]，正好是它与 JAXB 分配量之差；
	 * 换成固定缓冲后这 6.6MB 消失，而行与值本来就不产生新 String。
	 *
	 * Streaming variant: only the dictionary line becomes a String (one line, unavoidable); data
	 * rows are scanned out of a fixed buffer. In `bindAllDict` the whole-file char[] accounted for
	 * 6.6MB per shard — precisely the gap to JAXB's allocation — and a fixed buffer removes it,
	 * while rows and values already allocate nothing.
	 */
	static List<ItemTemplate> bindAllDictStreaming(File file) throws Exception {
		Binder binder = new Binder();
		int[] row = new int[512];
		List<ItemTemplate> templates = new ArrayList<>(16384);
		char[] buffer = new char[1 << 18];
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String header = reader.readLine();
			if (header == null) {
				throw new IllegalStateException("empty file: " + file);
			}
			String[][] dictionaries = readDictionaries(header);
			String[] values = dictionaries[1];
			List<String> unresolved = binder.precompile(dictionaries[0]);
			if (!unresolved.isEmpty() && unresolvedLogged.compareAndSet(false, true)) {
				System.out.println("INFO unresolved_paths=" + unresolved.size() + " " + unresolved);
			}
			int end = 0;
			boolean eof = false;
			while (!eof) {
				int read = reader.read(buffer, end, buffer.length - end);
				if (read < 0) {
					eof = true;
				} else {
					end += read;
				}
				int lineStart = 0;
				int newline;
				while ((newline = indexOf(buffer, '\n', lineStart, end)) >= 0) {
					templates.add(bindRow(buffer, lineStart, newline, binder, row, values));
					lineStart = newline + 1;
				}
				if (eof && lineStart < end) {
					templates.add(bindRow(buffer, lineStart, end, binder, row, values));
					lineStart = end;
				}
				if (lineStart > 0) {
					System.arraycopy(buffer, lineStart, buffer, 0, end - lineStart);
					end -= lineStart;
				}
				if (end == buffer.length) {
					throw new IllegalStateException("row exceeds scan buffer in " + file);
				}
			}
		}
		return templates;
	}

	private static ItemTemplate bindRow(char[] buffer, int lineStart, int lineEnd, Binder binder,
			int[] row, String[] values) {
		int count = JsonArrays.parseIndexRow(buffer, lineStart, lineEnd, row);
		ItemTemplate template = new ItemTemplate();
		binder.bindIndexed(template, row, values, count);
		binder.finish(template);
		return template;
	}

	private static long jsonlDictStream(File file) throws Exception {
		List<ItemTemplate> templates = bindAllDictStreaming(file);
		Map<Integer, ItemTemplate> byId = new HashMap<>(templates.size() * 2);
		for (ItemTemplate template : templates) {
			byId.put(template.getTemplateId(), template);
		}
		return byId.size();
	}

	private static long batchJsonlDictStream(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		long total = 0;
		for (File shard : shards) {
			total += bindAllDictStreaming(shard).size();
		}
		return total;
	}

	/** 从首行解析出路径字典与值字典。 / Parses the path dictionary and value dictionary out of the header line. */
	private static String[][] readDictionaries(String header) {
		int marker = header.indexOf("\"_values\"");
		if (marker < 0) {
			throw new IllegalStateException("header lacks _values dictionary");
		}
		return new String[][] {
			JsonArrays.parseStringArray(header.substring(0, marker)),
			JsonArrays.parseStringArray(header.substring(marker))
		};
	}

	private static long jsonlDictParse(File file) throws Exception {
		char[] text = readFully(file);
		int headerEnd = indexOf(text, '\n', 0);
		int[] row = new int[512];
		long total = 0;
		int lineStart = headerEnd + 1;
		while (lineStart < text.length) {
			int lineEnd = indexOf(text, '\n', lineStart);
			if (lineEnd < 0) {
				lineEnd = text.length;
			}
			total += JsonArrays.parseIndexRow(text, lineStart, lineEnd, row);
			lineStart = lineEnd + 1;
		}
		return total;
	}

	private static long jsonlDict(File file) throws Exception {
		List<ItemTemplate> templates = bindAllDict(file);
		Map<Integer, ItemTemplate> byId = new HashMap<>(templates.size() * 2);
		for (ItemTemplate template : templates) {
			byId.put(template.getTemplateId(), template);
		}
		return byId.size();
	}

	static List<ItemTemplate> bindAllDict(File file) throws Exception {
		char[] text = readFully(file);
		int headerEnd = indexOf(text, '\n', 0);
		if (headerEnd < 0) {
			throw new IllegalStateException("no header line in " + file);
		}
		String[][] dictionaries = readDictionaries(new String(text, 0, headerEnd));
		String[] values = dictionaries[1];

		Binder binder = new Binder();
		List<String> unresolved = binder.precompile(dictionaries[0]);
		if (!unresolved.isEmpty() && unresolvedLogged.compareAndSet(false, true)) {
			System.out.println("INFO unresolved_paths=" + unresolved.size() + " " + unresolved);
		}
		int[] row = new int[512];
		List<ItemTemplate> templates = new ArrayList<>(16384);
		int lineStart = headerEnd + 1;
		while (lineStart < text.length) {
			int lineEnd = indexOf(text, '\n', lineStart);
			if (lineEnd < 0) {
				lineEnd = text.length;
			}
			int count = JsonArrays.parseIndexRow(text, lineStart, lineEnd, row);
			ItemTemplate template = new ItemTemplate();
			binder.bindIndexed(template, row, values, count);
			binder.finish(template);
			templates.add(template);
			lineStart = lineEnd + 1;
		}
		return templates;
	}

	// ------------------------------------------------------------------ 批量 / batch (all shards, serial)

	/** 串行处理目录下全部分片，模拟生产 item 加载阶段的完整工作量。
	 *  Serially processes every shard in the directory, mirroring the full production item-load workload. */
	private static long batchJaxb(File dir) throws Exception {
		Class<?> model = Class.forName(ITEM_DATA);
		JAXBContext context = JAXBContext.newInstance(model);
		Method size = model.getMethod("size");
		File[] shards = shardsOf(dir, ".xml");
		long total = 0;
		for (File shard : shards) {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			try (InputStream input = new BufferedInputStream(new FileInputStream(shard), 1 << 16)) {
				Object data = unmarshaller.unmarshal(input);
				total += (int) size.invoke(data);
			}
		}
		return total;
	}

	private static long batchJsonl(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		long total = 0;
		for (File shard : shards) {
			total += bindAll(shard).size();
		}
		return total;
	}

	private static long batchJsonlDict(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		long total = 0;
		for (File shard : shards) {
			total += bindAllDict(shard).size();
		}
		return total;
	}

	private static File[] shardsOf(File dir, String suffix) {
		File[] shards = dir.listFiles((d, name) -> name.startsWith("item_template_") && name.endsWith(suffix));
		if (shards == null || shards.length == 0) {
			throw new IllegalStateException("no shards matching *" + suffix + " under " + dir);
		}
		Arrays.sort(shards, java.util.Comparator.comparing(File::getName));
		return shards;
	}

	// ------------------------------------------------------------------ 正确性校验 / correctness check

	/**
	 * 逐字段比对 JAXB 与 JSONL 两条路径产出的 ItemTemplate，确认探针测的不是"快但错"的路径。
	 * Field-by-field comparison of the ItemTemplate objects produced by the JAXB and JSONL paths,
	 * so the probe cannot report a fast-but-wrong route as a win.
	 */
	@SuppressWarnings("unchecked")
	private static void verify(File xmlFile, File jsonlFile) throws Exception {
		if (jsonlFile == null) {
			jsonlFile = new File("/tmp/item-shard-1.compact.jsonl");
		}
		Class<?> model = Class.forName(ITEM_DATA);
		JAXBContext context = JAXBContext.newInstance(model);
		Object itemData = context.createUnmarshaller()
			.unmarshal(new BufferedInputStream(new FileInputStream(xmlFile), 1 << 16));
		Method accessor = model.getDeclaredMethod("getItemData");
		accessor.setAccessible(true);
		Map<Integer, ItemTemplate> reference = (Map<Integer, ItemTemplate>) accessor.invoke(itemData);

		// 首行决定解析器：_values=双字典，_keys=单字典紧凑，两者都不是=标准 JSONL。
		// The header picks the reader: _values = double-dictionary, _keys = compact single-dictionary,
		// neither = plain JSONL.
		String header;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(jsonlFile), StandardCharsets.UTF_8))) {
			header = reader.readLine();
		}
		List<ItemTemplate> mine;
		if (header != null && header.contains("\"_values\"")) {
			mine = bindAllDictStreaming(jsonlFile);
		} else if (header != null && header.contains("\"_keys\"")) {
			mine = bindAll(jsonlFile);
		} else {
			mine = bindAllPlain(jsonlFile);
		}

		System.out.printf(Locale.ROOT, "VERIFY jaxb_templates=%d jsonl_templates=%d%n", reference.size(), mine.size());
		List<String> samples = new ArrayList<>();
		int missing = 0;
		int mismatched = 0;
		int comparedFields = 0;
		for (ItemTemplate template : mine) {
			ItemTemplate expected = reference.get(template.getTemplateId());
			if (expected == null) {
				missing++;
				if (samples.size() < 12) {
					samples.add("missing id=" + template.getTemplateId());
				}
				continue;
			}
			int[] result = new int[2];
			diff(template, expected, "", 0, result, samples);
			mismatched += result[0];
			comparedFields += result[1];
		}
		System.out.printf(Locale.ROOT,
			"VERIFY compared_fields=%d mismatched_fields=%d missing_templates=%d%n",
			comparedFields, mismatched, missing);
		for (String sample : samples) {
			System.out.println("VERIFY sample: " + sample);
		}
		int referenceOnly = reference.size() - (mine.size() - missing);
		System.out.printf(Locale.ROOT, "VERIFY jaxb_only_templates=%d%n", referenceOnly);
	}

	/** 递归比对：字段计数写入 result[1]，不一致计数写入 result[0]。
	 *  Recursive comparison: result[1] counts fields, result[0] counts mismatches. */
	@SuppressWarnings("unchecked")
	private static void diff(Object mine, Object reference, String path, int depth, int[] result, List<String> samples) {
		if (depth > 4) {
			return;
		}
		for (Class<?> current = mine.getClass(); current != null && current != Object.class;
				current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
						|| field.isAnnotationPresent(jakarta.xml.bind.annotation.XmlTransient.class)) {
					continue;
				}
				field.setAccessible(true);
				Object left;
				Object right;
				try {
					left = field.get(mine);
					right = field.get(reference);
				} catch (IllegalAccessException e) {
					continue;
				}
				String fieldPath = path + "/" + field.getName();
				if (left instanceof List<?> leftList && right instanceof List<?> rightList) {
					result[1]++;
					if (leftList.size() != rightList.size()) {
						result[0]++;
						if (samples.size() < 12) {
							StringBuilder mineTypes = new StringBuilder();
							for (Object item : leftList) {
								mineTypes.append(item.getClass().getSimpleName()).append(' ');
							}
							StringBuilder referenceTypes = new StringBuilder();
							for (Object item : rightList) {
								referenceTypes.append(item.getClass().getSimpleName()).append(' ');
							}
							samples.add(fieldPath + " size " + leftList.size() + " vs " + rightList.size()
								+ "  mine=[" + mineTypes.toString().trim() + "] ref=[" + referenceTypes.toString().trim() + "]");
						}
						continue;
					}
					for (int i = 0; i < leftList.size(); i++) {
						Object leftItem = leftList.get(i);
						Object rightItem = rightList.get(i);
						if (leftItem == null || rightItem == null || isScalar(leftItem.getClass())) {
							result[1]++;
							if (!java.util.Objects.equals(leftItem, rightItem) && samples.size() < 12) {
								samples.add(fieldPath + "[" + i + "] " + leftItem + " vs " + rightItem);
							}
						} else {
							diff(leftItem, rightItem, fieldPath + "[" + i + "]", depth + 1, result, samples);
						}
					}
					continue;
				}
				if (isScalar(field.getType())) {
					result[1]++;
					if (!java.util.Objects.equals(left, right)) {
						result[0]++;
						if (samples.size() < 12) {
							samples.add(fieldPath + " = " + left + " vs " + right);
						}
					}
					continue;
				}
				if (left != null && right != null) {
					diff(left, right, fieldPath, depth + 1, result, samples);
				}
			}
		}
	}

	private static boolean isScalar(Class<?> type) {
		return type.isPrimitive() || Number.class.isAssignableFrom(type) || type == String.class
			|| type == Boolean.class || type == Character.class || type.isEnum();
	}

	// ------------------------------------------------------------------ 绑定器 / binder

	/**
	 * JAXB 注解驱动的绑定器：按 JSONL 的路径（如 {@code /actions/craftlearn@skillid}）沿注解元数据
	 * 导航对象图并写入字段值。元数据按类缓存，路径按字符串缓存。
	 *
	 * JAXB-annotation-driven binder: walks the object graph along annotated metadata following the
	 * JSONL path (e.g. {@code /actions/craftlearn@skillid}) and writes the value into the field.
	 * Class metadata and navigation steps are cached.
	 */
	static final class Binder {
		/** restrict 的 17 项长度固定（4.3/4.5 各职业限制）。 / restrict is always 17 entries. */
		private static final int RESTRICT_LENGTH = 17;
		/** 无 restrict 时的共享占位数组。 / Shared placeholder when a record carries no restrict. */
		private static final int[] DEFAULT_RESTRICTS = new int[RESTRICT_LENGTH];
		/** `ItemTemplate.emptyWeaponStats`，经反射取得以避免新增 import / obtained reflectively to avoid a new import. */
		private static final Object EMPTY_WEAPON_STATS;
		private static final Method SET_WEAPON_STATS;

		static {
			try {
				Field field = ItemTemplate.class.getDeclaredField("emptyWeaponStats");
				field.setAccessible(true);
				EMPTY_WEAPON_STATS = field.get(null);
				SET_WEAPON_STATS = ItemTemplate.class.getMethod("setWeaponStats", EMPTY_WEAPON_STATS.getClass());
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("cannot reach ItemTemplate.emptyWeaponStats", e);
			}
		}

		private final Map<Class<?>, ClassMeta> classCache = new HashMap<>();
		private final Map<String, Object> currentByPath = new HashMap<>();
		/** 元素存在、但属性在模型里没有字段的路径（JAXB 同样忽略）。
		 *  Paths whose element exists but whose attribute has no model field (JAXB ignores these too). */
		final List<String> ignoredAttributes = new ArrayList<>();
		private final Map<String, Set<String>> seenAttrs = new HashMap<>();
		/**
		 * restrict / restrict_max 的解析结果缓存，键是值字典里那个共享 String。
		 * 全量只有 767 种不同的 restrict 取值，而 128,632 条记录全都带它——不缓存的话每条记录
		 * 都要付 `split(",")` 的 17 个 String（全量约 218 万个），并各自持有一个 int[17]。
		 * 缓存后解析只发生 767 次，且所有记录**共享同一个 int[] 实例**。
		 *
		 * Parse-result cache for restrict / restrict_max, keyed by the shared dictionary String.
		 * Only 767 distinct restrict values exist while all 128,632 records carry one: uncached,
		 * every record pays `split(",")` (≈2.18M Strings across the dataset) plus its own int[17].
		 * Cached, parsing happens 767 times and every record shares the same int[] instance.
		 */
		private final Map<String, int[]> restrictCache = new HashMap<>();
		private final Map<String, byte[]> restrictMaxCache = new HashMap<>();

		Step[] steps = new Step[0];

		/** 按键表预解析全部路径，返回无法映射到 JAXB 模型的路径。
		 *  Pre-resolves every key-table path; returns the paths that do not map onto the JAXB model. */
		List<String> precompile(String[] keys) {
			steps = new Step[keys.length];
			ignoredAttributes.clear();
			List<String> unresolved = new ArrayList<>();
			for (int i = 0; i < keys.length; i++) {
				steps[i] = compile(keys[i]);
				if (steps[i] == null) {
					unresolved.add(keys[i]);
				}
			}
			return unresolved;
		}

		void bind(ItemTemplate root, JsonArrays.Row row, int count) {
			currentByPath.clear();
			seenAttrs.clear();
			for (int i = 0; i < count; i++) {
				Step step = steps[row.indexes[i]];
				if (step == null) {
					continue;
				}
				Object owner = resolve(root, step);
				if (owner != null && step.attribute != null) {
					applyAttribute(owner, step.attribute, row.values[i]);
				}
			}
		}

		/** 双字典路径：值由索引从值字典取出，全体记录共享同一批 String 实例。
		 *  Double-dictionary path: values are resolved from the value dictionary by index,
		 *  so every record shares the same String instances. */
		void bindIndexed(ItemTemplate root, int[] indexes, String[] values, int count) {
			currentByPath.clear();
			seenAttrs.clear();
			for (int i = 0; i + 1 < count; i += 2) {
				Step step = steps[indexes[i]];
				if (step == null) {
					continue;
				}
				Object owner = resolve(root, step);
				if (owner != null && step.attribute != null) {
					applyAttribute(owner, step.attribute, values[indexes[i + 1]]);
				}
			}
		}

		/** 设置一个标量或集合属性。集合属性：{@code @XmlList} 按空格切分，否则整个值作单元素
		 *  （`Stigma.skill` 就是后者——数据里整个值成为 List 的唯一元素）。
		 *  Sets a scalar or collection attribute. Collection attributes: {@code @XmlList} splits on
		 *  spaces, otherwise the whole value becomes a single element (`Stigma.skill` is the latter:
		 *  the whole value lands as the list's only element). */
		static void applyAttribute(Object owner, Accessor accessor, String value) {
			try {
				if (accessor.isList) {
					List<Object> list = new ArrayList<>(accessor.xmlList ? 2 : 1);
					if (accessor.xmlList) {
						for (String part : value.split(" ")) {
							list.add(JsonArrays.convert(part, accessor.elementType));
						}
					} else {
						list.add(JsonArrays.convert(value, accessor.elementType));
					}
					accessor.field.set(owner, list);
				} else {
					accessor.field.set(owner, JsonArrays.convert(value, accessor.valueType));
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("cannot set " + accessor.field, e);
			}
		}

		/**
		 * 复刻 `ItemTemplate.afterUnmarshal` 的收尾语义，但 restrict 走共享缓存：
		 * 跳过 `split(",")` 产生的 17 个临时 String 与逐记录的 int[17]。
		 * 其余三项（itemId、restrictsMax、weaponStats 默认值）与 JAXB 行为逐字对齐。
		 *
		 * Replicates the `ItemTemplate.afterUnmarshal` tail, but routes restrict through the shared
		 * cache: skips the 17 temporary Strings of `split(",")` and the per-record int[17]. The other
		 * three steps (itemId, restrictsMax, weaponStats default) match JAXB exactly.
		 */
		void finish(ItemTemplate template) {
			String id = template.getId();
			template.setItemId(id == null ? 0 : Integer.parseInt(id));
			String restrict = template.getRestrict();
			template.setRestricts(restrict == null ? DEFAULT_RESTRICTS : restrictCacheFor(restrict));
			String restrictMax = template.getRestrictMax();
			if (restrictMax != null) {
				template.setRestrictsMax(restrictMaxCacheFor(restrictMax));
			}
			if (template.getWeaponStats() == null) {
				try {
					SET_WEAPON_STATS.invoke(template, EMPTY_WEAPON_STATS);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("cannot set empty weapon stats", e);
				}
			}
		}

		private int[] restrictCacheFor(String value) {
			int[] cached = restrictCache.get(value);
			if (cached == null) {
				cached = parseIntArray(value, RESTRICT_LENGTH);
				restrictCache.put(value, cached);
			}
			return cached;
		}

		private byte[] restrictMaxCacheFor(String value) {
			byte[] cached = restrictMaxCache.get(value);
			if (cached == null) {
				int[] parsed = parseIntArray(value, RESTRICT_LENGTH);
				cached = new byte[RESTRICT_LENGTH];
				for (int i = 0; i < parsed.length; i++) {
					cached[i] = (byte) parsed[i];
				}
				restrictMaxCache.put(value, cached);
			}
			return cached;
		}

		/** 从 "1,1,1,..." 直接扫成定长 int[]，不经过 split。 / Scans "1,1,..." into a fixed int[] without split. */
		private static int[] parseIntArray(String value, int length) {
			int[] out = new int[length];
			int index = 0;
			int accumulator = 0;
			boolean negative = false;
			boolean inNumber = false;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == ',') {
					if (index < length) {
						out[index++] = negative ? -accumulator : accumulator;
					}
					accumulator = 0;
					negative = false;
					inNumber = false;
				} else if (c == '-') {
					negative = true;
				} else if (c >= '0' && c <= '9') {
					accumulator = accumulator * 10 + (c - '0');
					inNumber = true;
				}
			}
			if (inNumber && index < length) {
				out[index] = negative ? -accumulator : accumulator;
			}
			return out;
		}

		/** 沿预解析的元素链导航，返回应当承载属性的对象（必要时创建列表元素）。
		 *  Walks the pre-resolved element chain and returns the object that should receive the
		 *  attribute (creating list elements when needed). */
		private Object resolve(ItemTemplate root, Step step) {
			Object owner = root;
			Accessor[] chain = step.chain;
			for (int i = 0; i < chain.length; i++) {
				Accessor accessor = chain[i];
				boolean last = i == chain.length - 1;
				owner = accessor.isList
					? listElement(owner, accessor, step.prefixes[i], last ? step.attributeName : null)
					: singleElement(owner, accessor);
				if (owner == null) {
					return null;
				}
			}
			return owner;
		}

		private Object singleElement(Object owner, Accessor accessor) {
			try {
				Object value = accessor.field.get(owner);
				if (value == null) {
					value = accessor.elementType.getDeclaredConstructor().newInstance();
					accessor.field.set(owner, value);
				}
				return value;
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("cannot instantiate " + accessor.elementType, e);
			}
		}

		/**
		 * 列表元素：同一路径的属性名出现回绕时视为新元素（XML 属性顺序保证了元素内属性相邻）。
		 * List element: a repeated attribute name on the same path starts a new element
		 * (XML attribute order keeps one element's attributes adjacent).
		 */
		private Object listElement(Object owner, Accessor accessor, String path, String attribute) {
			try {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) accessor.field.get(owner);
				if (list == null) {
					list = new ArrayList<>();
					accessor.field.set(owner, list);
				}
				Set<String> seen = seenAttrs.computeIfAbsent(path, key -> new HashSet<>());
				Object latest = currentByPath.get(path);
				if (latest == null || (attribute != null && !seen.add(attribute))) {
					latest = accessor.elementType.getDeclaredConstructor().newInstance();
					list.add(latest);
					currentByPath.put(path, latest);
					seenAttrs.put(path, new HashSet<>());
					if (attribute != null) {
						seenAttrs.get(path).add(attribute);
					}
				}
				return latest;
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("cannot grow list " + accessor.field, e);
			}
		}

		/** 把路径解析成「元素链 + 末尾属性」，并从 JAXB 注解读出对应的 accessor 链。
		 *  Resolves a path into an element chain plus trailing attribute, reading the matching
		 *  accessor chain out of the JAXB annotations. */
		private Step compile(String path) {
			String[] rawParts = path.split("/");
			List<String> elementNames = new ArrayList<>(2);
			String attributeName = null;
			for (int i = 0; i < rawParts.length; i++) {
				String part = rawParts[i];
				if (part.isEmpty()) {
					continue;
				}
				int at = part.indexOf('@');
				if (i == rawParts.length - 1 && at >= 0) {
					attributeName = part.substring(at + 1);
					if (at > 0) {
						elementNames.add(part.substring(0, at));
					}
				} else {
					elementNames.add(part);
				}
			}
			Accessor[] chain = new Accessor[elementNames.size()];
			String[] prefixes = new String[elementNames.size()];
			StringBuilder prefix = new StringBuilder();
			Class<?> type = ItemTemplate.class;
			for (int i = 0; i < chain.length; i++) {
				String element = elementNames.get(i);
				prefix.append('/').append(element);
				prefixes[i] = prefix.toString();
				int bracket = element.indexOf('[');
				String lookup = bracket < 0 ? element : element.substring(0, bracket);
				ClassMeta meta = classCache.computeIfAbsent(type, Binder::buildMeta);
				Accessor accessor = meta.elements.get(lookup);
				if (accessor == null) {
					return null;
				}
				chain[i] = accessor;
				type = accessor.elementType;
			}
			Accessor attribute = null;
			if (attributeName != null) {
				ClassMeta meta = classCache.computeIfAbsent(type, Binder::buildMeta);
				attribute = meta.attributes.get(attributeName);
				if (attribute == null) {
					// 数据比模型新：元素存在，但它的这个属性在模型里没有对应字段
					// （如 instancetimeclear@sync_ids、activate_target）。JAXB 照样创建元素、
					// 只是忽略该属性；把 attributeName 置空即可让 resolve 仍走完元素链，
					// 同时 listElement 只在首次出现时建一个实例。
					// Data newer than the model: the element exists but this attribute has no field
					// (e.g. instancetimeclear@sync_ids, activate_target). JAXB still creates the element
					// and merely drops the attribute; nulling attributeName keeps the element chain
					// intact while listElement stays a create-once.
					ignoredAttributes.add(path);
					attributeName = null;
				}
			}
			return new Step(prefixes, chain, attributeName, attribute);
		}

		static ClassMeta buildMeta(Class<?> type) {
			ClassMeta meta = new ClassMeta();
			for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					XmlAttribute attribute = field.getAnnotation(XmlAttribute.class);
					if (attribute != null) {
						field.setAccessible(true);
						boolean attributeList = List.class.isAssignableFrom(field.getType());
						Class<?> attributeElementType = attributeList
							? (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
								.getActualTypeArguments()[0]
							: null;
						boolean xmlList = attributeList
							&& field.isAnnotationPresent(jakarta.xml.bind.annotation.XmlList.class);
						String key = nameOf(attribute.name(), field);
						meta.attributes.put(key,
							Accessor.ofAttribute(key, field, field.getType(), attributeElementType, attributeList, xmlList));
						continue;
					}
					XmlElement element = field.getAnnotation(XmlElement.class);
					if (element != null) {
						field.setAccessible(true);
						boolean list = List.class.isAssignableFrom(field.getType());
						Class<?> elementType;
						if (element.type() != XmlElement.DEFAULT.class) {
							elementType = element.type();
						} else if (list) {
							// List<T> 字段的 getType() 是 List，元素类型必须从泛型实参取。
							// For a List<T> field getType() yields List; the element type must come from the generic argument.
							elementType = (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType())
								.getActualTypeArguments()[0];
						} else {
							elementType = field.getType();
						}
						String key = nameOf(element.name(), field);
						meta.elements.put(key, Accessor.ofElement(key, field, elementType, elementType, list));
						continue;
					}
					XmlElements elements = field.getAnnotation(XmlElements.class);
					if (elements != null) {
						field.setAccessible(true);
						for (XmlElement nested : elements.value()) {
							meta.elements.put(nested.name(),
								Accessor.ofPolymorphic(nested.name(), field, nested.type()));
						}
					}
				}
			}
			meta.indexKeys();
			return meta;
		}

		private static String nameOf(String declared, Field field) {
			return declared == null || declared.isEmpty() || "##default".equals(declared) ? field.getName() : declared;
		}
	}

	/** 预解析后的路径：元素链 accessor + 末尾属性 accessor，绑定期不再查表。
	 *  A pre-resolved path: element-chain accessors plus the trailing attribute accessor,
	 *  so binding performs no further lookups. */
	static final class Step {
		final String[] prefixes;
		final Accessor[] chain;
		final String attributeName;
		final Accessor attribute;

		Step(String[] prefixes, Accessor[] chain, String attributeName, Accessor attribute) {
			this.prefixes = prefixes;
			this.chain = chain;
			this.attributeName = attributeName;
			this.attribute = attribute;
		}
	}

	static final class ClassMeta {
		final Map<String, Accessor> attributes = new HashMap<>();
		final Map<String, Accessor> elements = new HashMap<>();

		/**
		 * 键名索引：把 attributes 与 elements 合并成一张开放寻址表，让标准 JSONL 的键查找走
		 * 「字符区间 + 整数哈希」而不是「建 String + HashMap 查找」。
		 *
		 * 动机：plain 每行约 20 个键 × 128,632 条 ≈ **257 万个键名 String**，正是它分配量比
		 * JAXB 高 16.8% 的主因；同时 HashMap 的 `String.hashCode()` 要再遍历一遍字符，而这里
		 * 的哈希在扫描键名时**顺带算出**，一次遍历完成两件事。
		 *
		 * 属性与元素合并为一张表是安全的：数据里没有同名属性与子元素（若有过，JSON 的单层键
		 * 空间本就无法表达，生成器会先撞上）。属性先插入，因而在探测顺序上仍占先——与原先
		 * 「先查 attributes 再查 elements」的优先级一致。
		 *
		 * Key index: merges attributes and elements into one open-addressed table so plain JSONL's
		 * key lookup runs on "character region + integer hash" instead of "build a String + HashMap".
		 *
		 * Why: plain has ~20 keys per line × 128,632 records ≈ **2.57M key Strings**, the main
		 * reason its allocation runs 16.8% above JAXB. A HashMap lookup would also walk the
		 * characters again for `String.hashCode()`; here the hash falls out of the scan itself.
		 *
		 * Merging attributes and elements is safe: no data carries an attribute and a child element
		 * of the same name (JSON's flat key space could not express it anyway, and the generator
		 * would hit that first). Attributes are inserted first, so probe order keeps the original
		 * "attributes before elements" precedence.
		 */
		private Accessor[] slots;
		private int[] slotHashes;
		private int mask;

		void indexKeys() {
			int count = attributes.size() + elements.size();
			int size = 4;
			while (size < count * 2) {
				size <<= 1;
			}
			slots = new Accessor[size];
			slotHashes = new int[size];
			mask = size - 1;
			for (Accessor accessor : attributes.values()) {
				insert(accessor);
			}
			for (Accessor accessor : elements.values()) {
				insert(accessor);
			}
		}

		private void insert(Accessor accessor) {
			int i = accessor.keyHash & mask;
			while (slots[i] != null) {
				i = (i + 1) & mask;
			}
			slots[i] = accessor;
			slotHashes[i] = accessor.keyHash;
		}

		/** 按 {@link JsonReader#readKeyRegion()} 留下的区间与哈希查表；键名不存在时返回 null。
		 *  Looks up by the region and hash left by {@link JsonReader#readKeyRegion()}; null when absent. */
		Accessor byKey(JsonReader reader) {
			int hash = reader.keyHash;
			int i = hash & mask;
			while (true) {
				Accessor accessor = slots[i];
				if (accessor == null) {
					return null;
				}
				if (slotHashes[i] == hash && matches(accessor.key, reader)) {
					return accessor;
				}
				i = (i + 1) & mask;
			}
		}

		private static boolean matches(String key, JsonReader reader) {
			int length = key.length();
			if (length != reader.keyEnd - reader.keyStart) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				if (key.charAt(i) != reader.keyCharAt(i)) {
					return false;
				}
			}
			return true;
		}
	}

	static final class Accessor {
		/** 键名（XML 属性名或元素名）。标准 JSONL 的键名查找靠它做冲突时的逐字符比对。
		 *  The key name (XML attribute or element name); plain JSONL's key lookup compares against
		 *  it character by character on a hash collision. */
		final String key;
		/** {@code key.hashCode()}，与 {@link JsonReader#readKeyRegion()} 在字符上算出的哈希同算法。
		 *  {@code key.hashCode()}, the same algorithm {@link JsonReader#readKeyRegion()} computes on
		 *  the raw characters. */
		final int keyHash;
		final Field field;
		final Class<?> valueType;
		final Class<?> elementType;
		final boolean isList;
		/** {@code @XmlList} 属性：值按空格切分成多个元素；否则整个值作为单元素。
		 *  {@code @XmlList} attribute: the value splits on spaces; otherwise the whole value is one element. */
		final boolean xmlList;
		/** 来自 {@code @XmlElements}：这是多态列表的一个**元素名**（`add`/`rate`/`read`…），
		 *  值是元素本身，随后追加到共同的列表字段。标准 JSONL 里以单键对象出现。
		 *  From {@code @XmlElements}: this is one **element name** of a polymorphic list
		 *  (`add`/`rate`/`read`…); the value is the element itself, appended to the shared list field.
		 *  Plain JSONL writes it as a single-key object. */
		final boolean polymorphic;
		/** 来自 {@code @XmlAttribute}。合并成一张键表后，`isList` 在属性上是「集合属性」
		 *  （@XmlList 要按空格切分）、在元素上是「列表字段」，必须靠这个标志区分。
		 *  From {@code @XmlAttribute}. With one merged key table, `isList` means "collection
		 *  attribute" (@XmlList splits on spaces) for attributes but "list field" for elements,
		 *  so this flag tells them apart. */
		final boolean attribute;

		static Accessor ofAttribute(String key, Field field, Class<?> valueType, Class<?> elementType,
				boolean isList, boolean xmlList) {
			return new Accessor(key, field, valueType, elementType, isList, xmlList, false, true);
		}

		static Accessor ofElement(String key, Field field, Class<?> valueType, Class<?> elementType, boolean isList) {
			return new Accessor(key, field, valueType, elementType, isList, false, false, false);
		}

		static Accessor ofPolymorphic(String key, Field field, Class<?> type) {
			return new Accessor(key, field, type, type, true, false, true, false);
		}

		private Accessor(String key, Field field, Class<?> valueType, Class<?> elementType, boolean isList,
				boolean xmlList, boolean polymorphic, boolean attribute) {
			this.key = key;
			this.keyHash = key.hashCode();
			this.field = field;
			this.valueType = valueType;
			this.elementType = elementType;
			this.isList = isList;
			this.xmlList = xmlList;
			this.polymorphic = polymorphic;
			this.attribute = attribute;
		}
	}

	// ------------------------------------------------------------------ JSON 数组解析 / JSON array scanning

	/**
	 * 紧凑 JSONL 的最小解析器：首行键表，数据行是 {@code [索引,"值",索引,"值",...]}。
	 * 值一律为字符串；索引为裸整数。
	 *
	 * Minimal parser for the compact JSONL: the first line holds the key table, data lines are
	 * {@code [index,"value",index,"value",...]} with string values and bare integer indices.
	 */
	static final class JsonArrays {
		private JsonArrays() {
		}

		static final class Row {
			int[] indexes = new int[256];
			String[] values = new String[256];

			void ensure(int n) {
				if (n > indexes.length) {
					int capacity = indexes.length * 2;
					indexes = Arrays.copyOf(indexes, capacity);
					values = Arrays.copyOf(values, capacity);
				}
			}
		}

		static String[] readKeys(String line) {
			// 形如 {"_keys":["@id","@name",...]} / shape: {"_keys":[...]}
			int start = line.indexOf('[');
			int end = line.lastIndexOf(']');
			if (start < 0 || end < start) {
				throw new IllegalStateException("bad key table line: " + line.substring(0, Math.min(80, line.length())));
			}
			List<String> keys = new ArrayList<>();
			int i = start + 1;
			while (i < end) {
				if (line.charAt(i) == '"') {
					StringBuilder builder = new StringBuilder();
					i = readString(line, i, builder, end);
					keys.add(builder.toString());
				} else {
					i++;
				}
			}
			return keys.toArray(new String[0]);
		}

		/** 解析一行数据，返回 (索引, 值) 对数量。 / Parses one data line and returns the pair count. */
		static int parseRow(String line, Row row) {
			int length = line.length();
			int i = 0;
			int n = 0;
			int pendingIndex = 0;
			while (i < length && line.charAt(i) != '[') {
				i++;
			}
			i++;
			while (i < length) {
				char c = line.charAt(i);
				if (c == ']') {
					break;
				}
				if (c == ',' || c == ' ') {
					i++;
					continue;
				}
				if (c == '"') {
					StringBuilder builder = new StringBuilder();
					i = readString(line, i, builder, length);
					row.ensure(n + 1);
					row.indexes[n] = pendingIndex;
					row.values[n] = builder.toString();
					n++;
				} else {
					int value = 0;
					int sign = 1;
					if (c == '-') {
						sign = -1;
						i++;
					}
					while (i < length) {
						char digit = line.charAt(i);
						if (digit < '0' || digit > '9') {
							break;
						}
						value = value * 10 + (digit - '0');
						i++;
					}
					pendingIndex = sign * value;
				}
			}
			return n;
		}

		private static int readString(String line, int quoteIndex, StringBuilder out, int limit) {
			int i = quoteIndex + 1;
			while (i < limit) {
				char c = line.charAt(i);
				if (c == '"') {
					return i + 1;
				}
				if (c == '\\' && i + 1 < limit) {
					char next = line.charAt(i + 1);
					switch (next) {
						case 'n' -> out.append('\n');
						case 't' -> out.append('\t');
						case 'r' -> out.append('\r');
						case '"' -> out.append('"');
						case '\\' -> out.append('\\');
						case '/' -> out.append('/');
						case 'u' -> {
							if (i + 5 < limit) {
								out.append((char) Integer.parseInt(line.substring(i + 2, i + 6), 16));
								i += 6;
								continue;
							}
							out.append(next);
						}
						default -> out.append(next);
					}
					i += 2;
					continue;
				}
				out.append(c);
				i++;
			}
			return i;
		}

		/** 从 `"_keys":["a","b"]` 之类的片段里取出全部字符串（首行一次性调用，非热点）。
		 *  Extracts every string from a fragment like {@code "_keys":["a","b"]} (header-only, not hot). */
		static String[] parseStringArray(String part) {
			int open = part.indexOf('[');
			int close = part.lastIndexOf(']');
			if (open < 0 || close < open) {
				return new String[0];
			}
			List<String> out = new ArrayList<>();
			int i = open + 1;
			while (i < close) {
				if (part.charAt(i) == '"') {
					StringBuilder builder = new StringBuilder();
					i = readString(part, i, builder, close);
					out.add(builder.toString());
				} else {
					i++;
				}
			}
			return out.toArray(new String[0]);
		}

		/** 解析一行「纯整数索引」数据，返回写入的整数个数（键索引与值索引交替）。
		 *  Parses one all-integer row and returns how many ints were written (key/value index alternating). */
		static int parseIndexRow(char[] text, int start, int end, int[] out) {
			int i = start;
			int n = 0;
			while (i < end && text[i] != '[') {
				i++;
			}
			i++;
			while (i < end && n < out.length) {
				char c = text[i];
				if (c == ']') {
					break;
				}
				if (c == ',' || c == ' ') {
					i++;
					continue;
				}
				boolean negative = c == '-';
				if (negative) {
					i++;
				}
				int value = 0;
				while (i < end) {
					char digit = text[i];
					if (digit < '0' || digit > '9') {
						break;
					}
					value = value * 10 + (digit - '0');
					i++;
				}
				out[n++] = negative ? -value : value;
			}
			return n;
		}

		static Object convert(String value, Class<?> type) {
			if (type == String.class || type == Object.class) {
				return value;
			}
			if (type == int.class || type == Integer.class) {
				return value.isEmpty() ? 0 : Integer.valueOf(value.trim());
			}
			if (type == long.class || type == Long.class) {
				return value.isEmpty() ? 0L : Long.valueOf(value.trim());
			}
			if (type == short.class || type == Short.class) {
				return value.isEmpty() ? (short) 0 : Short.valueOf(value.trim());
			}
			if (type == byte.class || type == Byte.class) {
				return value.isEmpty() ? (byte) 0 : Byte.valueOf(value.trim());
			}
			if (type == float.class || type == Float.class) {
				return value.isEmpty() ? 0f : Float.valueOf(value.trim());
			}
			if (type == double.class || type == Double.class) {
				return value.isEmpty() ? 0d : Double.valueOf(value.trim());
			}
			if (type == boolean.class || type == Boolean.class) {
				return Boolean.valueOf(value.trim());
			}
			if (type == char.class || type == Character.class) {
				return value.isEmpty() ? (char) 0 : value.charAt(0);
			}
			if (type.isEnum()) {
				try {
					@SuppressWarnings({"unchecked", "rawtypes"})
					Object constant = Enum.valueOf((Class<? extends Enum>) type, value);
					return constant;
				} catch (IllegalArgumentException e) {
					// 复刻 JAXB 的容错：数据中存在模型枚举没有的取值（如 armor_type="ARROW"）时，
					// JAXB 不报错、字段保持 null。自写绑定器必须照做，否则该分片会直接中断加载。
					// Mirrors JAXB tolerance: when data carries an enum constant the model lacks
					// (e.g. armor_type="ARROW"), JAXB leaves the field null instead of failing.
					return null;
				}
			}
			return value;
		}
	}

	// ------------------------------------------------------------------ 测量框架 / measurement harness

	// ------------------------------------------------------------------ 标准 JSONL / plain JSONL

	/**
	 * 标准 JSONL 的流式读取器：直接在行字符串上扫描，不建任何中间树。
	 *
	 * 无转义的字符串走 `substring` 快路径，只有真正出现 `\` 才进入逐字符折叠——数据里
	 * 绝大多数值都不含转义，这条分支决定了解析层的成本。
	 *
	 * Streaming reader for plain JSONL: scans the line string in place and builds no tree.
	 * Strings without escapes take a `substring` fast path; only an actual `\` enters the
	 * character-by-character fold, and most values carry no escape, so this branch sets the cost
	 * of the lexical layer.
	 */
	static final class JsonReader {
		private String text;
		private int pos;
		private int end;

		void reset(String text) {
			this.text = text;
			this.pos = 0;
			this.end = text.length();
		}

		private void skipWs() {
			while (pos < end) {
				char c = text.charAt(pos);
				if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
					return;
				}
				pos++;
			}
		}

		char peek() {
			skipWs();
			return pos < end ? text.charAt(pos) : '\0';
		}

		void expect(char c) {
			skipWs();
			if (pos >= end || text.charAt(pos) != c) {
				throw new IllegalStateException("expected '" + c + "' at " + pos + " in " + text);
			}
			pos++;
		}

		/** 键名在行内的区间与哈希，由 {@link #readKeyRegion()} 填写。
		 *  The key's region in the line and its hash, filled by {@link #readKeyRegion()}. */
		int keyStart;
		int keyEnd;
		int keyHash;
		/** 键名里出现了反斜杠转义——区间法读不了，调用方必须回退到 String 路径。
		 *  The key carried a backslash escape, unreadable as a region; the caller must fall back. */
		boolean keyEscaped;

		/**
		 * 读键名，把「字符区间 + 哈希」留在字段里，**不建 String**，随后跳过冒号。
		 *
		 * 哈希与 {@link String#hashCode()} 同算法（`h = 31h + c`），因此可直接与
		 * {@link Accessor#keyHash} 比较；而且这一步顺带完成了扫描，不像 `String.hashCode()`
		 * 那样要再遍历一遍字符。
		 *
		 * Reads the key, leaving its region and hash in fields and skipping the colon —
		 * **without building a String**. The hash follows {@link String#hashCode()} (`h = 31h + c`)
		 * so it compares directly against {@link Accessor#keyHash}, and it falls out of the scan
		 * instead of costing a second pass the way `String.hashCode()` would.
		 */
		void readKeyRegion() {
			skipWs();
			if (pos >= end || text.charAt(pos) != '"') {
				throw new IllegalStateException("expected key at " + pos + " in " + text);
			}
			int start = ++pos;
			int hash = 0;
			keyEscaped = false;
			while (pos < end) {
				char c = text.charAt(pos);
				if (c == '"') {
					keyStart = start;
					keyEnd = pos;
					keyHash = hash;
					pos++;
					break;
				}
				if (c == '\\') {
					keyEscaped = true;
				}
				hash = hash * 31 + c;
				pos++;
			}
			skipWs();
			if (pos < end && text.charAt(pos) == ':') {
				pos++;
			}
		}

		/** 键名的 String 形态，仅转义回退路径使用。 / The key as a String, only for the escape fallback. */
		String keyText() {
			return text.substring(keyStart, keyEnd);
		}

		/** 键名区间内第 offset 个字符，供查表做逐字符比对。
		 *  The offset-th character of the key region, for the table's character-by-character compare. */
		char keyCharAt(int offset) {
			return text.charAt(keyStart + offset);
		}

		/** 读整数：直接在字符上累加，不建 String。 / Reads an integer over the characters, building no String. */
		int readInt() {
			boolean negative = false;
			if (pos < end && text.charAt(pos) == '-') {
				negative = true;
				pos++;
			}
			int value = 0;
			while (pos < end) {
				char c = text.charAt(pos);
				if (c < '0' || c > '9') {
					break;
				}
				value = value * 10 + (c - '0');
				pos++;
			}
			return negative ? -value : value;
		}

		long readLong() {
			boolean negative = false;
			if (pos < end && text.charAt(pos) == '-') {
				negative = true;
				pos++;
			}
			long value = 0L;
			while (pos < end) {
				char c = text.charAt(pos);
				if (c < '0' || c > '9') {
					break;
				}
				value = value * 10 + (c - '0');
				pos++;
			}
			return negative ? -value : value;
		}

		/** 读 true/false；其余字面量回退到标量扫描，与 `Boolean.valueOf` 语义一致。
		 *  Reads true/false; other literals fall back to the scalar scan, matching `Boolean.valueOf`. */
		boolean readBoolean() {
			if (pos + 4 <= end && text.startsWith("true", pos)) {
				pos += 4;
				return true;
			}
			if (pos + 5 <= end && text.startsWith("false", pos)) {
				pos += 5;
				return false;
			}
			return Boolean.parseBoolean(readScalar());
		}

		/**
		 * 读一个标量并转成目标类型：数字与布尔走无 String 的快路径，其余建 String 后交给
		 * {@link JsonArrays#convert}（枚举、String、char 等）。
		 *
		 * Reads a scalar into the target type: numbers and booleans take the no-String fast path,
		 * everything else builds a String for {@link JsonArrays#convert}.
		 */
		Object readValue(Class<?> type) {
			skipWs();
			char c = pos < end ? text.charAt(pos) : '\0';
			if (c == '"') {
				return JsonArrays.convert(readString(), type);
			}
			if (type == int.class || type == Integer.class) {
				return readInt();
			}
			if (type == long.class || type == Long.class) {
				return readLong();
			}
			if (type == short.class || type == Short.class) {
				return (short) readInt();
			}
			if (type == byte.class || type == Byte.class) {
				return (byte) readInt();
			}
			if (type == boolean.class || type == Boolean.class) {
				return readBoolean();
			}
			return JsonArrays.convert(readScalar(), type);
		}

		String readString() {
			skipWs();
			if (pos >= end || text.charAt(pos) != '"') {
				throw new IllegalStateException("expected string at " + pos + " in " + text);
			}
			int start = ++pos;
			for (int i = start; i < end; i++) {
				char c = text.charAt(i);
				if (c == '"') {
					pos = i + 1;
					return text.substring(start, i);
				}
				if (c == '\\') {
					return readEscaped(start, i);
				}
			}
			throw new IllegalStateException("unterminated string in " + text);
		}

		private String readEscaped(int start, int scanFrom) {
			StringBuilder out = new StringBuilder(32).append(text, start, scanFrom);
			int i = scanFrom;
			while (i < end) {
				char c = text.charAt(i);
				if (c == '"') {
					pos = i + 1;
					return out.toString();
				}
				if (c != '\\') {
					out.append(c);
					i++;
					continue;
				}
				i++;
				if (i >= end) {
					break;
				}
				char esc = text.charAt(i);
				switch (esc) {
					case 'n' -> out.append('\n');
					case 't' -> out.append('\t');
					case 'r' -> out.append('\r');
					case 'b' -> out.append('\b');
					case 'f' -> out.append('\f');
					case 'u' -> {
						out.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
						i += 4;
					}
					default -> out.append(esc);
				}
				i++;
			}
			throw new IllegalStateException("unterminated escape in " + text);
		}

		/** 读标量（数字 / true / false / null），一律返回字符串交给类型转换。
		 *  Reads a scalar (number / true / false / null) as a String for later conversion. */
		String readScalar() {
			skipWs();
			if (pos < end && text.charAt(pos) == '"') {
				return readString();
			}
			int start = pos;
			while (pos < end) {
				char c = text.charAt(pos);
				if (c == ',' || c == '}' || c == ']' || c == ' ' || c == '\n' || c == '\r' || c == '\t') {
					break;
				}
				pos++;
			}
			return text.substring(start, pos);
		}

		/** 跳过一个值，嵌套对象/数组一并跳过。 / Skips one value, nested objects and arrays included. */
		void skipValue() {
			char c = peek();
			if (c != '{' && c != '[') {
				readScalar();
				return;
			}
			int depth = 0;
			while (pos < end) {
				char ch = text.charAt(pos);
				if (ch == '"') {
					readString();
					continue;
				}
				pos++;
				if (ch == '{' || ch == '[') {
					depth++;
				} else if (ch == '}' || ch == ']') {
					depth--;
					if (depth == 0) {
						return;
					}
				}
			}
		}
	}

	/**
	 * 标准 JSONL 的递归绑定器：按键名沿**当前对象的类型**查模型元数据，边解析边构造对象，
	 * 不建中间 JSON 树。
	 *
	 * 与紧凑路径的关键差别：紧凑 JSONL 的键是全局路径（`/modifiers/add@bonus`），预编译成
	 * 一条完整的 accessor 链，绑定期零查表；标准 JSONL 只有裸键名，必须逐层按类型查表并
	 * 递归下降，因此每层多一次哈希查找——这正是本次要测的成本。
	 *
	 * 容器塌缩（`"modifiers":[{"add":{...}}]`）对应 `gen_plain_jsonl.py` 省略中间层的规则：
	 * 字段类型本身没有属性、只有一个列表字段时，数组直接装进那个列表。多态列表的元素带
	 * 标签包装（多个名称共享同一列表字段），单一 @XmlElement 列表的元素不带标签，用
	 * `elements.size() >= 2` 区分。
	 *
	 * Recursive binder for plain JSONL: looks fields up by key against the *current object's*
	 * type and builds objects while parsing, keeping no intermediate JSON tree.
	 *
	 * The key difference from the compact path — whose global paths
	 * (`/modifiers/add@bonus`) precompile into one accessor chain with no lookup at bind time — is
	 * that plain JSONL carries bare keys, so every level costs a hash lookup plus a recursion
	 * step. That extra cost is exactly what this arm measures.
	 *
	 * Container collapse (`"modifiers":[{"add":{...}}]`) mirrors the middle-layer elision rule of
	 * `gen_plain_jsonl.py`: when the field's type has no attributes and a single list field, the
	 * array goes straight into that list. Polymorphic list elements carry a tag wrapper (several
	 * names share one list field); elements of a plain @XmlElement list do not. The two are told
	 * apart by `elements.size() >= 2`.
	 */
	static final class PlainBinder {
		private final Map<Class<?>, ClassMeta> classCache = new HashMap<>();
		/** 复用 Binder 的 restrict 缓存与 afterUnmarshal 收尾语义。
		 *  Reuses Binder's restrict cache and afterUnmarshal tail semantics. */
		private final Binder tail = new Binder();
		private final JsonReader reader = new JsonReader();

		ItemTemplate bindLine(String line) {
			reader.reset(line);
			ItemTemplate template = new ItemTemplate();
			fill(template);
			tail.finish(template);
			return template;
		}

		private ClassMeta metaOf(Class<?> type) {
			return classCache.computeIfAbsent(type, Binder::buildMeta);
		}

		/** 读一个 {@code {...}} 并按模型填进 obj。 / Reads one {@code {...}} into obj following the model. */
		private void fill(Object obj) {
			ClassMeta meta = metaOf(obj.getClass());
			reader.expect('{');
			while (true) {
				char c = reader.peek();
				if (c == '}') {
					reader.expect('}');
					return;
				}
				if (c == ',') {
					reader.expect(',');
					continue;
				}
				// 键名不建 String：扫描时顺带算出哈希，直接在合并表里查（走字符区间比对）。
				// No key String: the hash falls out of the scan and the merged table is probed on
				// the raw character region.
				reader.readKeyRegion();
				Accessor accessor = meta.byKey(reader);
				if (accessor == null && reader.keyEscaped) {
					String key = reader.keyText();
					accessor = meta.attributes.get(key);
					if (accessor == null) {
						accessor = meta.elements.get(key);
					}
				}
				if (accessor == null) {
					// 数据比模型新：元素存在但这个键在模型里没有字段，JAXB 同样忽略。
					// Data newer than the model: the element exists but this key has no field;
					// JAXB drops it too.
					reader.skipValue();
					continue;
				}
				try {
					if (accessor.attribute) {
						applyAttribute(obj, accessor);
						continue;
					}
					if (accessor.polymorphic) {
						Object item = newInstance(accessor.elementType);
						fill(item);
						append(obj, accessor, item);
						continue;
					}
					if (accessor.isList) {
						accessor.field.set(obj, readUntaggedList(accessor));
						continue;
					}
					if (reader.peek() == '[') {
						accessor.field.set(obj, readCollapsed(accessor));
						continue;
					}
					Object child = newInstance(accessor.valueType);
					fill(child);
					accessor.field.set(obj, child);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("cannot bind key " + accessor.key, e);
				}
			}
		}

		/**
		 * 设置一个属性：基础类型走无 String、无装箱的快路径（`Field.setInt` 等直接写入），
		 * 其余交给 {@link Binder#applyAttribute}。
		 *
		 * 装箱值得单独处理：`id=100001266` 这类值超出 `Integer` 缓存范围（-128..127），
		 * `field.set(obj, Integer)` 每条记录都要新建一个 Integer 对象。
		 *
		 * Sets an attribute: primitives take the no-String, no-boxing fast path (`Field.setInt`
		 * and friends write directly), everything else goes through {@link Binder#applyAttribute}.
		 *
		 * Boxing is worth its own branch: values like `id=100001266` fall outside the `Integer`
		 * cache (-128..127), so `field.set(obj, Integer)` allocates one Integer per record.
		 */
		private void applyAttribute(Object obj, Accessor accessor) throws IllegalAccessException {
			Field field = accessor.field;
			Class<?> type = accessor.valueType;
			if (accessor.isList) {
				// 集合属性（@XmlList 按空格切分）必须拿字符串，维持原路径。
				// Collection attributes (@XmlList splits on spaces) need the String; keep the old path.
				Binder.applyAttribute(obj, accessor, reader.readString());
				return;
			}
			// 数据里存在以字符串形式书写的数字（`"04450"` 这类带前导零的 id，全量 8 处）——
			// 生成器出于安全故意保留其为字符串，因此裸数字快路径只在真正的数字字面量上成立，
			// 这里必须先分辨。曾因省略这一步，把 8 处 questid 静默读成 0。
			// The data carries numbers written as strings (zero-padded ids like `"04450"`, 8 across
			// the dataset) — the generator keeps them as strings on purpose, so the bare-number fast
			// path holds only for real number literals and needs this check first. Omitting it once
			// read 8 questid values as 0, silently.
			if (reader.peek() == '"') {
				field.set(obj, reader.readValue(type));
				return;
			}
			if (type == int.class) {
				field.setInt(obj, reader.readInt());
			} else if (type == long.class) {
				field.setLong(obj, reader.readLong());
			} else if (type == boolean.class) {
				field.setBoolean(obj, reader.readBoolean());
			} else if (type == short.class) {
				field.setShort(obj, (short) reader.readInt());
			} else if (type == byte.class) {
				field.setByte(obj, (byte) reader.readInt());
			} else {
				field.set(obj, reader.readValue(type));
			}
		}

		/** 非多态列表字段：{@code "tradein_item":[{...},{...}]}，元素不带标签。
		 *  Non-polymorphic list field: {@code "tradein_item":[{...},{...}]}, elements untagged. */
		private List<Object> readUntaggedList(Accessor accessor) throws ReflectiveOperationException {
			List<Object> list = new ArrayList<>(2);
			reader.expect('[');
			while (true) {
				char c = reader.peek();
				if (c == ']') {
					reader.expect(']');
					return list;
				}
				if (c == ',') {
					reader.expect(',');
					continue;
				}
				Object item = newInstance(accessor.elementType);
				fill(item);
				list.add(item);
			}
		}

		/**
		 * 容器塌缩：字段值直接是数组，元素装进 valueType 唯一的列表字段。
		 * 多态容器（`ModifiersTemplate` / `ItemActions`）的元素带标签包装，单一列表容器
		 * （`tradein_list`）的不带。
		 *
		 * Container collapse: the field's value is the array itself and its elements go into the
		 * value type's sole list field. Elements of a polymorphic container
		 * (`ModifiersTemplate` / `ItemActions`) carry a tag wrapper; those of a single-list
		 * container (`tradein_list`) do not.
		 */
		private Object readCollapsed(Accessor accessor) throws ReflectiveOperationException {
			ClassMeta meta = metaOf(accessor.valueType);
			Accessor inner = soleListAccessor(meta);
			boolean tagged = meta.elements.size() >= 2;
			Object container = newInstance(accessor.valueType);
			List<Object> list = new ArrayList<>(4);
			reader.expect('[');
			while (true) {
				char c = reader.peek();
				if (c == ']') {
					reader.expect(']');
					break;
				}
				if (c == ',') {
					reader.expect(',');
					continue;
				}
				Accessor itemAccessor = inner;
				if (tagged) {
					reader.expect('{');
					// 多态元素名同样走区间查表（容器类没有属性，合并表等价于原 elements 表）。
					// The polymorphic element name takes the same region lookup (a container class has
					// no attributes, so the merged table equals the elements table).
					reader.readKeyRegion();
					itemAccessor = meta.byKey(reader);
					if (itemAccessor == null) {
						reader.skipValue();
						reader.expect('}');
						continue;
					}
				}
				Object item = newInstance(itemAccessor.elementType);
				fill(item);
				if (tagged) {
					reader.expect('}');
				}
				list.add(item);
			}
			inner.field.set(container, list);
			return container;
		}

		/** 容器类里唯一的列表字段；多态列表的多个名称共享同一个字段，因此必须比对 field 本身。
		 *  The sole list field of a container class; a polymorphic list's several names share one
		 *  field, so the comparison is on the field, not the accessor. */
		private static Accessor soleListAccessor(ClassMeta meta) {
			Accessor found = null;
			for (Accessor candidate : meta.elements.values()) {
				if (!List.class.isAssignableFrom(candidate.field.getType())) {
					continue;
				}
				if (found == null) {
					found = candidate;
				} else if (found.field != candidate.field) {
					throw new IllegalStateException("ambiguous list field: " + candidate.field);
				}
			}
			if (found == null) {
				throw new IllegalStateException("no list field to collapse into");
			}
			return found;
		}

		@SuppressWarnings("unchecked")
		private static void append(Object owner, Accessor accessor, Object item) throws IllegalAccessException {
			List<Object> list = (List<Object>) accessor.field.get(owner);
			if (list == null) {
				list = new ArrayList<>(4);
				accessor.field.set(owner, list);
			}
			list.add(item);
		}

		private static Object newInstance(Class<?> type) throws ReflectiveOperationException {
			return type.getDeclaredConstructor().newInstance();
		}
	}

	/** 标准 JSONL 全流程：解析 + 绑定 + afterUnmarshal 收尾。 / Plain JSONL end to end. */
	static List<ItemTemplate> bindAllPlain(File file) throws Exception {
		PlainBinder binder = new PlainBinder();
		List<ItemTemplate> templates = new ArrayList<>(16384);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty()) {
					templates.add(binder.bindLine(line));
				}
			}
		}
		return templates;
	}

	private static long jsonlPlainBind(File file) throws Exception {
		List<ItemTemplate> templates = bindAllPlain(file);
		Map<Integer, ItemTemplate> byId = new HashMap<>(templates.size() * 2);
		Map<String, ItemTemplate> byName = new HashMap<>(templates.size() * 2);
		for (ItemTemplate template : templates) {
			byId.put(template.getTemplateId(), template);
			String name = template.getName();
			if (name != null && !name.isBlank()) {
				byName.putIfAbsent(name.toLowerCase(Locale.ROOT), template);
			}
		}
		return byId.size() + byName.size();
	}

	/** 只走标准 JSONL 的词法层：扫行内的字符串与标量，不构造任何对象（解析层下限）。
	 *  Lexical layer only: scans strings and scalars, constructing nothing (the parse floor). */
	private static long jsonlPlainParse(File file) throws Exception {
		long scalars = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String line;
			while ((line = reader.readLine()) != null) {
				int limit = line.length();
				for (int i = 0; i < limit; i++) {
					if (line.charAt(i) != '"') {
						continue;
					}
					scalars++;
					i++;
					while (i < limit && line.charAt(i) != '"') {
						if (line.charAt(i) == '\\') {
							i++;
						}
						i++;
					}
				}
			}
		}
		return scalars;
	}

	private static long batchJsonlPlain(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		long total = 0;
		for (File shard : shards) {
			total += bindAllPlain(shard).size();
		}
		return total;
	}

	private static long parallelJsonlPlain(File dir) throws Exception {
		File[] shards = shardsOf(dir, ".jsonl");
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(POOL_SIZE);
		try {
			List<java.util.concurrent.Future<long[]>> futures = new ArrayList<>(shards.length);
			for (File shard : shards) {
				futures.add(pool.submit(() -> {
					ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
					long allocStart = thread.getCurrentThreadAllocatedBytes();
					int count = bindAllPlain(shard).size();
					return new long[] {count, thread.getCurrentThreadAllocatedBytes() - allocStart};
				}));
			}
			long allocated = 0;
			for (java.util.concurrent.Future<long[]> future : futures) {
				allocated += future.get()[1];
			}
			return allocated;
		} finally {
			pool.shutdown();
		}
	}

	private record Stats(long wallNanos, long cpuNanos, long allocatedBytes) {
	}

	private interface Op {
		Object run() throws Exception;
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

	private static double median(double[] values) {
		double[] copy = values.clone();
		Arrays.sort(copy);
		int size = copy.length;
		return size % 2 == 1 ? copy[size / 2] : (copy[size / 2 - 1] + copy[size / 2]) / 2.0;
	}

	private static double minimum(double[] values) {
		double min = Double.MAX_VALUE;
		for (double value : values) {
			min = Math.min(min, value);
		}
		return min;
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
}
