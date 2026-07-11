package com.aionemu.gameserver.services.base;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.base.BaseNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 据点运行时实例，管理占领、首领、袭击与相关广播。
 * Base runtime instance managing ownership, boss, assaults, and broadcasts.
 *
 * @author Rinzler
 */
public class Base<BL extends BaseLocation> {
	private Npc boss, flag;
	private boolean started;
	private final BL baseLocation;
	private Future<?> startAssault, stopAssault;
	private List<Race> list = new ArrayList<Race>();
	private List<Npc> spawned = new ArrayList<Npc>();
	private List<Npc> attackers = new ArrayList<Npc>();
	private final AtomicBoolean finished = new AtomicBoolean();
	private final BaseBossDeathListener baseBossDeathListener = new BaseBossDeathListener(this);

	/**
	 * 以据点位置模板创建运行时实例。
	 * Creates a runtime instance from a base location template.
	 *
	 * base location
	 */
	public Base(BL baseLocation) {
		list.add(Race.ASMODIANS);
		list.add(Race.ELYOS);
		list.add(Race.NPC);
		this.baseLocation = baseLocation;
	}

	/**
	 * 启动据点并刷新归属阵营单位。
	 * Starts the base and spawns owning-race units.
	 */
	public final void start() {
		boolean doubleStart = false;
		synchronized (this) {
			if (started) {
				doubleStart = true;
			} else {
				started = true;
			}
		}
		if (doubleStart) {
			return;
		}
		spawn();
	}

