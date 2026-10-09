package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.retail.RetailStringIds;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 原生任务车道的播报端口：DD 附加动作 case 7（`Message`，原版执行器 case 7 →
 * `IUserImp::Say` = NC_SAY_CODE 包，说话者 = 玩家 + 字符串表 id）的唯一出口。
 * <p>
 * 2026-10-02 偏差修复（第二批）：翻转登记偏差，落原版 say 气泡面——正文取自同一原版
 * 字符串表入仓行（第 3 列 `body`），经 {@link SM_MESSAGE} `NORMAL`（say）频道广播给玩家
 * 及周身（与玩家普通说话同通道，客户端渲染头顶气泡）；id 无正文的旧行走系统消息兜底
 * （原登记偏差仅在该兜底路径保留，门禁钉正文全覆盖）。
 * <p>
 * The say port of the native quest lane: the single exit for the DD Message extra action
 * (retail executor case 7 → IUserImp::Say, speaker = player + string-table id). Deviation
 * flipped 2026-10-02: the retail say-bubble face is now live — the body comes from the same
 * retail string-table row (column 3) and is broadcast on the NORMAL (say) channel like player
 * chat, rendering a head bubble; legacy rows without a body fall back to the system-message
 * channel (the registered deviation survives only on that fallback path; a gate pins full
 * body coverage).
 */
public interface NativeSayPort {

	/** 播报字符串表 id 对应文本。 / Announces the string-table id. */
	void say(Player player, int stringId);

	/** 生产实现（say 气泡广播）。 / The live implementation (say-bubble broadcast). */
	static NativeSayPort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeSayPort {

		private static final NativeSayPort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void say(Player player, int stringId) {
			String body = RetailStringIds.instance().bodyOf(stringId);
			if (body != null) {
				PacketSendUtility.broadcastPacket(player,
					new SM_MESSAGE(player, body, ChatType.NORMAL), true);
			}
			else {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(stringId));
			}
		}
	}
}
