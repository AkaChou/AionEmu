package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 疯狂大埃服务，管理限时 PvP 活动状态与奖励。
 * Crazy Daeva service managing timed PvP event state and rewards.
 * @author Rinzler (Encom)
 */
@Slf4j
public class CrazyDaevaService {

	/** Spring 实例提供者 / Spring instance provider
     * -- SETTER --
     *  注入 Spring ObjectProvider 以覆盖默认单例。
     *  Injects a Spring ObjectProvider that overrides the default singleton.
     */
	@Setter
    private static volatile ObjectProvider<CrazyDaevaService> instanceProvider;

	/** 本轮已选出的疯狂大埃计数。 / Crazy Daeva count selected this round. */
	int crazyCount = 0;
	private final List<Runnable> schedules = new ArrayList<>();

	/**
	 * 按配置 cron 注册活动开始定时器。
	 * Registers the event start timer from configured cron.
	 */
	public synchronized void startTimer() {
		for (Runnable schedule : schedules) {
			GameCronServices.cronService().cancel(schedule);
		}
		schedules.clear();
		if (!EventsConfig.ENABLE_CRAZY) {
			return;
		}
		String[] times = EventsConfig.CRAZY_TIMES.split("\\|");
		for (String cron : times) {
			Runnable schedule = () -> checkStart();
			schedules.add(schedule);
			GameCronServices.cronService().schedule(schedule, cron);
			log.info(I18n.get("log.22a3e7a99e76", cron, EventsConfig.CRAZY_ENDTIME));
		}
	}

	/**
	 * 检查并启动一轮疯狂大埃（选人 + 结束清理）。
	 * Checks and starts one Crazy Daeva round (choose + end cleanup).
	 */
	public void checkStart() {
		startChoose();
		clearCrazy();
		log.info(I18n.get("log.1b3e1cec5043"));
	}

	/**
	 * 随机挑选一名在线玩家，发送活动通知但不执行原地传送或 PvP 奖励。
	 * Randomly selects one online player, sends the event notice, but does not teleport in place or grant a PvP reward.
	 */
	public void startChoose() {
		crazyCount = 0;
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			int rnd = 0;
			rnd = Rnd.get(1, 100);
			player.setRndCrazy(rnd);
			if (player.getRndCrazy() >= EventsConfig.CRAZY_LOWEST_RND && player.getLevel() >= 55) {
				crazyCount++;
				if (crazyCount == 1) {
					PacketSendUtility.sendYellowMessageOnCenter(player, "CRAZY DAEVA " + player.getName());
					log.info(I18n.get("log.b2c3238d6b52", player.getName()));
					player.setInCrazy(true);
				}
			}
			log.info(I18n.get("log.17935c8ff33a", player.getName(), rnd));
		});
	}

	/**
	 * 增加疯狂大埃连杀并更新连杀等级。
	 * Increases Crazy Daeva kill count and updates spree level.
	 * winner
	 */
	public void increaseRawKillCount(Player winner) {
		int currentCrazyKillCount = winner.getCrazyKillCount();
		winner.setCrazyKillCount(currentCrazyKillCount + 1);
		int newCrazyKillCount = currentCrazyKillCount + 1;

		if (newCrazyKillCount >= 0 && newCrazyKillCount <= 10) {
			updateCrazyLevel(winner, 1);
		}
		if (newCrazyKillCount >= 10 && newCrazyKillCount <= 20) {
			updateCrazyLevel(winner, 2);
		}
		if (newCrazyKillCount >= 20 && newCrazyKillCount <= 30) {
			updateCrazyLevel(winner, 3);
		}
		log.info(I18n.get("log.6323ce0a3ae8", newCrazyKillCount));
	}

	/**
	 * 更新疯狂大埃连杀等级。
	 * Updates the Crazy Daeva spree level.
	 * winner
	 * @param level 连杀等级 / spree level
	 */
	private void updateCrazyLevel(Player winner, int level) {
		winner.setCrazyLevel(level);
	}

	/**
	 * 处理疯狂大埃死亡并广播终结者。
	 * Handles Crazy Daeva death and announces the spree ender.
	 * victim
	 * killer
	 * whether PvP death
	 */
	public void crazyOnDie(Player victim, Creature killer, boolean isPvPDeath) {
		if (victim.isInCrazy()) {
			victim.setCrazyLevel(0);
			sendEndSpreeMessage(victim, killer, isPvPDeath);
		}
	}

	/**
	 * 发放终结奖励并向全服广播连杀结束。
	 * Grants ender reward and broadcasts the spree end to all players.
	 * victim
	 * killer
	 * whether PvP death
	 */
	private void sendEndSpreeMessage(final Player victim, Creature killer, boolean isPvPDeath) {
		if (killer instanceof Player) {
			if (killer.getRace().getRaceId() != victim.getRace().getRaceId()) {
				final String spreeEnder = isPvPDeath ? killer.getName() : "Killer";
				AbyssPointsService.addAp((Player) killer, 5000);
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> PacketSendUtility.sendYellowMessageOnCenter(player,
						"Crazier " + victim.getName() + " has slain by " + spreeEnder + "!"));
				log.info(I18n.get("log.fe71cec0426c", victim.getName(), spreeEnder));
			}
		}
	}

	/**
	 * 结束活动：清理状态并在配置时长后停止。
	 * Ends the event: clears state and stops after configured duration.
	 */
	public void clearCrazy() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
				if (player.isInCrazy()) {
					if (player.getCrazyLevel() == 1) {
						AbyssPointsService.addAp(player, 5000);
						log.info(I18n.get("log.a3154d047f49", player.getName(), player.getCrazyKillCount()));
					}
					if (player.getCrazyLevel() == 2) {
						AbyssPointsService.addAp(player, 10000);
						log.info(I18n.get("log.3384ff2aa68a", player.getName(), player.getCrazyKillCount()));
					}
					if (player.getCrazyLevel() == 3) {
						AbyssPointsService.addAp(player, 15000);
						log.info(I18n.get("log.5088c1ecb7b6", player.getName(), player.getCrazyKillCount()));
					}
					player.setCrazyKillCount(0);
					player.setCrazyLevel(0);
					player.setInCrazy(false);
					player.setRndCrazy(0);
				}
				player.setInCrazy(false);
				player.setRndCrazy(0);
				player.getLifeStats().increaseHp(TYPE.HP, player.getLifeStats().getMaxHp() + 5000);

				PacketSendUtility.sendYellowMessageOnCenter(player, "Crazy Daeva event has stopped!");
			});
			log.info(I18n.get("log.f2f29a601942"));
		}, (long) EventsConfig.CRAZY_ENDTIME * 60 * 1000); // 活动停止时间 / time stop
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final CrazyDaevaService getInstance() {
		ObjectProvider<CrazyDaevaService> provider = instanceProvider;
		CrazyDaevaService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("CrazyDaevaService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
