package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 自由混战（FFA）竞技场服务，管理地图轮换、进出场、击杀奖励与连杀播报。
 * Free-for-all (FFA) arena service managing map rotation, enter/leave, kill rewards, and kill-streak announcements.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class FFAService {
	/** Spring 实例提供者 / Spring instance provider */
	private static volatile ObjectProvider<FFAService> instanceProvider;
	/** Worldpositionscached 前 enteringFFA / World positions cached before entering FFA */
	private Map<Integer, WorldPosition> previousLocations = new HashMap<Integer, WorldPosition>();
	/** 当前活跃竞技场实例。 / Currently active arena instance. */
	private WorldMapInstance activeInstance;
	/** 可用竞技场地图列表。 / Available arena map list. */
	private List<ArenaMap> maps = new ArrayList<ArenaMap>();
	/** 当前活跃竞技场地图。 / Currently active arena map. */
	private ArenaMap activeMap = null;
	/** 定时任务秒计数器。 / Periodic task second counter. */
	private int incrementCounter = 0;
	@SuppressWarnings("unused")
	/** 当前实例静态门（预留）。 / Current instance static doors (reserved). */
	private Map<Integer, StaticDoor> doors;
	private Object UnsummonType;
	/** 服务是否已启用。 / Whether the service is enabled. */
	private static boolean isAvailable;


	/**
	 * 初始化 FFA：加载竞技场地图并启动周期调度（人数播报 / 地图轮换 / 全服邀请）。
	 * map rotation / world invites).
	 */
	public void init() {
		if (!FFAConfig.FFA_ENABLED) {
			log.info(I18n.get("log.4d38a9ff446c"));
			isAvailable = false;
			return;
		}
		
		log.info(I18n.get("log.86a16ce407e0"));
		isAvailable = true;
		
		// 诺克萨纳训练营。 / Nochsana Training Camp.
		maps.add(new ArenaMap(300030000, 99,
				Arrays.asList(new Float[] { 331f, 272f, 384f }, new Float[] { 314f, 325f, 380f },
						new Float[] { 366f, 320f, 380f }, new Float[] { 308f, 280f, 392f },
						new Float[] { 356f, 271f, 392f }, new Float[] { 343f, 339f, 379f })));
		// 卡普斯岛仓库。 / Carpus Isle Storeroom.
		maps.add(new ArenaMap(300050000, 99,
				Arrays.asList(new Float[] { 484f, 566f, 201f }, new Float[] { 524f, 591f, 199f },
						new Float[] { 556f, 564f, 198f }, new Float[] { 521f, 536f, 199f },
						new Float[] { 514f, 564f, 197f }, new Float[] { 530f, 563f, 197f })));
		// 硫磺树巢。 / Sulfur Tree Nest.
		maps.add(new ArenaMap(300060000, 99,
				Arrays.asList(new Float[] { 476f, 418f, 163f }, new Float[] { 435f, 440f, 162f },
						new Float[] { 430f, 486f, 162f }, new Float[] { 480f, 504f, 162f },
						new Float[] { 485f, 460f, 162f }, new Float[] { 453f, 472f, 163f })));
		// 哈马特岛仓库。 / Hamate Isle Storeroom.
		maps.add(new ArenaMap(300070000, 99,
				Arrays.asList(new Float[] { 504f, 423f, 91f }, new Float[] { 503f, 460f, 86f },
						new Float[] { 481f, 483f, 87f }, new Float[] { 528f, 483f, 87f },
						new Float[] { 504f, 503f, 88f }, new Float[] { 503f, 479f, 87f })));
		// 左翼室。 / Left Wing Chamber.
		maps.add(new ArenaMap(300080000, 99,
				Arrays.asList(new Float[] { 488f, 512f, 352f }, new Float[] { 495f, 548f, 354f },
						new Float[] { 458f, 530f, 352f }, new Float[] { 485f, 585f, 355f },
						new Float[] { 495f, 623f, 354f }, new Float[] { 451f, 618f, 352f })));
		// 钢耙号。 / Steel Rake.
		maps.add(new ArenaMap(300100000, 99,
				Arrays.asList(new Float[] { 568f, 489f, 1023f }, new Float[] { 568f, 528f, 1023f },
						new Float[] { 544f, 527f, 1023f }, new Float[] { 545f, 489f, 1023f },
						new Float[] { 592f, 489f, 1023f }, new Float[] { 592f, 528f, 1023f })));
		// 巴拉纳斯战舰。 / Baranath Dredgion.
		maps.add(new ArenaMap(300110000, 99,
				Arrays.asList(new Float[] { 485f, 857f, 417f }, new Float[] { 485f, 877f, 405f },
						new Float[] { 513f, 889f, 405f }, new Float[] { 457f, 889f, 405f },
						new Float[] { 485f, 909f, 405f }, new Float[] { 485f, 814f, 416f })));
		// 钢铁之墓仓库。 / Grave Of Steel Storeroom.
		maps.add(new ArenaMap(300120000, 99,
				Arrays.asList(new Float[] { 496f, 826f, 199f }, new Float[] { 492f, 851f, 199f },
						new Float[] { 504f, 873f, 199f }, new Float[] { 528f, 881f, 199f },
						new Float[] { 552f, 873f, 199f }, new Float[] { 564f, 851f, 199f })));
		// 暮光战场仓库。 / Twilight Battlefield Storeroom.
		maps.add(new ArenaMap(300130000, 99,
				Arrays.asList(new Float[] { 496f, 826f, 199f }, new Float[] { 492f, 851f, 199f },
						new Float[] { 504f, 873f, 199f }, new Float[] { 528f, 881f, 199f },
						new Float[] { 552f, 873f, 199f }, new Float[] { 564f, 851f, 199f })));
		// 根之岛仓库。 / Isle Of Roots Storeroom.
		maps.add(new ArenaMap(300140000, 99,
				Arrays.asList(new Float[] { 496f, 826f, 199f }, new Float[] { 492f, 851f, 199f },
						new Float[] { 504f, 873f, 199f }, new Float[] { 528f, 881f, 199f },
						new Float[] { 552f, 873f, 199f }, new Float[] { 564f, 851f, 199f })));
		// 下乌达斯神殿。 / Lower Udas Temple.
		maps.add(new ArenaMap(300160000, 99,
				Arrays.asList(new Float[] { 571f, 1297f, 187f }, new Float[] { 566f, 1242f, 188f },
						new Float[] { 572f, 1344f, 188f }, new Float[] { 636f, 1385f, 186f },
						new Float[] { 658f, 1297f, 186f }, new Float[] { 640f, 1215f, 186f })));
		// 贝斯蒙迪尔神殿。 / Beshmundir Temple.
		maps.add(new ArenaMap(300170000, 99,
				Arrays.asList(new Float[] { 1505f, 1463f, 304f }, new Float[] { 1441f, 1378f, 305f },
						new Float[] { 1511f, 1385f, 307f }, new Float[] { 1428f, 1448f, 307f },
						new Float[] { 1533f, 1433f, 300f }, new Float[] { 1468f, 1483f, 300f })));
		// 塔洛克空洞。 / Taloc's Hollow.
		maps.add(new ArenaMap(300190000, 99,
				Arrays.asList(new Float[] { 392f, 897f, 1266f }, new Float[] { 442f, 919f, 1274f },
						new Float[] { 434f, 878f, 1276f }, new Float[] { 387f, 862f, 1264f },
						new Float[] { 429f, 934f, 1266f }, new Float[] { 382f, 842f, 1271f })));
		// 哈拉梅尔。 / Haramel.
		maps.add(new ArenaMap(300200000, 99,
				Arrays.asList(new Float[] { 387f, 315f, 88f }, new Float[] { 376f, 285f, 89f },
						new Float[] { 347f, 287f, 90f }, new Float[] { 344f, 331f, 87f },
						new Float[] { 356f, 367f, 90f }, new Float[] { 327f, 380f, 89f })));
		// 钱特拉战舰。 / Chantra Dredgion.
		maps.add(new ArenaMap(300210000, 99,
				Arrays.asList(new Float[] { 458f, 493f, 397f }, new Float[] { 514f, 493f, 397f },
						new Float[] { 486f, 455f, 398f }, new Float[] { 484f, 527f, 396f },
						new Float[] { 483f, 496f, 397f }, new Float[] { 484f, 420f, 398f })));
		// 克罗梅德试炼。 / Kromede Trial.
		maps.add(new ArenaMap(300230000, 99,
				Arrays.asList(new Float[] { 528f, 640f, 201f }, new Float[] { 493f, 640f, 201f },
						new Float[] { 513f, 610f, 201f }, new Float[] { 512f, 670f, 201f },
						new Float[] { 557f, 640f, 206f }, new Float[] { 531f, 612f, 201f })));
		// 伊索特拉斯。 / Esoterrace.
		maps.add(new ArenaMap(300250000, 99,
				Arrays.asList(new Float[] { 1254f, 624f, 296f }, new Float[] { 1217f, 620f, 295f },
						new Float[] { 1230f, 664f, 298f }, new Float[] { 1249f, 695f, 299f },
						new Float[] { 1286f, 675f, 296f }, new Float[] { 1294f, 623f, 297f })));
		// 特拉斯战舰。 / Terath Dredgion.
		maps.add(new ArenaMap(300440000, 99,
				Arrays.asList(new Float[] { 443f, 321f, 403f }, new Float[] { 484f, 342f, 403f },
						new Float[] { 485f, 297f, 402f }, new Float[] { 529f, 323f, 403f },
						new Float[] { 485f, 314f, 403f }, new Float[] { 424f, 300f, 409f })));
		// 封印的达努阿尔秘术馆。 / Sealed Danuar Mysticarium.
		maps.add(new ArenaMap(300480000, 99,
				Arrays.asList(new Float[] { 189f, 180f, 239f }, new Float[] { 152f, 194f, 239f },
						new Float[] { 154f, 219f, 239f }, new Float[] { 190f, 207f, 238f },
						new Float[] { 188f, 244f, 240f }, new Float[] { 230f, 208f, 239f })));
		// 永恒堡垒。 / Eternal Bastion.
		// maps.add(new ArenaMap(300540000, 99, Arrays.asList(new Float[]{740f, 255f,
		// 253f}, new Float[]{778f, 288f, 253f}, new Float[]{754f, 336f, 253f},
		// new Float[]{717f, 321f, 252f}, new Float[]{698f, 287f, 253f}, new
		// Float[]{766f, 266f, 233f})));
		// 六角道。 / The Hexway.
		maps.add(new ArenaMap(300700000, 99,
				Arrays.asList(new Float[] { 488f, 512f, 352f }, new Float[] { 495f, 548f, 354f },
						new Float[] { 458f, 530f, 352f }, new Float[] { 485f, 585f, 355f },
						new Float[] { 495f, 623f, 354f }, new Float[] { 451f, 618f, 352f })));
		// 卡玛尔战场。 / Kamar Battlefield.
		maps.add(new ArenaMap(301120000, 99,
				Arrays.asList(new Float[] { 1344f, 1528f, 595f }, new Float[] { 1313f, 1510f, 597f },
						new Float[] { 1313f, 1460f, 597f }, new Float[] { 1387f, 1513f, 597f },
						new Float[] { 1370f, 1460f, 599f }, new Float[] { 1396f, 1423f, 600f })));
		// 被吞没的奥菲丹桥。 / Engulfed Ophidan Bridge.
		maps.add(new ArenaMap(301210000, 99,
				Arrays.asList(new Float[] { 499f, 523f, 597f }, new Float[] { 527f, 541f, 604f },
						new Float[] { 494f, 550f, 597f }, new Float[] { 434f, 495f, 600f },
						new Float[] { 474f, 490f, 597f }, new Float[] { 448f, 537f, 599f })));
		// 铁壁战线。 / Iron Wall Warfront.
		maps.add(new ArenaMap(301220000, 99,
				Arrays.asList(new Float[] { 491f, 765f, 200f }, new Float[] { 552f, 744f, 197f },
						new Float[] { 591f, 777f, 187f }, new Float[] { 565f, 807f, 188f },
						new Float[] { 599f, 823f, 187f }, new Float[] { 612f, 776f, 185f })));
		// 伊杰尔穹顶。 / Idgel Dome.
		maps.add(new ArenaMap(301310000, 99,
				Arrays.asList(new Float[] { 252f, 246f, 92f }, new Float[] { 276f, 272f, 92f },
						new Float[] { 226f, 258f, 89f }, new Float[] { 302f, 258f, 89f },
						new Float[] { 248f, 289f, 89f }, new Float[] { 277f, 225f, 89f })));
		// 龙脊深渊。 / Drakenspire Depths.
		maps.add(new ArenaMap(301390000, 99,
				Arrays.asList(new Float[] { 208f, 542f, 1754f }, new Float[] { 176f, 579f, 1760f },
						new Float[] { 127f, 575f, 1754f }, new Float[] { 128f, 461f, 1754f },
						new Float[] { 177f, 458f, 1759f }, new Float[] { 208f, 496f, 1754f })));
		// 永恒摇篮。 / Cradle Of Eternity.
		maps.add(new ArenaMap(301550000, 99,
				Arrays.asList(new Float[] { 464f, 1398f, 827f }, new Float[] { 474f, 1418f, 827f },
						new Float[] { 510f, 1387f, 823f }, new Float[] { 430f, 1429f, 823f },
						new Float[] { 449f, 1374f, 823f }, new Float[] { 491f, 1445f, 823f })));
		// 永恒摇篮【纪念之路】 / Cradle Of Eternity [Memorial Path]
		maps.add(new ArenaMap(301550000, 99,
				Arrays.asList(new Float[] { 602f, 806f, 565f }, new Float[] { 626f, 768f, 561f },
						new Float[] { 629f, 717f, 555f }, new Float[] { 738f, 727f, 546f },
						new Float[] { 685f, 721f, 548f }, new Float[] { 709f, 772f, 547f })));
		// 龙视者之巢。 / Drakenseer's Lair.
		maps.add(new ArenaMap(301620000, 99,
				Arrays.asList(new Float[] { 276f, 342f, 336f }, new Float[] { 328f, 309f, 318f },
						new Float[] { 350f, 266f, 318f }, new Float[] { 330f, 204f, 319f },
						new Float[] { 266f, 197f, 319f }, new Float[] { 237f, 292f, 318f })));
		// 陨落波埃塔。 / Fallen Poeta.
		maps.add(new ArenaMap(301660000, 99,
				Arrays.asList(new Float[] { 216f, 348f, 130f }, new Float[] { 235f, 382f, 124f },
						new Float[] { 183f, 334f, 123f }, new Float[] { 175f, 379f, 120f },
						new Float[] { 221f, 400f, 118f }, new Float[] { 193f, 393f, 119f })));
		// 奥菲丹战道。 / Ophidan Warpath.
		maps.add(new ArenaMap(301670000, 99,
				Arrays.asList(new Float[] { 697f, 466f, 599f }, new Float[] { 676f, 495f, 599f },
						new Float[] { 665f, 449f, 600f }, new Float[] { 570f, 412f, 610f },
						new Float[] { 599f, 395f, 609f }, new Float[] { 620f, 423f, 607f })));
		// 遗忘裂隙。 / Fissure Of Oblivion.
		maps.add(new ArenaMap(302100000, 99,
				Arrays.asList(new Float[] { 326f, 512f, 352f }, new Float[] { 278f, 513f, 351f },
						new Float[] { 300f, 531f, 350f }, new Float[] { 301f, 496f, 350f },
						new Float[] { 290f, 527f, 350f }, new Float[] { 312f, 499f, 350f })));
		// 因德拉图要塞。 / Indratu Fortress.
		maps.add(new ArenaMap(310090000, 99,
				Arrays.asList(new Float[] { 604f, 466f, 1019f }, new Float[] { 617f, 516f, 1019f },
						new Float[] { 575f, 540f, 1013f }, new Float[] { 566f, 507f, 1012f },
						new Float[] { 552f, 479f, 1011f }, new Float[] { 615f, 562f, 1018f })));
		// 阿佐图兰要塞。 / Azoturan Fortress.
		maps.add(new ArenaMap(310100000, 99,
				Arrays.asList(new Float[] { 462f, 442f, 993f }, new Float[] { 417f, 402f, 1004f },
						new Float[] { 425f, 398f, 991f }, new Float[] { 459f, 392f, 991f },
						new Float[] { 413f, 426f, 991f }, new Float[] { 443f, 419f, 991f })));
		// 火神殿。 / Fire Temple.
		maps.add(new ArenaMap(320100000, 99,
				Arrays.asList(new Float[] { 414f, 97f, 117f }, new Float[] { 392f, 88f, 117f },
						new Float[] { 411f, 120f, 117f }, new Float[] { 392f, 128f, 117f },
						new Float[] { 377f, 99f, 117f }, new Float[] { 361f, 126f, 116f })));
		// 帕德玛拉什卡洞穴。 / Padmarashka's Cave.
		maps.add(new ArenaMap(320150000, 99,
				Arrays.asList(new Float[] { 576f, 279f, 66f }, new Float[] { 605f, 235f, 66f },
						new Float[] { 578f, 206f, 66f }, new Float[] { 537f, 209f, 66f },
						new Float[] { 524f, 239f, 66f }, new Float[] { 535f, 279f, 66f })));
		// 特兰西迪姆附楼。 / Transidium Annex.
		// maps.add(new ArenaMap(400030000, 99, Arrays.asList(new Float[]{481f, 500f,
		// 674f}, new Float[]{480f, 524f, 674f}, new Float[]{497f, 541f, 674f},
		// new Float[]{521f, 542f, 674f}, new Float[]{538f, 524f, 674f}, new
		// Float[]{538f, 500f, 674f}, new Float[]{521f, 483f, 674f}, new Float[]{497f,
		// 483f, 674f})));
		pickArenaMap();
		activeInstance = getWorldMap().getMainWorldMapInstance();
		doors = activeInstance.getDoors();
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				incrementCounter++;
				if ((incrementCounter % 300) == 0) {
					announcePlayerCount();
				}
				if ((incrementCounter % 180) == 0) {
					announcePlayerCount();
				}
				if ((incrementCounter % 900) == 0) {
					final int players = activeInstance.getPlayersInside().size();
					if (players > 0) {
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player pl) {
								if (!isInArena(pl) && pl.getBattleground() == null) {
									PacketSendUtility.sendSys3Message(pl, "\uE00B", "<FFA> Join the <FFA> map in writing: .ffa and play with " + players + " other players right now!!!");
								}
							}
						});
					}
				}
				if ((incrementCounter % 600) == 0) { // Change map every 10 Min.
					pickArenaMap();
				}
				if ((incrementCounter % 3600) == 0) {
					incrementCounter = 0;
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player pl) {
							if (!isInArena(pl) && pl.getBattleground() == null) {
								PacketSendUtility.sendSys3Message(pl, "\uE00B", "<FFA> Join the <FFA> area, and try to win AP/GP. Write: .ffa!!!");
							}
						}
					});
				}
			}
		}, 1 * 1000, 1 * 1000);
	}

	/**
	 * 随机挑选下一张竞技场地图；若已有活跃地图则把场内玩家重新送入新图。
	 * Picks the next arena map at random; if a map was already active, re-enters all players into the new map.
	 *
	 * @return 是否成功切换到新地图 / whether a new map was selected
	 */
	public boolean pickArenaMap() {
		if (maps.size() == 0) {
			return false;
		}
		if (maps.size() == 1) {
			activeMap = maps.get(0);
			analyseInstanceBalance();
			return false;
		}
		List<ArenaMap> mapsWithoutActive = new ArrayList<ArenaMap>(maps.size());
		mapsWithoutActive.addAll(maps);
		if (activeMap != null) {
			mapsWithoutActive.remove(activeMap);
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				final String msg = "Map loading, please wait...";
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendMessage(pl, msg);
						enterArena(pl, true);
					}
				});
			}
		}
		activeMap = mapsWithoutActive.get(Rnd.get(mapsWithoutActive.size()));
		activeInstance = getWorldMap().getMainWorldMapInstance();
		return true;
	}

	/** 返回当前活跃地图的世界地图对象。 / Returns the world map for the active arena map. */
	private WorldMap getWorldMap() {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(activeMap.getMapId());
	}

	/** 向场内玩家播报当前人数。 / Announces the current player count to arena players. */
	private void announcePlayerCount() {
		for (WorldMapInstance instance : getWorldMap().getInstances()) {
			final String msg = "[FFA] There are currently: " + instance.getPlayersInside().size() + " player's on the map.";
			instance.doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player pl) {
					PacketSendUtility.sendMessage(pl, msg);
				}
			});
		}
	}

	/**
	 * 向场内广播击杀职业信息。
	 * Broadcasts killer/victim class info inside the arena.
	 *
	 * victim
	 * killer
	 */
	public void announceKill(Player victim, Player killer) {
		for (WorldMapInstance instance : getWorldMap().getInstances()) {
			final String msg = killer.getPlayerClass() + " has killed " + victim.getPlayerClass() + "!";
			instance.doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player pl) {
					PacketSendUtility.sendSys3Message(pl, "\uE00B", msg);
				}
			});
		}
	}

	/**
	 * 处理 FFA 内死亡：清理状态、奖励击杀者并延迟复活传送。
	 * Handles death inside FFA: cleans state, rewards the killer, and delayed-revives/teleports.
	 *
	 * dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public void onDie(final Player player, Creature lastAttacker) {
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, lastAttacker == null ? 0 : lastAttacker.getObjectId()), true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DEATH_MESSAGE_ME);
		player.getMoveController().abortMove();
		player.setState(CreatureState.DEAD);
		player.getObserveController().notifyDeathObservers(player);
		player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
		player.getEffectController().removeEffectByDispelCat(DispelCategoryType.ALL, SkillTargetSlot.DEBUFF, 100, 2, 100, false);
		player.setTarget(null);
		PacketSendUtility.sendPacket(player, new SM_TARGET_SELECTED(player));
		if (lastAttacker instanceof Player) {
			rewardKiller(player, (Player) lastAttacker);
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (isInArena(player) && player.isFFA()) {
					if (player.getLifeStats().isAlreadyDead()) {
						PlayerReviveService.ffaRevive(player);
					}
					Float[] spawn = getRandomSpawn();
					TeleportService2.teleportTo(player, getWorldMap().getMapId(), player.getInstanceId(), spawn[0], spawn[1], spawn[2]);
				}
			}
		}, 6000);
	}

	/**
	 * 奖励击杀者（AP/GP/回血/连杀），并广播击杀消息。
	 * Rewards the killer (AP/GP/heal/streak) and broadcasts the kill message.
	 *
	 * victim
	 * killer
	 */
	public void rewardKiller(Player player, Player killer) {
		if (killer == null || killer == player) {
			return;
		}
		killer.setKillStreak(killer.getKillStreak() + 1);
		checkKillerLevel(killer);
		// 奖励 AP。 / Reward AP.
		AbyssPointsService.addAp(player, 0);
		AbyssPointsService.addAp(killer, 5000);
		// 奖励 GP。 / Reward GP.
		AbyssPointsService.addGp(killer, 10);
		player.setKillStreak(0);
		killer.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 1500);
		killer.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, 1500);
		killer.getCommonData().setDp(500 + killer.getCommonData().getDp());
		for (WorldMapInstance instance : getWorldMap().getInstances()) {
			final String msg = killer.getName() + " has killed " + player.getName() + "!";
			instance.doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player pl) {
					PacketSendUtility.sendSys3Message(pl, "\uE005", msg);
				}
			});
		}
	}

	/**
	 * 检查并处理连杀里程碑奖励（物品 / 点数 / 全场播报）。
	 * toll / arena announce).
	 *
	 * killer
	 */
	public void checkKillerLevel(Player player) {
		if (player.getKillStreak() == 5) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_1;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 10) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_2;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 15) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_3;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 20) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_4;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 25) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_5;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 30) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_6;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 35) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_7;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 40) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_8;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() == 45) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_9;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
		if (player.getKillStreak() >= 50 && player.getKillStreak() <= 999) {
			for (WorldMapInstance instance : getWorldMap().getInstances()) {
				ItemService.addItem(player, FFAConfig.FFA_SPREE_REWARD_ITEM, 1);
				GameRuntimeServices.inGameShopEn().addToll(player, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
				PacketSendUtility.sendMessage(player, "You've received " + FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY + " tolls from FFA!");
				final String msg = player.getName() + FFAConfig.FFA_SPREE_10;
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player pl) {
						PacketSendUtility.sendSys3Message(pl, "\uE07e", msg);
					}
				});
			}
		}
	}

	/**
	 * 判断玩家是否处于任一 FFA 竞技场地图。
	 * Returns whether the player is on any FFA arena map.
	 *
	 * @param player 玩家 / player
	 * @return 是否在竞技场 / whether in arena
	 */
	public boolean isInArena(Player player) {
		for (ArenaMap arenaMap : maps) {
			if (arenaMap.getMapId() == player.getWorldId()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 延迟进入 FFA：睡眠保护后脱队、标记 FFA 并传送到随机出生点。
	 * Delayed FFA entry: sleep-protects, leaves party, marks FFA, and teleports to a random spawn.
	 *
	 * @param player 玩家 / player
	 * @param isMapRotation 是否地图轮换进入（不缓存原坐标） / whether entry is due to map rotation (skip caching origin)
	 */
	public void enterArena(final Player player, final boolean isMapRotation) {
		player.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		if (!isMapRotation) {
			previousLocations.put(player.getObjectId(), player.getPosition().clone());
		}
		final ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {
			@Override
			public void attacked(Creature creature) {
				if (player.getController().hasTask(TaskId.FFA)) {
					player.getController().cancelTask(TaskId.FFA);
					player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
					player.getEffectController().updatePlayerEffectIcons();
					player.getEffectController().broadCastEffects();
				}
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.FFA, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				if (player.getLifeStats().isAlreadyDead()) {
					PlayerReviveService.skillRevive(player);
				}
				if (player.isInGroup2()) {
					PlayerGroupService.removePlayer(player);
				}
				if (player.isInAlliance2()) {
					PlayerAllianceService.removePlayer(player);
				}
				player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
				player.getEffectController().updatePlayerEffectIcons();
				player.getEffectController().broadCastEffects();
				player.getCommonData().setDp(0);
				player.setFFA(true);
				analyseInstanceBalance();
				Float[] spawn = getRandomSpawn();
				// sendEventPacket(StageType.PVP_STAGE_1, 0);
				TeleportService2.teleportTo(player, getWorldMap().getMapId(), activeInstance.getInstanceId(), spawn[0],
						spawn[1], spawn[2]);
			}
		}, 10 * 1000));
	}

	/**
	 * 延迟离开 FFA：解除标记并传送回进入前位置（或绑定点）。
	 * Delayed FFA leave: clears FFA flag and teleports back to the previous position (or bind point).
	 *
	 * @param player 玩家 / player
	 */
	public void leaveArena(final Player player) {
		final WorldPosition pos = previousLocations.remove(player.getObjectId());
		player.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (player.getLifeStats().isAlreadyDead()) {
					PlayerReviveService.skillRevive(player);
				}
				player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
				player.getEffectController().updatePlayerEffectIcons();
				player.getEffectController().broadCastEffects();
				player.setFFA(false);
				if (pos != null) {
					TeleportService2.teleportTo(player, pos.getMapId(), pos.getX(), pos.getY(), pos.getZ());
				} else {
					TeleportService2.moveToBindLocation(player, true);
				}
			}
		}, 10 * 1000);
	}

	/** 从当前地图随机取一个出生点坐标。 / Picks a random spawn coordinate from the active map. */
	private Float[] getRandomSpawn() {
		return activeMap.getSpawns().get(Rnd.get(activeMap.getSpawns().size()));
	}

	/** 选择人数未满的实例；若皆满则创建新战场实例。 / Picks an under-cap instance or creates a new BG instance. */
	private void analyseInstanceBalance() {
		for (WorldMapInstance instance : getWorldMap().getInstances()) {
			if (instance.getPlayersInside().size() < activeMap.getPlayerCap()) {
				activeInstance = instance;
			}
		}
		if (activeInstance == null || activeInstance.getPlayersInside().size() >= activeMap.getPlayerCap()) {
			activeInstance = InstanceService.getNextBgInstance(getWorldMap().getMapId());
		}
	}

	/**
	 * 判断给定实例是否为当前活跃 FFA 实例。
	 * Returns whether the given instance is the active FFA instance.
	 *
	 * @param instance 世界地图实例 / world map instance
	 * @return 是否为活跃实例 / whether active instance
	 */
	public boolean isActiveInstance(WorldMapInstance instance) {
		return instance.getInstanceId() == activeInstance.getInstanceId();
	}

	/**
	 * 判断给定实例是否位于当前活跃 FFA 地图。
	 * Returns whether the given instance is on the active FFA world map.
	 *
	 * @param instance 世界地图实例 / world map instance
	 * @return 是否为活跃地图 / whether active world map
	 */
	public boolean isActiveWorld(WorldMapInstance instance) {
		return instance.getMapId() == activeInstance.getMapId();
	}

	/**
	 * 返回 FFA 内用于显示的目标名称（职业名）。
	 * Returns the display name used for a target inside FFA (player class name).
	 *
	 * viewer
	 * target
	 * display name
	 */
	public String getName(Player player, Player target) {
		String FFAplayerName = target.getPlayerClass().name();
		return FFAplayerName;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers the Spring provider).
	 *
	 * service instance
	 */
	public static final FFAService getInstance() {
		ObjectProvider<FFAService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<FFAService> instanceProvider) {
		FFAService.instanceProvider = instanceProvider;
	}

	/**
	 * 懒加载单例持有者。
	 * Lazy singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	public static class SingletonHolder {
		protected static final FFAService instance = new FFAService();
	}

	/**
	 * FFA 竞技场地图定义（地图 ID、人数上限、出生点）。
	 * FFA arena map definition (map id, player cap, spawn points).
	 */
	public static class ArenaMap {
		/** 映射 ID / Map id */
		private int mapId;
		/** 出生点坐标列表。 / Spawn coordinate list. */
		private List<Float[]> spawns;
		/** 单实例人数上限。 / Per-instance player cap. */
		private int playerCap;
		private List<Integer> staticDoors = null;

		/**
		 * 构造竞技场地图。
		 * Constructs an arena map.
		 *
		 * map id
		 * player cap
		 * spawns
		 */
		public ArenaMap(int mapId, int playerCap, List<Float[]> spawns) {
			this.mapId = mapId;
			this.playerCap = playerCap;
			this.spawns = spawns;
		}

		/**
		 * map id
		 */
		public int getMapId() {
			return mapId;
		}

		/**
		 * player cap
		 */
		public int getPlayerCap() {
			return playerCap;
		}

		/**
		 * @return 出生点列表 / spawn list
		 */
		public List<Float[]> getSpawns() {
			return spawns;
		}
	}

	/**
	 * 返回 FFA 服务是否可用。
	 * Returns whether the FFA service is available.
	 *
	 * whether available
	 */
	public static boolean isAvailable() {
		return isAvailable;
	}
}