	/**
	 * 停止据点并清理监听与刷新单位。
	 * Stops the base and cleans listeners and spawned units.
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			if (getBoss() != null) {
				rmvBaseBossListener();
			}
			despawn(getId());
		}
	}

	private List<SpawnGroup2> getBaseSpawns() {
		List<SpawnGroup2> spawns = DataManager.SPAWNS_DATA2.getBaseSpawnsByLocId(getId());
		if (spawns == null) {
		}
		return spawns;
	}

	protected void spawn() {
		for (SpawnGroup2 group : getBaseSpawns()) {
			for (SpawnTemplate spawn : group.getSpawnTemplates()) {
				final BaseSpawnTemplate template = (BaseSpawnTemplate) spawn;
				if (template.getBaseRace().equals(getBaseLocation().getRace())) {
					if (template.getHandlerType() == null) {
						Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
						NpcTemplate npcTemplate = npc.getObjectTemplate();
						if (npcTemplate.getNpcTemplateType().equals(NpcTemplateType.FLAG)) {
							setFlag(npc);
						}
						getSpawned().add(npc);
					}
				}
			}
		}
		delayedAssault();
		delayedSpawn(getRace());
	}

	private void delayedAssault() {
		startAssault = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				chooseAttackersRace();
				sendMsgKiller(getId());
			}
		}, Rnd.get(120, 180) * 60000);
	}

	/**
	 * 按据点 ID 广播袭击/术古相关系统消息。
	 * Broadcasts assault/Shugo system messages by base id.
	 *
	 * @param id 据点 ID / base id
	 * @return 是否匹配并发送消息 / whether a message was sent
	 */
	public boolean sendMsgKiller(int id) {
		switch (id) {
		case 90:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v13);
				}
			});
			return true;
		case 91:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v04);
				}
			});
			return true;
		case 92:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v12);
				}
			});
			return true;
		case 93:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v03);
				}
			});
			return true;
		case 94:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v06);
				}
			});
			return true;
		case 95:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v05);
				}
			});
			return true;
		case 96:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v01);
				}
			});
			return true;
		case 97:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v09);
				}
			});
			return true;
		case 98:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v11);
				}
			});
			return true;
		case 99:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v10);
				}
			});
			return true;
		case 100:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v07);
				}
			});
			return true;
		case 101:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v02);
				}
			});
			return true;
		case 102:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_killer_v08);
				}
			});
			return true;
		// 术古谈判者 5.3 / Shugo Negotiator 5.3
		case 105: // Oharung At The Sulfur Archipelago.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Sulfur
						// 要塞。 / Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_01);
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Oharung at the Sulfur Tree Archipelago. As a
						// 奖励，船只支援魔族。 / reward, the ship supports the Asmodians.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_01,
								5000);
						// 奥哈隆雇佣的钢玫瑰佣兵已派往硫磺 / The Steel Rose Mercenaries hired by the Oharung were dispatched to the Sulfur
						// 树要塞。 / Tree Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_01,
								10000);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Sulfur
						// 要塞。 / Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_01);
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Oharung at the Sulfur Tree Archipelago. As a
						// 奖励，船只支援天族。 / reward, the ship supports the Elyos.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_01,
								5000);
						// 奥哈隆雇佣的钢玫瑰佣兵已派往硫磺 / The Steel Rose Mercenaries hired by the Oharung were dispatched to the Sulfur
						// 树要塞。 / Tree Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_01,
								10000);
					}
				}
			});
			return true;
		case 106: // Joarin At Zephyr Island.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Joarin at Zephyr Island. As a reward, the ship
						// 支援魔族。 / supports the Asmodians.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_02);
						// 在乔阿林支援下，你对龙族的攻击已 / With the support of the Joarin, your attacks against the Balaur have been
						// 已增强。 / bolstered.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_02,
								5000);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Joarin at Zephyr Island. As reward, the ship
						// 支援天族。 / supports the Elyos.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_02);
						// 在乔阿林支援下，你对龙族的攻击已 / With the support of the Joarin, your attacks against the Balaur have been
						// 已增强。 / bolstered.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_02,
								5000);
					}
				}
			});
			return true;
		case 107: // Temirun At Leibo Island.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Temirun at Leibo Island. As a reward, the ship
						// 支援魔族。 / supports the Asmodians.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_03);
						// 在特米伦支援下，你对龙族的攻击已 / With the support of the Temirun, your attacks against the Balaur have been
						// 已增强。 / bolstered.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_03,
								5000);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Temirun at Leibo Island. As reward, the ship
						// 支援天族。 / supports the Elyos.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_03);
						// 在特米伦支援下，你对龙族的攻击已 / With the support of the Temirun, your attacks against the Balaur have been
						// 已增强。 / bolstered.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_03,
								5000);
					}
				}
			});
			return true;
		case 108: // Shairing At Carpus Isle.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Shairing at Storm Island. As a reward, the
						// 船只支援魔族。 / ship supports the Asmodians.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_04);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Shairing at Storm Island. As reward, the ship will
						// 支援天族。 / support the Elyos.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_04);
					}
				}
			});
			return true;
		case 109: // Bomishung At Siel's Left Wing.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Siel's
						// 西部要塞。 / Western Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_02);
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Bomishung at the Siel's Left Wing. As a
						// 奖励，船只支援魔族。 / reward, the ship supports the Asmodians.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_05,
								5000);
						// 博米雄雇佣的钢玫瑰佣兵已派往希尔的 / The Steel Rose Mercenaries hired by the Bomishung were dispatched to Siel's
						// 西部要塞。 / Western Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_05,
								10000);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Siel's
						// 西部要塞。 / Western Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_02);
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Bomishung at the Siel's Left Wing. As a reward,
						// 船只支援天族。 / the ship supports the Elyos.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_05,
								5000);
						// 博米雄雇佣的钢玫瑰佣兵已派往希尔的 / The Steel Rose Mercenaries hired by the Bomishung were dispatched to Siel's
						// 西部要塞。 / Western Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_05,
								10000);
					}
				}
			});
			return true;
		case 110: // Sasming At Siel's Right Wing.
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 魔族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Asmodians have arrived at the Siel's
						// 东部要塞。 / Eastern Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_D_03);
						// 魔族已救出……作为奖励，船只 / The Asmodians have rescued the Sasming at the Siel's Right Wing. As a reward,
						// 船只支援魔族。 / the ship supports the Asmodians.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuD_06,
								5000);
						// 萨斯明雇佣的钢玫瑰佣兵已派往希尔的 / The Steel Rose Mercenaries hired by the Sasming were dispatched to Siel's
						// 东部要塞。 / Eastern Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_06,
								10000);
					} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 天族雇佣的钢玫瑰佣兵已抵达希尔的 / The Steel Rose Mercenaries hired by the Elyos have arrived at the Siel's
						// 东部要塞。 / Eastern Fortress.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoSoldier_L_03);
						// 天族已救出……作为奖励，船只 / The Elyos have rescued the Sasming at the Siel's Right Wing. As a reward, the
						// 船只支援天族。 / ship supports the Elyos.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_OccuL_06,
								5000);
						// 萨斯明雇佣的钢玫瑰佣兵已派往希尔的 / The Steel Rose Mercenaries hired by the Sasming were dispatched to Siel's
						// 东部要塞。 / Eastern Fortress.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_ShugoShip_Buff_06,
								10000);
					}
				}
			});
			return true;
		default:
			return false;
		}
	}

	private void delayedSpawn(final Race race) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (getRace().equals(race) && getBoss() == null) {
					spawnBoss();
				}
			}
		}, Rnd.get(5, 10) * 60000);
	}

	protected void spawnBoss() {
		for (SpawnGroup2 group : getBaseSpawns()) {
			for (SpawnTemplate spawn : group.getSpawnTemplates()) {
				final BaseSpawnTemplate template = (BaseSpawnTemplate) spawn;
				if (template.getBaseRace().equals(getBaseLocation().getRace())) {
					if (template.getHandlerType() != null && template.getHandlerType().equals(SpawnHandlerType.CHIEF)) {
						Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
						setBoss(npc);
						addBaseBossListeners();
						getSpawned().add(npc);
					}
				}
			}
		}
	}

	protected void chooseAttackersRace() {
		AtomicBoolean next = new AtomicBoolean(Math.random() < 0.5);
		for (Race race : list) {
			if (!race.equals(getRace())) {
				if (next.compareAndSet(true, false)) {
					continue;
				}
				spawnAttackers(race);
				if (baseLocation.getWorldId() == 400010000) {
					// 霜响前哨。 / Rattlefrost Outpost.
					if (getBaseLocation().getId() == 53) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 碎雪前哨。 / Sliversleet Outpost.
					if (getBaseLocation().getId() == 54) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 寒锻前哨。 / Coldforge Outpost.
					if (getBaseLocation().getId() == 55) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 微光霜前哨。 / Shimmerfrost Outpost.
					if (getBaseLocation().getId() == 56) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 冰嚎前哨。 / Icehowl Outpost.
					if (getBaseLocation().getId() == 57) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 寒魂前哨。 / Chillhaunt Outpost.
					if (getBaseLocation().getId() == 58) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 吞烟前哨。 / Sootguzzle Outpost.
					if (getBaseLocation().getId() == 59) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 焰毁前哨。 / Flameruin Outpost.
					if (getBaseLocation().getId() == 60) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 炉风前哨。 / Stokebellow Outpost.
					if (getBaseLocation().getId() == 61) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 焰架前哨。 / Blazerack Outpost.
					if (getBaseLocation().getId() == 62) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 闷燃幽灵前哨。 / Smoldergeist Outpost.
					if (getBaseLocation().getId() == 63) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
					// 熔刺前哨。 / Moltenspike Outpost.
					if (getBaseLocation().getId() == 64) {
						if (race == Race.ASMODIANS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									false);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									true);
						} else if (race == Race.ELYOS) {
							GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(6000, LandingPointsEnum.BASE,
									true);
							GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(6000, LandingPointsEnum.BASE,
									false);
						}
					}
				}
				break;
			}
		}
	}

	/**
	 * 按指定阵营刷新袭击单位。
	 * Spawns assault attackers for the given race.
	 *
	 * @param race 袭击阵营 / attacking race
	 */
	public void spawnAttackers(Race race) {
		if (getFlag() == null) {
		} else if (!getFlag().getPosition().getMapRegion().isMapRegionActive()) {
			if (Math.random() < 0.5) {
				GameFeatureServices.baseService().capture(getId(), race);
			} else {
				delayedAssault();
			}
			return;
		}
		if (!isAttacked()) {
			despawnAttackers();
			for (SpawnGroup2 group : getBaseSpawns()) {
				for (SpawnTemplate spawn : group.getSpawnTemplates()) {
					final BaseSpawnTemplate template = (BaseSpawnTemplate) spawn;
					if (template.getBaseRace().equals(race)) {
						if (template.getHandlerType() != null
								&& template.getHandlerType().equals(SpawnHandlerType.SLAYER)) {
							Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
							getAttackers().add(npc);
						}
					}
				}
			}
			if (getAttackers().isEmpty()) {
			} else {
				stopAssault = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnAttackers();
						delayedAssault();
					}
				}, 5 * 60000);
			}
		}
	}

	/**
	 * 是否仍有存活的袭击单位。
	 * Whether any assault attackers are still alive.
	 *
	 * @return 被袭击中为 true / true if under attack
	 */
	public boolean isAttacked() {
		for (Npc attacker : getAttackers()) {
			if (!attacker.getLifeStats().isAlreadyDead()) {
				return true;
			}
		}
		return false;
	}

	protected void despawn(int baseLocationId) {
		setFlag(null);
		Collection<BaseNpc> baseNpcs = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getLocalBaseNpcs(baseLocationId);
		for (BaseNpc npc : new ArrayList<BaseNpc>(baseNpcs)) {
			npc.getController().onDelete();
		}
		if (startAssault != null) {
			startAssault.cancel(true);
		}
		if (stopAssault != null) {
			stopAssault.cancel(true);
			despawnAttackers();
		}
	}

	protected void despawnAttackers() {
		for (Npc attacker : new ArrayList<Npc>(getAttackers())) {
			attacker.getController().onDelete();
		}
		getAttackers().clear();
	}

	protected void addBaseBossListeners() {
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		EnhancedObject eo = (EnhancedObject) ai;
		eo.addCallback(getBaseBossDeathListener());
	}

	protected void rmvBaseBossListener() {
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		EnhancedObject eo = (EnhancedObject) ai;
		eo.removeCallback(getBaseBossDeathListener());
	}

	/**
	 * 获取据点旗帜 NPC。
	 * Returns the base flag NPC.
	 *
	 * flag NPC
	 */
	public Npc getFlag() {
		return flag;
	}

	/**
	 * 设置据点旗帜 NPC。
	 * Sets the base flag NPC.
	 *
	 * flag NPC
	 */
	public void setFlag(Npc flag) {
		this.flag = flag;
	}

	/**
	 * 获取据点首领 NPC。
	 * Returns the base boss NPC.
	 *
	 * boss NPC
	 */
	public Npc getBoss() {
		return boss;
	}

	/**
	 * 设置据点首领 NPC。
	 * Sets the base boss NPC.
	 *
	 * boss NPC
	 */
	public void setBoss(Npc boss) {
		this.boss = boss;
	}

	/**
	 * 获取首领死亡监听器。
	 * Returns the boss death listener.
	 *
	 * @return 死亡监听器 / death listener
	 */
	public BaseBossDeathListener getBaseBossDeathListener() {
		return baseBossDeathListener;
	}

	/**
	 * 据点是否已结束。
	 * Whether the base instance is finished.
	 *
	 * @return 若 finished 则为 true / true if finished
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 获取据点位置模板。
	 * Returns the base location template.
	 *
	 * base location
	 */
	public BL getBaseLocation() {
		return baseLocation;
	}

	/**
	 * 获取据点 ID。
	 * Returns the base id.
	 *
	 * base id
	 */
	public int getId() {
		return baseLocation.getId();
	}

	/**
	 * 获取当前占领阵营。
	 * Returns the current owning race.
	 *
	 * owning race
	 */
	public Race getRace() {
		return baseLocation.getRace();
	}

	/**
	 * 设置当前占领阵营。
	 * Sets the current owning race.
	 *
	 * @param race 占领阵营 / owning race
	 */
	public void setRace(Race race) {
		baseLocation.setRace(race);
	}

	/**
	 * 获取当前袭击单位列表。
	 * Returns the current attacker list.
	 *
	 * attackers
	 */
	public List<Npc> getAttackers() {
		return attackers;
	}

	/**
	 * 获取已刷新单位列表。
	 * Returns the spawned unit list.
	 *
	 * @return 已刷新单位 / spawned units
	 */
	public List<Npc> getSpawned() {
		return spawned;
	}
}
