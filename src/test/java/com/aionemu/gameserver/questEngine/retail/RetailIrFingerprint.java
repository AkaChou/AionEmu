package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定义 IR 的规范化指纹：删除 quest-definition XML 之后，用它在**没有 XML** 的情况下继续证明
 * "真端驱动的定义 == 删除前 XML 编译出的定义"。
 * <p>
 * 归一化口径（与家族等价门禁一致）：节点按 {@code (status, packed)} 记一行、转换把
 * source/target 标签换成同口径的规范键，事件/条件/动作/after-commit/priority 用其规范文本，
 * 进度布局逐字段记录；全部行排序后取 SHA-256。标签本身是编译期内部名，不参与指纹。
 * metadata 轴由 {@code RetailMetadataEquivalenceGateTest} 单独负责。
 * <p>
 * Normalized IR fingerprint used as the frozen evidence once the XML definition is deleted.
 */
final class RetailIrFingerprint {

	private RetailIrFingerprint() {
	}

	/** 规范化文本（行已排序，标签已归一）。 / Canonical text with sorted lines and normalized labels. */
	static String canonicalText(QuestDefinition definition) {
		Map<String, String> keys = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			keys.put(node.label(), key(definition, node));
		}
		List<String> lines = new ArrayList<>();
		for (QuestNode node : definition.nodes()) {
			lines.add("N\t" + key(definition, node));
		}
		for (QuestTransition transition : definition.transitions()) {
			String source = transition.sourceNode() == null ? "null" : keys.get(transition.sourceNode());
			for (String event : RetailKillRoutes.eventTexts(transition.event())) {
				lines.add("T\t" + source + ">" + event + ">" + keys.get(transition.targetNode()) + ">"
					+ transition.conditions() + ">" + transition.actions() + ">" + transition.afterCommit() + ">"
					+ (transition.priority() == null ? "-" : transition.priority()));
			}
		}
		for (BitField field : definition.progressLayout().fields()) {
			lines.add("L\t" + field);
		}
		lines.sort(null);
		return String.join("\n", lines);
	}

	/** 规范化文本的 SHA-256（十六进制）。 / SHA-256 of the canonical text, in hex. */
	static String fingerprint(QuestDefinition definition) {
		String text = canonicalText(definition);
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte value : digest) {
				hex.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	private static String key(QuestDefinition definition, QuestNode node) {
		return node.projection().status() + "/" + definition.progressLayout().pack(node.projection().variables());
	}
}
