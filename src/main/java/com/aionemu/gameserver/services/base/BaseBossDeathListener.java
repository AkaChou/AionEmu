package com.aionemu.gameserver.services.base;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.eventcallback.OnDieEventCallback;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dao.BaseDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 基地 BOSS 死亡监听器，处理占领广播、种族切换、欧比斯登陆点与奖励。
 * Base boss death listener handling capture announce, race switch, abyss landing and rewards.
 *
 * @author Rinzler
 * @reworked Ranastic
 */
@SuppressWarnings("rawtypes")
public class BaseBossDeathListener extends OnDieEventCallback {
	private final Base<?> base;

	/**
	 * 绑定目标基地实例。
	 * Binds the target base instance.
	 *
	 * Base
	 */
	public BaseBossDeathListener(Base base) {
		this.base = base;
	}

	/**
	 * BOSS 死亡前结算归属、增益、登陆点与占领。
	 * Before boss death, resolves ownership, buffs, landing points and capture.
	 *
	 * Dying AI
	 */
	@Override
	public void onBeforeDie(AbstractAI obj) {
		Race race = null;
		Npc boss = base.getBoss();
		AionObject winner = base.getBoss().getAggroList().getMostDamage();
		if (winner instanceof Creature) {
			final Creature kill = (Creature) winner;
			applyBaseBuff();
			// 检查 kill 是否为 Player 类型。
			// Check if kill is of Player type.
			if (CustomConfig.ENABLE_BASE_REWARDS && kill instanceof Player) {
				giveBaseRewardsToPlayers((Player) kill); // 确保 kill 是 Player 类型 / Ensure kill is of Player type
			}
			if (kill.getRace().isPlayerRace()) {
				base.setRace(kill.getRace());
				race = kill.getRace();
			}
			announceCapture(null, kill);
		} else if (winner instanceof TemporaryPlayerTeam) {
			final TemporaryPlayerTeam team = (TemporaryPlayerTeam) winner;
			applyBaseBuff();
			if (team.getRace().isPlayerRace()) {
				base.setRace(team.getRace());
				race = team.getRace();
			}
			announceCapture(team, null);
		} else {
			// 处理其他类型的 winner 对象。
			// Handle other types of winner objects.
			base.setRace(Race.NPC);
		}
		if (base.getBaseLocation().getWorldId() == 400010000) {
			if (race == Race.ASMODIANS && boss.getRace() == Race.ELYOS) {
				GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE, false);
			}
			if (race == Race.ELYOS && boss.getRace() == Race.ASMODIANS) {
				GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE, false);
			}
			landingWinBase(race);
		}
		GameFeatureServices.baseService().capture(base.getId(), base.getRace());
	}

	/**
	 * BOSS 死亡后回调（当前无额外逻辑）。
	 * After-death callback (no-op currently).
	 *
	 * Dying AI
	 */
	@Override
	public void onAfterDie(AbstractAI obj) {
	}

	/**
	 * 向全服玩家广播基地被占领消息。
	 * Announces base capture to all players.
	 *
	 * @param team 获胜队伍，可为 null / Winning team, may be null
	 * @param kill 击杀生物，可为 null / Killing creature, may be null
	 */
	public void announceCapture(final TemporaryPlayerTeam team, final Creature kill) {
		final String baseName = base.getBaseLocation().getName();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (team != null && kill == null) {
					// %0 成功征服了 %1。 / %0 succeeded in conquering %1.
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1301039, team.getRace().getRaceDescriptionId(), baseName));
				} else {
					// %0 成功征服了 %1。 / %0 succeeded in conquering %1.
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1301039, kill.getRace().getRaceDescriptionId(), baseName));
				}
				// 欧比斯登陆 4.9.1 / Abyss Landing 4.9.1
				switch (player.getWorldId()) {
				case 400010000: // Reshanta.
					if (team != null && kill == null) {
						// %0 已占领 %1 基地，登陆点已增强。 / %0 has occupied %1 Base and the Landing is now enhanced.
						PacketSendUtility.sendPacket(player,
								new SM_SYSTEM_MESSAGE(1403186, team.getRace().getRaceDescriptionId(), baseName));
					} else {
						// %0 已占领 %1 基地，登陆点已增强。 / %0 has occupied %1 Base and the Landing is now enhanced.
						PacketSendUtility.sendPacket(player,
								new SM_SYSTEM_MESSAGE(1403186, kill.getRace().getRaceDescriptionId(), baseName));
					}
					break;
				}
			}
		});
	}

	/**
	 * 向全服玩家施加占领相关的种族削弱/增益提示与效果。
	 * Applies race bane buffs and related messages to all players.
	 */
	public void applyBaseBuff() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.getCommonData().getRace() == Race.ELYOS) {
					GameEngineServices.skillEngine().applyEffectDirectly(12115, player, player, 0); // Kaisinel's Bane.
					// 凯希内尔保护的力量环绕着你。 / The power of Kaisinel's Protection surrounds you.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_WEAK_RACE_BUFF_LIGHT_GAIN,
							5000);
					// 玛尔库坦的保护增强了敌对阵营。 / Marchutan's Protection has strengthened the opposing faction.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_WEAK_RACE_BUFF_DARK_WARNING, 10000);
				} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
					GameEngineServices.skillEngine().applyEffectDirectly(12117, player, player, 0); // Marchutan's Bane.
					// 玛尔库坦保护的力量环绕着你。 / The power of Marchutan's Protection surrounds you.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_WEAK_RACE_BUFF_DARK_GAIN,
							5000);
					// 凯希内尔的保护增强了敌对阵营。 / Kaisinel's Protection has strengthened the opposing faction.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_WEAK_RACE_BUFF_LIGHT_WARNING, 10000);
				}
			}
		});
	}

	/**
	 * 按地图向击杀者发送基地奖励 HTML 指南。
	 * Sends base reward HTML guide to the killer by map.
	 *
	 * Killer player
	 */
	protected void giveBaseRewardsToPlayers(Player player) {
		switch (player.getWorldId()) {
		case 210020000: // Eltnen.
		case 210040000: // Heiron.
		case 210050000: // Inggison.
		case 210130000: // Inggison [Master Server].
		case 220020000: // Morheim.
		case 220040000: // Beluslan.
		case 220070000: // Gelkmaros.
		case 220140000: // Gelkmaros [Master Server].
			HTMLService.sendGuideHtml(player, "adventurers_base1");
			break;
		case 600090000: // Kaldor.
		case 600100000: // Levinshor.
			HTMLService.sendGuideHtml(player, "adventurers_base2");
			break;
		case 400020000: // Belus.
		case 400040000: // Aspida.
		case 400050000: // Atanatos.
		case 400060000: // Disillon.
			HTMLService.sendGuideHtml(player, "adventurers_base3");
			break;
		case 600050000: // Katalam.
			HTMLService.sendGuideHtml(player, "adventurers_base4");
			break;
		}
	}

	/**
	 * 更新欧比斯登陆点因占领基地获得的积分。
	 * Updates abyss landing points earned by winning a base.
	 *
	 * @param race 获胜种族 / Winning race
	 */
	public void landingWinBase(Race race) {
		if (race == Race.ASMODIANS) {
			GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE, true);
		}
		if (race == Race.ELYOS) {
			GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE, true);
		}
	}

	/**
	 * 获取基地 DAO。
	 * Returns the base DAO.
	 *
	 * Base DAO
	 */
	private BaseDAO getDAO() {
		return DAOManager.getDAO(BaseDAO.class);
	}
}
