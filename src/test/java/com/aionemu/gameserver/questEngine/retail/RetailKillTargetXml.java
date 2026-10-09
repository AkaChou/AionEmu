package com.aionemu.gameserver.questEngine.retail;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 对拍用 XML 归一化：把击杀目标 id 集合展开成同名族闭包
 * （{@link RetailNpcNameIndex#withDisplayNameVariants}），与原版驱动侧落在同一等价集上。
 * <p>
 * 覆盖三个携带击杀目标的元素：{@code dimension}、{@code kill-routes}、{@code kill-npc}
 * （{@code kill-npc} 的单值写法会改写成 {@code npc-ids}）；对话类 {@code npc-ids} 不动。
 * <p>
 * XML-side normalization for comparison: kill targets expand to the display-name family closure.
 */
final class RetailKillTargetXml {

	/** 携带击杀目标 id 集合的元素。 / Elements carrying kill-target id sets. */
	private static final Pattern KILL_TARGET_ATTRIBUTE = Pattern.compile(
		"(<(?:dimension|kill-routes|kill-npc)\\b[^>]*?npc-ids=\")([^\"]*)(\")");
	/** 单值击杀目标元素（{@code <kill-npc npc-id="X"/>}）。 / Single-id kill element. */
	private static final Pattern KILL_SINGLE_ATTRIBUTE = Pattern.compile(
		"(<kill-npc\\b[^>]*?)npc-id=\"(\\d+)\"([^>]*>)");

	private RetailKillTargetXml() {
	}

	/** 展开击杀目标后的 XML 文本。 / XML text with kill targets expanded. */
	static String expand(String xml, RetailNpcNameIndex index) {
		Matcher matcher = KILL_TARGET_ATTRIBUTE.matcher(xml);
		StringBuilder expanded = new StringBuilder(xml.length());
		while (matcher.find()) {
			String replacement = matcher.group(1) + joined(parseIds(matcher.group(2)), index) + matcher.group(3);
			matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(expanded);
		Matcher singular = KILL_SINGLE_ATTRIBUTE.matcher(expanded);
		StringBuilder normalized = new StringBuilder(expanded.length());
		while (singular.find()) {
			String ids = joined(Set.of(Integer.parseInt(singular.group(2))), index);
			String replacement = singular.group(1) + (ids.indexOf(' ') < 0
				? "npc-id=\"" + singular.group(2) + "\""
				: "npc-ids=\"" + ids + "\"") + singular.group(3);
			singular.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
		}
		singular.appendTail(normalized);
		return normalized.toString();
	}

	private static Set<Integer> parseIds(String raw) {
		return Arrays.stream(raw.trim().split("\\s+"))
			.filter(value -> !value.isBlank())
			.map(Integer::parseInt)
			.collect(Collectors.toCollection(TreeSet::new));
	}

	/** 同显示名闭包，升序空格分隔。 / Display-name closure as ascending space-separated ids. */
	private static String joined(Set<Integer> ids, RetailNpcNameIndex index) {
		return index.withDisplayNameVariants(ids).stream().sorted()
			.map(String::valueOf).collect(Collectors.joining(" "));
	}
}
