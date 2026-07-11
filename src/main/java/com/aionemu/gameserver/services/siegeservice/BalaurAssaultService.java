package com.aionemu.gameserver.services.siegeservice;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.assemblednpc.AssembledNpc;
import com.aionemu.gameserver.model.assemblednpc.AssembledNpcPart;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.assemblednpc.AssembledNpcTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_ASSEMBLER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 龙族自动突击服务：按影响力概率对要塞/神器发起突击。
 * Balaur auto-assault service that starts fortress/artifact assaults by influence chance.
 */
@Slf4j(topic = "SIEGE_LOG")
public class BalaurAssaultService {

	/** 默认单例。 / Default singleton instance. */
	private static final BalaurAssaultService instance = new BalaurAssaultService();

	/** Spring ObjectProvider override / Spring ObjectProvider override */
	private static volatile ObjectProvider<BalaurAssaultService> instanceProvider;

	/** 据点 ID → 进行中的要塞突击。 / Location id → active fortress assault. */
	private final ConcurrentMap<Integer, FortressAssault> fortressAssaults = new ConcurrentHashMap<Integer, FortressAssault>();
	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static BalaurAssaultService getInstance() {
		ObjectProvider<BalaurAssaultService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}
	/**
	 * 注入 Spring {@link ObjectProvider} 以覆盖默认单例。
	 * Injects a Spring {@link ObjectProvider} to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<BalaurAssaultService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 攻城开始时评估并可能启动龙族突击。
	 * Evaluates and may start a Balaur assault when a siege begins.
	 *
	 * @param siege 关联攻城 / related siege
	 */
	public void onSiegeStart(final Siege<?> siege) {
		int rvrId = siege.getSiegeLocationId();
		if (siege instanceof FortressSiege) {
			if (!calculateFortressAssault(((FortressSiege) siege).getSiegeLocation())) {
				return;
			}
			switch (rvrId) {
			case 1011:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <DIVINE FORTRESS> !");
					}
				});
				break;
			case 1131:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <SIEL'S WESTERN FORTRESS> !");
					}
				});
				break;
			case 1132:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <SIEL'S EASTERN FORTRESS> !");
					}
				});
				break;
			case 1141:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <SULFUR FORTRESS> !");
					}
				});
				break;
			case 1221:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <KROTAN REFUGE> !");
					}
				});
				break;
			case 1231:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <KYSIS FORTRESS> !");
					}
				});
				break;
			case 1241:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <MIREN FORTRESS> !");
					}
				});
				break;
			case 7011:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <WEALHTHEOW'S KEEP> !");
					}
				});
				break;
			case 10111:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <ARCADIAN FORTRESS> !");
					}
				});
				break;
			case 10211:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <UMBRAL FORTRESS> !");
					}
				});
				break;
			case 10311:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <ETERNUM FORTRESS> !");
					}
				});
				break;
			case 10411:
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys4Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur launch an assault on <SKYCLASH FORTRESS> !");
					}
				});
				break;
			}
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 龙族摧毁了城门。 / The Balaur have destroyed the Castle Gate.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_DRAGON_DOOR_BROKEN,
							600000);
					// 龙族摧毁了大门守护石。 / The Balaur have destroyed the Gate Guardian Stone.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_FIELDABYSS_DRAGON_REPAIR_BROKEN, 1500000);
					// 龙族摧毁了以太力场激活石。 / The Balaur have destroyed the Aetheric Field Activation Stone.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_FIELDABYSS_DRAGON_SHIELD_BROKEN, 2100000);
				}
			});
		} else if (siege instanceof ArtifactSiege) {
			if (!calculateArtifactAssault(((ArtifactSiege) siege).getSiegeLocation())) {
				return;
			}
		} else {
			return;
		}
		newAssault(siege, Rnd.get(1, 600));
		if (LoggingConfig.LOG_SIEGE) {
			log.info(I18n.get("log.3d108b62621f", siege.getSiegeLocationId()));
		}
	}

	public void onSiegeFinish(Siege<?> siege) {
		int locId = siege.getSiegeLocationId();
		FortressAssault assault = fortressAssaults.remove(locId);
		if (assault != null) {
			Boolean bossIsKilled = siege.isBossKilled();
			assault.finishAssault(bossIsKilled);
			if (bossIsKilled && siege.getSiegeLocation().getRace().equals(SiegeRace.BALAUR)) {
				log.info(I18n.get("log.9641cecdac43", siege.getSiegeLocationId()));
				switch (locId) {
				case 1011:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <DIVINE FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1131:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <SIEL'S WESTERN FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1132:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <SIEL'S EASTERN FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1141:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <SULFUR FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1221:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <KROTAN REFUGE> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1231:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <KYSIS FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 1241:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <MIREN FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 7011:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <WEALHTHEOW'S KEEP> has been captured by Balaur Assault!");
						}
					});
					break;
				case 10111:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <ARCADIAN FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 10211:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <UMBRAL FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 10311:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <ETERNUM FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				case 10411:
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys5Message(player, "\uE005",
									"[RVR/SIEGE]: <SKYCLASH FORTRESS> has been captured by Balaur Assault!");
						}
					});
					break;
				}
			} else {
				log.info(I18n.get("log.b8e0e2aae381", siege.getSiegeLocationId()));
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys6Message(player, "\uE005",
								"[RVR/SIEGE]: the Balaur failed to capture fortress");
					}
				});
			}
		}
	}
	/**
	 * 判断要塞是否可发起龙族突击（去重、影响力、同图并发上限）。
	 * Decides whether a fortress can start a Balaur assault (dedupe, influence, per-world limit).
	 *
	 * fortress location
	 *
	 * @param fortress @return 是否可突击 / whether assault may start
	 */
	private boolean calculateFortressAssault(FortressLocation fortress) {
		boolean isBalaurea = fortress.getWorldId() != 400010000;
		int locationId = fortress.getLocationId();
		if (fortressAssaults.containsKey(locationId)) {
			return false;
		}
		if (!calcFortressInfluence(isBalaurea, fortress)) {
			return false;
		}
		int count = 0;
		for (FortressAssault fa : fortressAssaults.values()) {
			if (fa.getWorldId() == fortress.getWorldId()) {
				count++;
			}
		}
		if (count >= (isBalaurea ? 1 : 2)) {
			return false;
		}
		return true;
	}
	/**
	 * 判断神器是否可发起龙族突击（当前始终 false）。
	 * Decides whether an artifact can start a Balaur assault (currently always false).
	 *
	 * artifact location
	 *
	 * @param artifact @return 是否可突击 / whether assault may start
	 */
	private boolean calculateArtifactAssault(ArtifactLocation artifact) {
		return false;
	}
	/**
	 * GM/指令手动对指定据点发起突击。
	 * Manually starts an assault on a location (GM/command).
	 *
	 * initiator
	 * location id
	 * delay in seconds
	 */
	public void startAssault(Player player, int location, int delay) {
		if (fortressAssaults.containsKey(location)) {
			PacketSendUtility.sendMessage(player, "Assault on " + location + " was already started");
			return;
		}
		newAssault(GameFeatureServices.siegeService().getSiege(location), delay);
	}

	private void newAssault(Siege<?> siege, int delay) {
		if (siege instanceof FortressSiege) {
			FortressAssault assault = new FortressAssault((FortressSiege) siege);
			int locationId = siege.getSiegeLocationId();
			if (fortressAssaults.putIfAbsent(locationId, assault) != null) {
				return;
			}
			try {
				assault.startAssault(delay);
			} catch (RuntimeException e) {
				fortressAssaults.remove(locationId, assault);
				throw e;
			}
		} else if (siege instanceof ArtifactSiege) {
			ArtifactAssault assault = new ArtifactAssault((ArtifactSiege) siege);
			assault.startAssault(delay);
		}
	}
	/**
	 * 按影响力/占有要塞数与配置概率判定是否触发突击。
	 * Rolls assault chance from influence/owned forts and config rate.
	 *
	 * @param isBalaurea 是否巴劳雷亚 / whether on Balaurea
	 * fortress location
	 * whether assault triggers
	 */
	private boolean calcFortressInfluence(boolean isBalaurea, FortressLocation fortress) {
		SiegeRace locationRace = fortress.getRace();
		if (locationRace.equals(SiegeRace.BALAUR) || !fortress.isVulnerable()) {
			return false;
		}
		int ownedForts = 0;
		float influence;
		if (isBalaurea) {
			for (FortressLocation fl : GameFeatureServices.siegeService().getFortresses().values()) {
				if (fl.getWorldId() != 400010000 && !fortressAssaults.containsKey(fl.getLocationId())
						&& fl.getRace().equals(locationRace)) {
					ownedForts++;
				}
			}
			influence = ownedForts >= 2 ? 0.25f : 0.1f;
		} else {
			influence = locationRace.equals(SiegeRace.ASMODIANS) ? GameRuntimeServices.influence().getGlobalAsmodiansInfluence()
					: GameRuntimeServices.influence().getGlobalElyosInfluence();
		}
		return Rnd.get() < influence * SiegeConfig.BALAUR_ASSAULT_RATE;
	}
	/**
	 * 向在线玩家刷出组装型德雷吉恩 NPC。
	 * Spawns an assembled dredgion NPC for all online players.
	 *
	 * assembled NPC template id
	 */
	public void spawnDredgion(int spawnId) {
		AssembledNpcTemplate template = DataManager.ASSEMBLED_NPC_DATA.getAssembledNpcTemplate(spawnId);
		List<AssembledNpcPart> assembledParts = new ArrayList<AssembledNpcPart>();
		for (AssembledNpcTemplate.AssembledNpcPartTemplate npcPart : template.getAssembledNpcPartTemplates()) {
			assembledParts.add(new AssembledNpcPart(GameWorldBootstrapServices.idFactory().nextId(), npcPart));
		}
		AssembledNpc npc = new AssembledNpc(template.getRouteId(), template.getMapId(), template.getLiveTime(),
				assembledParts);
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		Player findedPlayer;
		while (iter.hasNext()) {
			findedPlayer = iter.next();
			PacketSendUtility.sendPacket(findedPlayer, new SM_NPC_ASSEMBLER(npc));
			// 一艘战舰已出现。 / A dredgion has appeared.
			PacketSendUtility.sendPacket(findedPlayer, SM_SYSTEM_MESSAGE.STR_ABYSS_CARRIER_SPAWN);
		}
	}
}
