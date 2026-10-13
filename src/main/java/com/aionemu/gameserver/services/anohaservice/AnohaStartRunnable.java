package com.aionemu.gameserver.services.anohaservice;

import java.util.Map;

import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.AllArgsConstructor;

/**
 * 狂暴阿诺哈活动启动定时任务。
 * Start runnable for the Berserk Anoha world event.
 * <p>刷出剑特效、广播回归预告、分阶段倒计时，并在 30 分钟后启动地点。
 * Spawns sword effect, broadcasts return warnings, stages countdown, then starts the location after 30 minutes.</p>
 * @author Rinzler (Encom)
 */
@AllArgsConstructor
public class AnohaStartRunnable implements Runnable {

	private final int id;

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 清理上一轮遗留的临时对象。 / Despawn temporary objects left by the previous cycle.
		AnohaService.getInstance().clearAdventObjects(id);
		// 狂暴阿诺哈剑效果。 / Berserk Anoha Sword Effect.
		AnohaService.getInstance().adventSwordEffectSP(id);
		// 狂暴阿诺哈将在 30 分钟后返回卡尔多。 / Berserk Anoha will return to Kaldor in 30 minutes.
		AnohaService.getInstance().berserkAnohaMsg1(id);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> AnohaService.getInstance().sendRequest(player));
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 暴怒的韦尔休守护者将在 5 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 5 minutes.
			AnohaService.getInstance().wealhtheowGuardianMsg1(id);
		}, 1500000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 暴怒的韦尔休守护者将在 3 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 3 minutes.
			AnohaService.getInstance().wealhtheowGuardianMsg2(id);
		}, 1620000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 暴怒的韦尔休守护者将在 1 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 1 minute.
			AnohaService.getInstance().wealhtheowGuardianMsg3(id);
		}, 1740000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			Map<Integer, AnohaLocation> locations = AnohaService.getInstance().getAnohaLocations();
			for (final AnohaLocation loc : locations.values()) {
				if (loc.getId() == id) {
					AnohaService.getInstance().startAnoha(loc.getId());
				}
			}
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
				// 召唤狂暴阿诺哈。 / Summon Berserk Anoha.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Anoha_Spawn);
			});
		}, 1800000);
	}
}
