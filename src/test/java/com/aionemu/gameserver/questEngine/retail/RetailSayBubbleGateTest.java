package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Say 气泡面钉子门（2026-10-02 偏差修复第二批）：DD 附加动作 case 7 的字符串键集冻结 +
 * 原版正文全覆盖 + 实体解码完备。
 * <ul>
 *   <li>键集 = 7 键冻结：新增/删键必须显式改本门（防数据漂移静默扩面）。</li>
 *   <li>每个 id 必须有非空正文：`NativeSayPort` 的系统消息兜底路径因此永不为已登记 id 触发。</li>
 *   <li>正文不得残留未解码的 `&xxx;` 实体（say 频道按纯文本渲染）。</li>
 * </ul>
 * Pin gate for the say-bubble face (deviation-fix batch 2): the DD Message key set is frozen,
 * every id carries a non-blank retail body (so the system-message fallback can never fire for
 * a registered id), and no undecoded {@code &xxx;} entity may remain (say chat renders plain text).
 */
class RetailSayBubbleGateTest {

	/** DD 附加动作 case 7 实际引用的键集（原版表行载荷，逐字冻结）。 / Keys referenced by DD case-7 rows. */
	private static final Set<String> FROZEN_KEYS = Set.of(
		"STR_QUEST_SAY_LF4_04", "STR_QUEST_SAY_LF4_05", "STR_QUEST_SAY_LF4_06",
		"STR_QUEST_SAY_LF4_21", "STR_QUEST_SAY_AB1_005", "STR_QUEST_SAY_LF5_001",
		"STR_CHAT_DF5_Quest_Gossip_41");

	/** 未解码实体形态（&hellip; 等）。 / Undecoded entity shape. */
	private static final Pattern RAW_ENTITY = Pattern.compile("&[a-zA-Z]+;|&#\\d+;");

	@Test
	void sayKeySetIsFrozenAndEveryIdCarriesADecodedRetailBody() {
		RetailStringIds ids = RetailStringIds.instance();
		assertEquals(FROZEN_KEYS, ids.keys(), "键集漂移：键集 = DD case-7 载荷冻结集，新增/删键必须改本门");
		for (String key : FROZEN_KEYS) {
			Integer id = ids.resolve(key);
			assertNotNull(id, () -> key + " 未解析（原版装载失败语义）");
			String body = ids.bodyOf(id);
			assertNotNull(body, () -> key + "(" + id + ") 无正文：系统消息兜底将被触发（偏差复活）");
			assertFalse(body.isBlank(), () -> key + "(" + id + ") 正文为空白");
			assertFalse(RAW_ENTITY.matcher(body).find(), () -> key + "(" + id + ") 正文残留未解码实体: " + body);
		}
		assertEquals(FROZEN_KEYS.size(), ids.bodies().size(), "正文行数必须与键集等大（每键必有正文）");
	}
}
