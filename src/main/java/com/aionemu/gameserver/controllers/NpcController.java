package com.aionemu.gameserver.controllers;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.service.PlayerTeamDistributionService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.templates.npc.NpcRank;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.services.RespawnService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.instance.InstanceScaler;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.StatFunctions;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
/**
 * NPC 控制器，管理视野、生成/消失、死亡奖励、对话与区域事件。
 * NPC controller managing sight, spawn/despawn, death rewards, dialogs and zone events.
 *
 * @author ATracer
 */
@Slf4j

public class NpcController extends CreatureController<Npc> {

	/**
	 * 对象离开 NPC 视野时回调。
	 * Callback when an object leaves the NPC's sight.
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		if (object instanceof Creature) {
			getOwner().getAi2().onCreatureEvent(AIEventType.CREATURE_NOT_SEE, (Creature) object);
		}
		super.notSee(object, isOutOfRange);
		if (object instanceof Creature && (getOwner().getAi2().getSubState() != AISubState.TARGET_LOST
				|| object != getOwner().getTarget())) {
			getOwner().getAggroList().remove((Creature) object);
		}
	}

	/**
	 * 对象进入 NPC 视野时回调。
	 * Callback when an object enters the NPC's sight.
	 *
	 * @param object 进入视野的对象 / the object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		super.see(object);
		Npc owner = getOwner();
		
		if (object instanceof Creature) {
			owner.getAi2().onCreatureEvent(AIEventType.CREATURE_SEE, (Creature) object);
		}
		
		if (object instanceof Player) {
			Player player = (Player) object;
			
			if (owner.getLifeStats().isAlreadyDead()) {
				if (!owner.isInState(CreatureState.DEAD) && !owner.isInState(CreatureState.FLOATING_CORPSE)) {
					owner.setState(CreatureState.DEAD);
				}
				
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						if (owner.isSpawned() && owner.getLifeStats().isAlreadyDead()) {
							PacketSendUtility.sendPacket(player, new SM_EMOTION(owner, EmotionType.DIE, 0, 0));
							GameCoreGameplayServices.dropService().see(player, owner);
						}
					}
				}, 100);
			}
		}
	}

	/**
	 * 生成前初始化。
	 * Initialization before spawn.
	 *
	 */
	@Override
	public void onBeforeSpawn() {
		super.onBeforeSpawn();
		Npc owner = getOwner();

		// 从 NPC 模板设置状态 / set state from npc templates
		if (owner.getObjectTemplate().getState() != 0) {
			owner.setState(owner.getObjectTemplate().getState());
		} else {
			owner.setState(CreatureState.NPC_IDLE);
		}
		
		owner.getLifeStats().setCurrentHpPercent(100);
		owner.getLifeStats().setCurrentMpPercent(100);
		InstanceScaler.onBeforeSpawn(owner);
		owner.getAi2().onGeneralEvent(AIEventType.RESPAWNED);
		
		if (owner.getSpawn().canFly()) {
			owner.setState(CreatureState.FLYING);
		}

		if (owner.getSpawn().getState() != 0) {
			owner.setState(owner.getSpawn().getState());
		}
		
	}

	/**
	 * 生成后处理。
	 * Processing after spawn.
	 *
	 */
	@Override
	public void onAfterSpawn() {
		super.onAfterSpawn();
		getOwner().getAi2().onGeneralEvent(AIEventType.SPAWNED);
	}

	/**
	 * 消失时处理。
	 * Processing on despawn.
	 *
	 */
	@Override
	public void onDespawn() {
		Npc owner = getOwner();
		GameCoreGameplayServices.dropService().unregisterDrop(getOwner());
		owner.getAi2().onGeneralEvent(AIEventType.DESPAWNED);
		super.onDespawn();
	}

