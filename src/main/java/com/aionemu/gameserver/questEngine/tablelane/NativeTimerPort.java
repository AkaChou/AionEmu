package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 原生任务车道的计时端口：DD 附加动作 case 10（`Add Timer`，原版执行器 case 10 →
 * `IUserImp::AddQuestTimer` → NPCServer 计时中转）的唯一出口。
 * <p>
 * 原版语义链（2026-10-02 取证闭环）：装载器 `FUN_180c49610` case 10 解析 `时间, 目标步, 旗标`；
 * 执行时武装（Server64 `AddQuestTimer` = `ServerToNPCServer` opcode 0xff93 转发 NPCServer 计时）；
 * 到期由宿主按 quest id 回调 ScriptDLL64 注册表 → `FUN_180c46d80` 检查：任务状态 3（进行中）
 * ∧ `0 < 当前步 < 目标步` ⇒ 旗标 0 推进到目标步（`+0xf0` 直写）/ 旗标 1 弃任（`+0x160`）。
 * 本服镜像：本端口只负责延时回调（线程池），到期判定与写面在
 * {@link DataDrivenNativeRuntime#onQuestTimerExpired}（可单测）。
 * <p>
 * The native quest lane's timer port: the single exit for the DD Add Timer extra action (retail
 * case 10 → IUserImp::AddQuestTimer, forwarded to the NPC server for ticking). Retail chain fully
 * adjudicated 2026-10-02: loader FUN_180c49610 parses `seconds, destStep, flag`; expiry dispatches
 * by quest id into FUN_180c46d80 which advances (flag 0) or abandons (flag 1) while the quest is
 * in progress and `0 < current step < dest`. This port only delays the callback; the expiry
 * verdict lives in {@link DataDrivenNativeRuntime#onQuestTimerExpired} (unit-testable).
 */
public interface NativeTimerPort {

	/**
	 * 武装一个任务计时器（原版 AddQuestTimer 面）。
	 * Arms one quest timer (the retail AddQuestTimer face).
	 */
	void schedule(Player player, int questId, int seconds, int destStep, boolean abandonOnExpiry);

	/** 生产实现（线程池延时回调）。 / The live implementation (thread-pool delayed callback). */
	static NativeTimerPort live() {
		return Live.INSTANCE;
	}

	/** 生产实现持有者。 / Live implementation holder. */
	final class Live implements NativeTimerPort {

		private static final NativeTimerPort INSTANCE = new Live();

		private Live() {
		}

		@Override
		public void schedule(Player player, int questId, int seconds, int destStep, boolean abandonOnExpiry) {
			ThreadPoolManager.getInstance().schedule(() -> {
				// 玩家离线时放弃判定（原版到期回调以在线 User 为前提）。
				// Skip when the player went offline (the retail expiry callback addresses a live User).
				if (player == null || !player.isOnline()) {
					return;
				}
				DataDrivenNativeRuntime.instance().onQuestTimerExpired(player, questId, destStep, abandonOnExpiry);
			}, seconds * 1000L);
		}
	}
}
