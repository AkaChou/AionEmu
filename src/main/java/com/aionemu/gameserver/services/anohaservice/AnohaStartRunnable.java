package com.aionemu.gameserver.services.anohaservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 狂暴阿诺哈活动启动定时任务。
 * Start runnable for the Berserk Anoha world event.
 *
 * <p>刷出剑特效、广播回归预告、分阶段倒计时，并在 30 分钟后启动地点。
 * Spawns sword effect, broadcasts return warnings, stages countdown, then starts the location after 30 minutes.</p>
 *
 * @author Rinzler (Encom)
 */
public class AnohaStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public AnohaStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 狂暴阿诺哈剑效果。 / Berserk Anoha Sword Effect.
		GameLocationBootstrapServices.anohaService().adventSwordEffectSP(id);
		// 狂暴阿诺哈将在 30 分钟后返回卡尔多。 / Berserk Anoha will return to Kaldor in 30 minutes.
		GameLocationBootstrapServices.anohaService().berserkAnohaMsg1(id);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				GameLocationBootstrapServices.anohaService().sendRequest(player);
			}
		});
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 暴怒的韦尔休守护者将在 5 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 5 minutes.
				GameLocationBootstrapServices.anohaService().wealhtheowGuardianMsg1(id);
			}
		}, 1500000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 暴怒的韦尔休守护者将在 3 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 3 minutes.
				GameLocationBootstrapServices.anohaService().wealhtheowGuardianMsg2(id);
			}
		}, 1620000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 暴怒的韦尔休守护者将在 1 分钟后出现。 / Enraged Wealhtheow Guardian will appear in 1 minute.
				GameLocationBootstrapServices.anohaService().wealhtheowGuardianMsg3(id);
			}
		}, 1740000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, AnohaLocation> locations = GameLocationBootstrapServices.anohaService().getAnohaLocations();
				for (final AnohaLocation loc : locations.values()) {
					if (loc.getId() == id) {
						GameLocationBootstrapServices.anohaService().startAnoha(loc.getId());
					}
				}
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						// 召唤狂暴阿诺哈。 / Summon Berserk Anoha.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Anoha_Spawn);
					}
				});
			}
		}, 1800000);
	}
}