	/**
	 * 发送击败命名 NPC 的系统消息。
	 * Sends the system message for defeating a named NPC.
	 *
	 * killing player
	 */
	public void defeatNamedMsg(final Player player) {
		Npc owner = getOwner();
		final int npcNameId = owner.getObjectTemplate().getNameId();
		NpcRank npcRank = owner.getObjectTemplate().getRank();
		if (npcRank == NpcRank.EXPERT && !player.isInInstance()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player players) {
					// “玩家名”击杀了“命名怪” / "Player Name" has killed "Named Monster"
					PacketSendUtility.sendPacket(players, new SM_SYSTEM_MESSAGE(1400021, player.getName(), new DescriptionId(npcNameId * 2 + 1)));
				}
			});
		}
	}

	/**
	 * NPC 死亡处理：掉落、奖励与重生调度。
	 * NPC death handling: drops, rewards and respawn scheduling.
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	@Override
	public void onDie(Creature lastAttacker) {
		Npc owner = getOwner();
		
		owner.unsetState(CreatureState.ACTIVE);
		owner.unsetState(CreatureState.FLYING);
		owner.unsetState(CreatureState.GLIDING);
		owner.setState(CreatureState.DEAD);
		
		if (owner.getSpawn().hasPool()) {
			owner.getSpawn().setUse(false);
		}

		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.DIE, 0, owner.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()));

		try {
			if (owner.getAi2().poll(AIQuestion.SHOULD_REWARD)) {
				this.doReward();
			}
			owner.getPosition().getWorldMapInstance().getInstanceHandler().onDie(owner);
			owner.getAi2().onGeneralEvent(AIEventType.DIED);
		} finally { // always make sure npc is scheduled to respawn
			if (owner.getAi2().poll(AIQuestion.SHOULD_DECAY)) {
				addTask(TaskId.DECAY, RespawnService.scheduleDecayTask(owner));
				if (owner.getSpawn() != null && owner.getSpawn().getStaticId() > 0) {
					GameWorldServices.geoService().despawnPlaceableObject(owner.getWorldId(), owner.getInstanceId(),
							owner.getSpawn().getStaticId());
				}
			}
			if (owner.getAi2().poll(AIQuestion.SHOULD_RESPAWN) && !owner.isDeleteDelayed() && !GameFeatureServices.siegeService().isSiegeNpcInActiveSiege(owner)) {
				Future<?> respawnTask = scheduleRespawn();
				if (respawnTask != null) {
					addTask(TaskId.RESPAWN, respawnTask);
				}
			} else if (!hasScheduledTask(TaskId.DECAY)) {
				onDelete();
			}
		}
		super.onDie(lastAttacker);
	}

	/**
	 * 分发击杀经验与欧比斯点等奖励。
	 * Distributes kill exp, abyss points and related rewards.
	 *
	 */
	@Override
	public void doReward() {
		super.doReward();
		int kinahCount = 0;
		AggroList list = getOwner().getAggroList();
		Collection<AggroInfo> finalList = list.getFinalDamageList(true);
		if (getOwner() instanceof SiegeNpc) {
			rewardSiegeNpc();
		}

		AionObject winner = list.getMostDamage();

		if (winner == null) {
			return;
		}

		float totalDmg = 0;

		for (AggroInfo info : finalList) {
			totalDmg += info.getDamage();
		}

		if (totalDmg <= 0) {
			return;
		}

		for (AggroInfo info : finalList) {
			AionObject attacker = info.getAttacker();
			// PvE 通行奖励 / PvE Toll Reward
			if (attacker instanceof Player) {
				if (CustomConfig.ENABLE_PVE_TOLL_REWARD) {
					if (Rnd.get(0, 100) > CustomConfig.TOLL_PVE_CHANCE) {
						Player player = (Player) attacker;
						for (String worldIds : CustomConfig.TOLL_PVE_WORLDID.split(",")) {
							if (player.getWorldId() == Integer.parseInt(worldIds)) {
								GameRuntimeServices.inGameShopEn().addToll(player, CustomConfig.TOLL_PVE_QUANTITY);
								PacketSendUtility.sendMessage(player, "You have received " + CustomConfig.TOLL_PVE_QUANTITY + " tolls from PvE!");
							}
						}

					}
				}
			}

			// 我们不是奖励 NPC / We are not reward Npc's
			if (attacker instanceof Npc) {
				continue;
			}
			float percentage = info.getDamage() / totalDmg;
			if (percentage > 1) {
				continue;
			}
			if (attacker instanceof TemporaryPlayerTeam<?>) {
				PlayerTeamDistributionService.doReward((TemporaryPlayerTeam<?>) attacker, percentage, getOwner(), winner);
			} else if (attacker instanceof Player && ((Player) attacker).isInGroup2()) {
				PlayerTeamDistributionService.doReward(((Player) attacker).getPlayerGroup2(), percentage, getOwner(), winner);
			} else if (attacker instanceof Player) {
				Player player = (Player) attacker;
				if (!player.getLifeStats().isAlreadyDead()) {
					long rewardXp = StatFunctions.calculateSoloExperienceReward(player, getOwner());
					int rewardDp = StatFunctions.calculateSoloDPReward(player, getOwner());
					float rewardAp = 1;
					rewardXp *= percentage;
					rewardDp *= percentage;
					rewardAp *= percentage;
					GameEngineServices.questEngine().onKill(new QuestEnv(getOwner(), player, 0, 0));
					// 玩家击败“Boss”时全服可见！！！ / When a player defeat a "Boss" all ppls on server see!!!
					defeatNamedMsg(player);
					// 单人经验奖励（新系统，正式服北美经验） / Reward XP Solo (New system, Exp Retail NA)
					switch (player.getWorldId()) {
					case 301720000: // Mirash Sanctuary.
					case 302100000: // Fissure Of Oblivion.
					case 302110000: // [Opportunity] Fissure Of Oblivion.
					case 302400000: // Crucible Spire.
					case 210100000: // Iluma.
					case 220110000: // Norsvold.
					case 600040000: // Tiamaranta's Eye.
					case 600090000: // Kaldor.
					case 600100000: // Levinshor.
					default:
						player.getCommonData().addExp(rewardXp, RewardType.HUNTING, this.getOwner().getObjectTemplate().getNameId());
						break;
					}
					player.getCommonData().addDp(rewardDp);
					if (getOwner().isRewardAP()) {
						int calculatedAp = StatFunctions.calculatePvEApGained(player, getOwner());
						rewardAp *= calculatedAp;
						if (rewardAp >= 1) {
							player.getCommonData().addAbyssFavor(1500); // 0.15% Abyss Favor Energy.
							AbyssPointsService.addAp(player, getOwner(), (int) rewardAp);
							PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
						}
					}
					if (attacker.equals(winner)) {
						GameWorldServices.dropRegistrationService().registerDrop(getOwner(), player, player.getLevel(), null);
					}
					// 自动掉落基纳。 / Auto Drop Kinah.
					if (CustomConfig.AUTO_KINAH_ENABLED) {
						switch (player.getWorldId()) {
						case 210010000: // Poeta.
						case 220010000: // Ishalgen.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210030000: // Verteron.
						case 220030000: // Altgard.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 1500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210020000: // Eltnen.
						case 220020000: // Morheim.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 2000) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210040000: // Heiron.
						case 220040000: // Beluslan.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 2500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210060000: // Theobomos.
						case 220050000: // Brushtonin.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 3000) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210130000: // Inggison [Master Server].
						case 220140000: // Gelkmaros [Master Server].
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 3500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210070000: // Cygnea.
						case 220080000: // Enshar.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 4000) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 400010000: // Reshanta.
						case 400020000: // Belus.
						case 400040000: // Aspida.
						case 400050000: // Atanatos.
						case 400060000: // Disillon.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 4500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 600090000: // Kaldor.
						case 600100000: // Levinshor.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 5000) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210100000: // Iluma.
						case 220110000: // Norsvold.
						case 600040000: // Tiamaranta's Eye.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 5500) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						case 210090000: // Idian Depths E.
						case 220100000: // Idian Depths A.
							if (player.getLevel() < getOwner().getLevel() + 5) {
								kinahCount = Rnd.get(100, 6000) * player.getLevel();
							} else if (player.getLevel() > getOwner().getLevel() + 5) {
								kinahCount = 1000;
							}
							break;
						default:
							kinahCount = 0;
							break;
						}
						if (player.isInInstance() && player.getLevel() < getOwner().getLevel() + 5) {
							kinahCount = Rnd.get(100, 1000) * player.getLevel();
						} else if (player.isInInstance() && player.getLevel() > getOwner().getLevel() + 5) {
							kinahCount = 1000;
						}
						player.getInventory().increaseKinah(kinahCount);
					}
					// 伯丁之星。 / Berdin's Star.
					if (getOwner().getLevel() >= 10) {
						player.getCommonData().addBerdinStar(1575000); // 0.14%
						PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
					}
					// 成长光环。 / Aura Of Growth.
					if (getOwner().getLevel() >= 66) {
						if (Rnd.get(1, 100) < RateConfig.AURA_OF_GROWTH) {
							GameFeatureServices.growthEnergy().addGrowthEnergy(player);
							PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
						}
					}
					// 阿特雷亚图鉴。 / Atreian Bestiary.
					if (getOwner().getLevel() >= 66) {
						GameFeatureServices.atreianBestiaryService().onKill(player, getOwner().getNpcId());
					}
				}
			}
		}
	}

	/**
	 * 获取所有者 NPC。
	 * Gets the owner NPC.
	 *
	 * @return owner NPC / 所有者 NPC / owner NPC。
	 */
	@Override
	public Npc getOwner() {
		return (Npc) super.getOwner();
	}

	/**
	 * 处理玩家对 NPC 的对话请求。
	 * Handles a player dialog request to the NPC.
	 *
	 * requesting player
	 */
	@Override
	public void onDialogRequest(Player player) {
		// 通知 NPC 对话请求观察者 / notify npc dialog request observer
		if (!getOwner().getObjectTemplate().canInteract()) {
			return;
		}
		player.getObserveController().notifyRequestDialogObservers(getOwner());
		getOwner().getAi2().onCreatureEvent(AIEventType.DIALOG_START, player);
	}

	/**
	 * 处理 NPC 对话选项选择。
	 * Handles NPC dialog option selection.
	 *
	 * dialog id
	 * 玩家 / player
	 * quest id
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @param unk 未知参数 / unknown parameter
	 */
	@Override
	public void onDialogSelect(int dialogId, final Player player, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		if (!MathUtil.isInRange(getOwner(), player, getOwner().getObjectTemplate().getTalkDistance() + 2) && !GameEngineServices.questEngine().onDialog(env)) {
			return;
		}
		if (!getOwner().getAi2().onDialogSelect(player, dialogId, questId, extendedRewardIndex)) {
			DialogService.onDialogSelect(dialogId, player, getOwner(), questId, extendedRewardIndex);
		}
	}

	/**
	 * NPC 受到攻击时的处理。
	 * Handles the NPC being attacked.
	 *
	 * attacker
	 * skill id
	 * @param type 伤害类型 / damage type
	 * damage
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 * @param log 日志类型 / log type
	 */
	@Override
	public void onAttack(Creature creature, int skillId, TYPE type, int damage, boolean notifyAttack, LOG log) {
		if (getOwner().getLifeStats().isAlreadyDead()) {
			return;
		}
		final Creature actingCreature;

		// 召唤物应获得自身仇恨 / summon should gain its own aggro
		if (creature instanceof Summon) {
			actingCreature = creature;
		}
		else {
			actingCreature = creature.getActingCreature();
		}

		super.onAttack(actingCreature, skillId, type, damage, notifyAttack, log);

		Npc npc = getOwner();

		if (actingCreature instanceof Player) {
			GameEngineServices.questEngine().onAttack(new QuestEnv(npc, (Player) actingCreature, 0, 0));
		}

		PacketSendUtility.broadcastPacket(npc, new SM_ATTACK_STATUS(npc, actingCreature, type, skillId, damage, log));
	}

	/**
	 * 停止移动时回调。
	 * Callback when movement stops.
	 *
	 */
	@Override
	public void onStopMove() {
		getOwner().getMoveController().setInMove(false);
		super.onStopMove();
	}

	/**
	 * 开始移动时回调。
	 * Callback when movement starts.
	 *
	 */
	@Override
	public void onStartMove() {
		getOwner().getMoveController().setInMove(true);
		super.onStartMove();
	}

	/**
	 * 返回出生点时回调。
	 * Callback when returning home.
	 *
	 */
	@Override
	public void onReturnHome() {
		if (getOwner().isDeleteDelayed()) {
			onDelete();
		}
		super.onReturnHome();
	}

	/**
	 * 进入区域时回调。
	 * Callback when entering a zone.
	 *
	 * zone instance
	 */
	@Override
	public void onEnterZone(ZoneInstance zoneInstance) {
		if (zoneInstance.getAreaTemplate().getZoneName() == null) {
			log.error(I18n.get("log.f297922b6249", zoneInstance.getAreaTemplate().getWorldId()));
		}
	}

	private void rewardSiegeNpc() {
		int totalDamage = getOwner().getAggroList().getTotalDamage();
		for (AggroInfo aggro : getOwner().getAggroList().getFinalDamageList(true)) {
			float percentage = aggro.getDamage() / totalDamage;
			List<Player> players = new ArrayList<Player>();
			if (aggro.getAttacker() instanceof Player) {
				Player player = (Player) aggro.getAttacker();
				if (MathUtil.isIn3dRange(player, getOwner(), GroupConfig.GROUP_MAX_DISTANCE) && !player.getLifeStats().isAlreadyDead()) {
					int apPlayerReward = Math.round(StatFunctions.calculatePvEApGained(player, getOwner()) * percentage);
					AbyssPointsService.addAp(player, getOwner(), apPlayerReward);
				}
			} else if (aggro.getAttacker() instanceof PlayerGroup) {
				PlayerGroup group = (PlayerGroup) aggro.getAttacker();
				for (Player member : group.getMembers()) {
					if (MathUtil.isIn3dRange(member, getOwner(), GroupConfig.GROUP_MAX_DISTANCE) && !member.getLifeStats().isAlreadyDead()) {
						players.add(member);
					}
				}
				if (!players.isEmpty()) {
					for (Player member : players) {
						int baseApReward = StatFunctions.calculatePvEApGained(member, getOwner());
						int apRewardPerMember = Math.round(baseApReward * percentage / players.size());
						if (apRewardPerMember > 0) {
							member.getCommonData().addAbyssFavor(1500); // 0.15% Abyss Favor Energy.
							PacketSendUtility.sendPacket(member, new SM_STATS_INFO(member));
							AbyssPointsService.addAp(member, getOwner(), apRewardPerMember);
						}
					}
				}
			} else if ((aggro.getAttacker() instanceof PlayerAlliance)) {
				PlayerAlliance alliance = (PlayerAlliance) aggro.getAttacker();
				players = new ArrayList<Player>();
				for (Player member : alliance.getMembers()) {
					if (MathUtil.isIn3dRange(member, getOwner(), GroupConfig.GROUP_MAX_DISTANCE) && !member.getLifeStats().isAlreadyDead()) {
						players.add(member);
					}
				}
				if (!players.isEmpty()) {
					for (Player member : players) {
						int baseApReward = StatFunctions.calculatePvEApGained(member, getOwner());
						int apRewardPerMember = Math.round(baseApReward * percentage / players.size());
						if (apRewardPerMember > 0) {
							member.getCommonData().addAbyssFavor(1500); // 0.15% Abyss Favor Energy.
							PacketSendUtility.sendPacket(member, new SM_STATS_INFO(member));
							AbyssPointsService.addAp(member, getOwner(), apRewardPerMember);
						}
					}
				}
			}
		}
	}

	/**
	 * 调度 NPC 重生。
	 * Schedules NPC respawn.
	 *
	 * @return respawn task Future / 重生任务 Future / respawn task Future。
	 */
	public Future<?> scheduleRespawn() {
		if (!getOwner().getSpawn().isNoRespawn()) {
			return RespawnService.scheduleRespawnTask(getOwner());
		}
		return null;
	}

	/**
	 * 获取与当前目标的攻击距离。
	 * Gets the attack distance to the current target.
	 *
	 * @return attack distance / 攻击距离 / attack distance。
	 */
	public final float getAttackDistanceToTarget() {
		return getOwner().getGameStats().getAttackRange().getCurrent() / 1000f;
	}

	/**
	 * 使用指定等级的技能。
	 * Uses a skill at the given level.
	 *
	 * skill id
	 * skill level
	 *
	 * @return whether successful / 是否成功 / whether successful。
	 */
	@Override
	public boolean useSkill(int skillId, int skillLevel) {
		SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (!getOwner().isSkillDisabled(skillTemplate)) {
			getOwner().getGameStats().renewLastSkillTime();
			return super.useSkill(skillId, skillLevel);
		}
		return false;
	}
}
