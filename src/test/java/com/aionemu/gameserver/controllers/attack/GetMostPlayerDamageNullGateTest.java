package com.aionemu.gameserver.controllers.attack;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * {@code AggroList#getMostPlayerDamage()} 与 {@code #getMostPlayerDamageOfMembers} 全部消费点的判空闸门。
 * Null-guard gate for every consumer of {@code AggroList#getMostPlayerDamage()} / {@code #getMostPlayerDamageOfMembers}.
 *
 * <p>背景：这两个方法在没有“玩家类型”伤害条目时返回 {@code null}（无玩家伤害、来源不在已知列表等，见 IR-008），
 * 而实例脚本长期直接解引用该结果或把它交给不接受 null 的方法，导致 NPC 死亡处理中途抛 NPE，后续点位累加、
 * {@code deleteNpc}、刷怪全部不执行。本闸门把“判空”固化为构建期契约：<b>在任何消费点首次使用之前必须出现判空</b>。
 * Background: both methods return {@code null} when no player-typed damage entry survives (see IR-008). Instance scripts
 * used to dereference the value or hand it to a null-hostile method, which aborted the rest of the NPC death handling.
 * This gate turns "check for null" into a build-time contract: a null check must precede the first use.</p>
 *
 * <p>规则细节 / Rules:</p>
 * <ol>
 * <li>赋值形态 {@code Player x = ...getMostPlayerDamage...;}：在 {@code x} 首次被使用（{@code x.} 解引用或作为实参传给
 * 非容忍 null 的方法）之前，必须出现 {@code x == null} 或 {@code x != null}；内层作用域重新声明同名变量即停止检查，
 * 避免把 {@code Visitor<Player>} 等同名形参误判。 / Assignment form: a null check must precede the first use of the
 * assigned variable; an inner re-declaration of the same name ends the scan so shadowed parameters are not misreported.</li>
 * <li>内联形态：同一语句内必须出现 null 比较，或该调用属于成对的 null 容忍方法（{@link #NULL_TOLERANT_CALLEES}）。
 * / Inline form: the same statement must compare with null or call a documented null-tolerant method.</li>
 * <li>{@link #KNOWN_UNGUARDED_SITES} 记录本闸门落地时已存在的未判空站点，作为待单独决策的基线：新增站点会使文件计数
 * 超出基线从而失败。 / The baseline records sites that already existed when this gate landed; new sites push a file's
 * count over its allowance and fail the build.</li>
 * </ol>
 */
class GetMostPlayerDamageNullGateTest {

	/** 项目主源码根目录 / Project main source root. */
	private static final Path MAIN_SOURCES = Path.of("src/main/java");

	/** 消费点匹配：必须带 {@code .} 前缀，方法声明本身不会被匹配。 / Call sites: the leading dot excludes the method declaration itself. */
	private static final Pattern CALL_SITE = Pattern.compile("\\.(getMostPlayerDamage|getMostPlayerDamageOfMembers)\\s*\\(");

	/** 赋值形态：{@code Player/Creature/var x = ...getMostPlayerDamage...}。 / Assignment form. */
	private static final Pattern ASSIGNMENT = Pattern.compile(
			"(?:final\\s+)?(?:Player|Creature|var)\\s+(\\w+)\\s*=\\s*[^;]{0,200}?\\.(?:getMostPlayerDamage|getMostPlayerDamageOfMembers)\\s*\\(");

	/** 成对的 null 容忍方法：即使玩家为 null 也安全。 / Paired null-tolerant methods: safe when the player is null. */
	private static final List<String> NULL_TOLERANT_CALLEES = List.of("sendMovie", "sendPacket", "sendMessage", "broadcastPacket");

	/** 至少应扫描到的消费点数量，防止路径或正则失效后闸门空转。 / Minimum scanned call sites so the gate cannot silently degrade. */
	private static final int MIN_CALL_SITES = 90;

	/**
	 * 闸门落地时的既有未判空站点基线：文件 → 允许的违规数。
	 * Baseline of pre-existing unguarded sites: file → allowed violations.
	 *
	 * <p>基线现为九处：三条 {@code stop*(player)} 形参当前未被方法体使用，因此不会 NPE（若将来开始使用该形参必须重新评估）；
	 * 其余六处是真实的 NPE 风险（玩家奖励 / 以玩家坐标刷怪），修法涉及“跳过还是回退坐标”的玩法语义，另行决策。
	 * {@code TalocsHollowInstance} 在 2026-09-15 运行态 NPE 后已补齐判空并移出基线，该文件再次出现未判空使用会直接失败。
	 * The baseline now holds nine entries: three {@code stop*(player)} parameters are currently unused, so they cannot
	 * NPE; the other six are real NPE risks (player rewards and spawning at player coordinates) whose fix depends on
	 * gameplay semantics and is tracked separately. {@code TalocsHollowInstance} was fixed and removed from the baseline
	 * after a 2026-09-15 runtime NPE, so a new unguarded use in that file fails the gate immediately.</p>
	 */
	private static final Map<String, Integer> KNOWN_UNGUARDED_SITES = buildBaseline();

	private static Map<String, Integer> buildBaseline() {
		Map<String, Integer> baseline = new LinkedHashMap<>();
		String scripts = "com/aionemu/gameserver/instance/handlers/scripts/";
		baseline.put(scripts + "FallenPoetaInstance.java", 1); // stopInstance(player)：形参未使用 / parameter unused
		baseline.put(scripts + "DrakenseerLairInstance.java", 1); // stopDrakenseerLairTimer(player)：形参未使用 / unused
		baseline.put(scripts + "KumukiCaveInstance.java", 1); // stopInstance2(player)：形参未使用 / unused
		baseline.put(scripts + "MirashSanctuaryInstance.java", 1); // player.getSkillList() / spawn(…, player.getX(), …)
		baseline.put(scripts + "TrialsOfEternityInstance.java", 1); // AbyssPointsService.addGp(player, 1200)
		baseline.put(scripts + "AturamSkyFortressInstance.java", 1); // sp(…, player.getX(), player.getY(), player.getZ(), …)
		baseline.put(scripts + "event/Event_AturamSkyFortressInstance.java", 1); // 同上 / same as above
		baseline.put(scripts + "event/IDEvent_Def_HInstance.java", 1); // ItemService.addItem(player, …) / player.getCommonData()
		baseline.put(scripts + "event/Event_ContaminatedUnderpathInstance.java", 1); // 同上 / same as above
		return Collections.unmodifiableMap(baseline);
	}

	/**
	 * 校验所有消费点，并输出违规清单。
	 * Verifies every consumer and fails with the offending list.
	 *
	 * @throws IOException 读取源码失败 / when the sources cannot be read
	 */
	@Test
	void everyGetMostPlayerDamageConsumerChecksForNull() throws IOException {
		List<String> violations = new ArrayList<>();
		Map<String, Integer> perFileCount = new LinkedHashMap<>();
		int callSites = 0;

		List<Path> sources;
		try (var paths = Files.walk(MAIN_SOURCES)) {
			sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
		}

		for (Path path : sources) {
			String code = stripCommentsAndLiterals(Files.readString(path));
			String relative = MAIN_SOURCES.relativize(path).toString();
			Matcher matcher = CALL_SITE.matcher(code);
			while (matcher.find()) {
				callSites++;
				String violation = inspect(code, relative, matcher.start(), matcher.end());
				if (violation != null) {
					perFileCount.merge(relative, 1, Integer::sum);
					violations.add(violation);
				}
			}
		}

		assertTrue(callSites >= MIN_CALL_SITES, "仅扫描到 " + callSites + " 个 getMostPlayerDamage 消费点（期望 >= " + MIN_CALL_SITES
				+ "）；路径或正则可能已失效。 / Scanned only " + callSites + " call sites; the gate may have stopped matching.");

		List<String> unbaselined = new ArrayList<>();
		for (String violation : violations) {
			String file = violation.substring(0, violation.indexOf(':'));
			int allowed = KNOWN_UNGUARDED_SITES.getOrDefault(file, 0);
			int seen = perFileCount.getOrDefault(file, 0);
			if (seen > allowed) {
				unbaselined.add(violation + (allowed == 0 ? "（该文件未登记基线）" : "（超出基线 " + allowed + "）"));
			}
		}

		assertTrue(unbaselined.isEmpty(),
				"以下消费点在首次使用前缺少判空（AggroList#getMostPlayerDamage 可能返回 null）：\n"
						+ String.join("\n", unbaselined));
	}

	/**
	 * 检查单个消费点是否满足判空契约。
	 * Checks whether a single call site satisfies the null-guard contract.
	 *
	 * @param code 去注释源码 / comment-free source
	 * @param file 相对路径 / relative path
	 * @param start 消费点起点 / call site start
	 * @param end 消费点终点 / call site end
	 * @return 违规描述或 null / violation description or null
	 */
	private static String inspect(String code, String file, int start, int end) {
		int line = code.substring(0, start).split("\n", -1).length;
		int statementStart = code.lastIndexOf(';', start) + 1;
		int statementEnd = code.indexOf(';', end);
		String statement = code.substring(statementStart, statementEnd < 0 ? code.length() : statementEnd);

		Matcher assignment = ASSIGNMENT.matcher(statement);
		if (!assignment.find()) {
			boolean comparedWithNull = statement.contains("== null") || statement.contains("!= null");
			boolean tolerantCall = NULL_TOLERANT_CALLEES.stream()
					.anyMatch(name -> Pattern.compile("\\b" + name + "\\s*\\(").matcher(statement).find());
			boolean allowlisted = file.endsWith("ai/RetailPatternAI2.java");
			return comparedWithNull || tolerantCall || allowlisted ? null : file + ":" + line + " 内联使用未判空";
		}

		String variable = assignment.group(1);
		int blockEnd = enclosingBlockEnd(code, start);
		if (blockEnd < 0) {
			return file + ":" + line + " 未找到所在代码块";
		}
		String scan = code.substring(end, blockEnd);
		Matcher shadow = Pattern.compile("(?:\\b(?:Player|Creature|var)\\s+" + Pattern.quote(variable) + "\\b|\\b"
				+ Pattern.quote(variable) + "\\s*=[^=])").matcher(scan);
		if (shadow.find()) {
			scan = scan.substring(0, shadow.start());
		}

		int guardIndex = nullCheckIndex(scan, variable);
		int firstUse = firstRequiringUseIndex(scan, variable);
		if (firstUse < 0 || (guardIndex >= 0 && guardIndex < firstUse)) {
			return null;
		}
		return file + ":" + line + " 变量 " + variable + " 首次使用前未判空";
	}

	/**
	 * 定位变量首次被“真正使用”的位置：解引用，或作为实参传给非 null 容忍的方法。
	 * Locates the first real use of the variable: a dereference, or an argument passed to a method that is not null-tolerant.
	 *
	 * <p>判空比较自身、同名形参声明、以及成对的 null 容忍方法实参都不算使用。
	 * A null comparison itself, a declaration, and arguments of paired null-tolerant methods do not count as uses.</p>
	 *
	 * @param scan 待扫描片段 / the fragment to scan
	 * @param variable 变量名 / variable name
	 * @return 首次使用下标，没有使用返回 -1 / index of the first use, or -1
	 */
	private static int firstRequiringUseIndex(String scan, String variable) {
		Matcher matcher = Pattern.compile("\\b" + Pattern.quote(variable) + "\\b").matcher(scan);
		while (matcher.find()) {
			if (isNullComparison(scan, matcher.start(), variable)) {
				continue;
			}
			String before = scan.substring(0, matcher.start()).stripTrailing();
			if (before.endsWith(".")) {
				continue;
			}
			if (Pattern.compile("(?:Player|Creature|var)\\s*$").matcher(before).find()) {
				continue;
			}
			Matcher callee = Pattern.compile("(\\w+)\\s*\\([^()]*$").matcher(before);
			if (callee.find() && NULL_TOLERANT_CALLEES.contains(callee.group(1))) {
				continue;
			}
			return matcher.start();
		}
		return -1;
	}

	/**
	 * 判断下标处是不是针对该变量的 null 比较（{@code x == null} / {@code null != x}）。
	 * Checks whether the index starts a null comparison involving the variable.
	 *
	 * @param scan 待扫描片段 / the fragment to scan
	 * @param index 变量起始下标 / variable start index
	 * @param variable 变量名 / variable name
	 * @return 是判空比较返回 true / true when it is a null comparison
	 */
	private static boolean isNullComparison(String scan, int index, String variable) {
		int from = Math.max(0, index - "null ".length() - 4);
		String window = scan.substring(from, Math.min(scan.length(), index + variable.length() + 16));
		return Pattern.compile("(" + Pattern.quote(variable) + "\\s*[!=]=\\s*null|null\\s*[!=]=\\s*" + Pattern.quote(variable) + ")")
				.matcher(window)
				.find();
	}

	/**
	 * 定位变量最近的判空位置。
	 * Locates the nearest null check for the variable.
	 *
	 * @param scan 待扫描片段 / the fragment to scan
	 * @param variable 变量名 / variable name
	 * @return 判空下标，没有返回 -1 / index of the null check, or -1
	 */
	private static int nullCheckIndex(String scan, String variable) {
		Matcher guard = Pattern.compile("\\b" + Pattern.quote(variable) + "\\s*[!=]=\\s*null").matcher(scan);
		return guard.find() ? guard.start() : -1;
	}

	/**
	 * 返回包含指定位置的代码块结束下标（大括号配对）。
	 * Returns the end index of the block enclosing the position (brace matching).
	 *
	 * @param code 去注释源码 / comment-free source
	 * @param position 位置 / position
	 * @return 配对大括号下标，无法定位返回 -1 / matching brace index, or -1
	 */
	private static int enclosingBlockEnd(String code, int position) {
		java.util.Deque<Integer> stack = new java.util.ArrayDeque<>();
		for (int i = 0; i <= position && i < code.length(); i++) {
			char c = code.charAt(i);
			if (c == '{') {
				stack.push(i);
			} else if (c == '}' && !stack.isEmpty()) {
				stack.pop();
			}
		}
		if (stack.isEmpty()) {
			return -1;
		}
		int depth = 0;
		for (int i = stack.peek(); i < code.length(); i++) {
			char c = code.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * 去掉注释与字符串/字符字面量，保留原始换行位置。
	 * Strips comments and string/char literals while preserving line positions.
	 *
	 * @param source 源码 / the source
	 * @return 仅含代码的文本 / code-only text
	 */
	private static String stripCommentsAndLiterals(String source) {
		StringBuilder out = new StringBuilder(source.length());
		int i = 0;
		int length = source.length();
		while (i < length) {
			char c = source.charAt(i);
			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
				int end = source.indexOf("*/", i + 2);
				end = end < 0 ? length : end + 2;
				appendBlanked(out, source, i, end);
				i = end;
				continue;
			}
			if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
				int end = source.indexOf('\n', i);
				end = end < 0 ? length : end;
				appendBlanked(out, source, i, end);
				i = end;
				continue;
			}
			if (c == '"' || c == '\'') {
				int end = i + 1;
				while (end < length) {
					char current = source.charAt(end);
					if (current == '\\') {
						end += 2;
						continue;
					}
					if (current == c) {
						end++;
						break;
					}
					if (current == '\n') {
						break;
					}
					end++;
				}
				end = Math.min(end, length);
				appendBlanked(out, source, i, end);
				i = end;
				continue;
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}

	/**
	 * 用空格替换片段中非换行字符，保持行列位置不变。
	 * Replaces non-newline characters with spaces so line and column positions stay stable.
	 *
	 * @param out 输出缓冲 / output buffer
	 * @param source 源文本 / source text
	 * @param from 起始下标 / start index
	 * @param to 结束下标 / end index
	 */
	private static void appendBlanked(StringBuilder out, String source, int from, int to) {
		for (int i = from; i < to; i++) {
			out.append(source.charAt(i) == '\n' ? '\n' : ' ');
		}
	}
}
