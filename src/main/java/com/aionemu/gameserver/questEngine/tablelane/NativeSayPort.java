package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 原生任务车道的播报端口：DD 附加动作 case 7（`Message`，真端执行器 case 7 →
 * `IUserImp::Say` = NC_SAY_CODE 包，文本 = 字符串表 id）的唯一出口。
 * <p>
 * 已知偏差（已裁定）：真端走 say 频道气泡（说话者 = 玩家）；本服以系统消息通道转发同一字符串
 * id（`SM_SYSTEM_MESSAGE`），客户端按自身字符串表渲染同文本。
 * <p>
 * The say port of the native quest lane: the single exit for the DD Message extra action
 * (retail executor case 7 → IUserImp::Say with a string-table id). Adjudicated deviation:
 * system-message channel instead of the retail say bubble; the client resolves the same id.
 */
public interface NativeSayPort {

	/** 播报字符串表 id 对应文本。 / Announces the string-table id. */
	void say(Player player, int stringId);

	/** 生产实现（SM_SYSTEM_MESSAGE）。 / The live implementation (SM_SYSTEM_MESSAGE). */
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
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(stringId));
		}
	}
}
