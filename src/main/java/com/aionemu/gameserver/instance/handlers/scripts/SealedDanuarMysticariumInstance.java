package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 封印达努亚尔秘术馆副本事件处理器。
 * Instance event handler for Sealed Danuar Mysticarium.
 *
 * @author Encom
 */

@InstanceID(300480000)
public class SealedDanuarMysticariumInstance extends GeneralInstanceHandler
{
    /** 刷怪种族 / spawn race */
    private Race spawnRace;
	/** raid 任务 / raid task */
		private Future<?> raidTask;
	/** keybox 任务 / key box task */
		private Future<?> keyBoxTask;
	/** prison 任务 / prison task */
		private Future<?> prisonTask;
	/**
	 * whether timer1 started
	 */
		private boolean isStartTimer1 = false;
	/** 是否启动计时器2 / is start timer2 */
		private boolean isStartTimer2 = false;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	// 小游戏 1 / Mini Game 1
	/** ancient box1 / ancient box1 */
		private List<Npc> AncientBox1 = new ArrayList<Npc>();
	/** ancient box2 / ancient box2 */
		private List<Npc> AncientBox2 = new ArrayList<Npc>();
	// 小游戏 2 / Mini Game 2
	/** test subject48012c / test subject48012c */
		private List<Npc> TestSubject48012C = new ArrayList<Npc>();
	/** test subject48013c / test subject48013c */
		private List<Npc> TestSubject48013C = new ArrayList<Npc>();
	/** test subject48015c / test subject48015c */
		private List<Npc> TestSubject48015C = new ArrayList<Npc>();
	/** test subject48023b / test subject48023b */
		private List<Npc> TestSubject48023B = new ArrayList<Npc>();
	/** test subject48027b / test subject48027b */
		private List<Npc> TestSubject48027B = new ArrayList<Npc>();
	/** test subject48025b / test subject48025b */
		private List<Npc> TestSubject48025B = new ArrayList<Npc>();
	/** test subject48039a / test subject48039a */
		private List<Npc> TestSubject48039A = new ArrayList<Npc>();
	/** test subject48123a / test subject48123a */
		private List<Npc> TestSubject48123A = new ArrayList<Npc>();
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		doors = instance.getDoors();
	}
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 702700: //Ancient Box.
			case 702702: //Ancient Box.
				switch (Rnd.get(1, 3)) {
				    case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000224, 1)); //Solid Prison Key.
					break;
					case 2:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000225, 1)); //Shabby Prison Key.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000226, 1)); //Hidden Prison Key.
					break;
				}
			break;
			case 219987: //Doomtread Kurores.
			    switch (Rnd.get(1, 5)) {
					case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080005, 2)); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080006, 2)); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080007, 2)); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080008, 2)); //可爱随从契约。 / Cute Minion Contract.
					break;
					case 5:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190200000, 50)); //Minium.
					break;
				}
			break;
		}
	}
	
	private void SpawnSealedMysticariumRace() {
		final int soloSet = spawnRace == Race.ASMODIANS ? 731584 : 731583;
        final int soloGuide = spawnRace == Race.ASMODIANS ? 805220 : 805219;
		spawn(soloSet, 167.55927f, 122.991554f, 231.67175f, (byte) 118);
		spawn(soloGuide, 171.01581f, 128.56917f, 231.66145f, (byte) 107);
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		if (spawnRace == null) {
			spawnRace = player.getRace();
			SpawnSealedMysticariumRace();
		}
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 219980: //Sheban Espionage Cavalry.
			case 219981: //Sheban Espionage Combatant.
			case 219982: //Sheban Espionage Assassin.
			case 219983: //Sheban Grunt.
			case 219984: //Sheban Gunner.
			case 219985: //MuMu Test Subject.
			case 219986: //Sprigg Test Subject.
			    despawnNpc(npc);
            break;
			case 219987: //Doomtread Kurores.
				despawnNpc(npc);
				deleteNpc(219979); //Ancient Danuar Relic.
				raidTask.cancel(true);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Sealed Danuar Mysticarium>");
				spawn(701572, 556.5924f, 416.37885f, 96.81002f, (byte) 43); //Sealed Danuar Mysticarium Exit.
            break;
		}
    }
	
	private void startAncientBoxTimer() {
		// 找到监狱��匙以击败内部怪物。 / Locate the prison keys to defeat the monsters inside.
		sendMsgByRace(1402801, Race.PC_ALL, 0);
		// 图书馆内所有怪物与钥匙箱已消失。 / All monsters and key boxes in the library have disappeared.
		sendMsgByRace(1402805, Race.PC_ALL, 300000);
		// 图书馆内所有怪物与钥匙箱将在 1 分钟后消失。 / All monsters and key boxes in the library will disappear in 1 minute.
		this.sendMessage(1402802, 4 * 60 * 1000);
		keyBoxTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				spawn(702715, 172.09422f, 206.95068f, 188.01584f, (byte) 118); //Experimental Prison Teleporter.
				AncientBox1.get(0).getController().onDelete();
				AncientBox1.get(1).getController().onDelete();
				AncientBox1.get(2).getController().onDelete();
				AncientBox1.get(3).getController().onDelete();
				AncientBox1.get(4).getController().onDelete();
				AncientBox1.get(5).getController().onDelete();
				AncientBox1.get(6).getController().onDelete();
				AncientBox1.get(7).getController().onDelete();
				AncientBox1.get(8).getController().onDelete();
				AncientBox1.get(9).getController().onDelete();
				AncientBox1.get(10).getController().onDelete();
				AncientBox1.get(11).getController().onDelete();
				AncientBox1.get(12).getController().onDelete();
				AncientBox1.get(13).getController().onDelete();
				AncientBox1.get(14).getController().onDelete();
				AncientBox1.get(15).getController().onDelete();
				AncientBox1.get(16).getController().onDelete();
				AncientBox1.get(17).getController().onDelete();
				AncientBox1.get(18).getController().onDelete();
				AncientBox1.get(19).getController().onDelete();
				// 古代箱 2 / Ancient Box 2
				AncientBox2.get(0).getController().onDelete();
				AncientBox2.get(1).getController().onDelete();
				AncientBox2.get(2).getController().onDelete();
				AncientBox2.get(3).getController().onDelete();
				AncientBox2.get(4).getController().onDelete();
				AncientBox2.get(5).getController().onDelete();
				AncientBox2.get(6).getController().onDelete();
			}
		}, 300000); //5 Minute.
    }
	
	private void startTestSubjectPrisonTimer() {
		// 抓住藏在欧比斯裂隙中的罪犯。 / Catch the criminals hiding in the Abyss Rift.
		sendMsgByRace(1402811, Race.PC_ALL, 0);
		// 所有怪物将在 30 秒后消失。 / All monsters will disappear in 30 seconds.
		sendMsgByRace(1402814, Race.PC_ALL, 270000);
		// 所有怪物即将消失。 / All monsters will disappear in a moment.
		sendMsgByRace(1402815, Race.PC_ALL, 290000);
		// 所有怪物已消失，通往神器库的门已打开。 / All monsters have disappeared and the door to the Artifact Repository has opened.
		sendMsgByRace(1402816, Race.PC_ALL, 300000);
		// 所有怪物将在 2 分钟后消失。 / All monsters will disappear in 2 minutes.
		this.sendMessage(1402812, 3 * 60 * 1000);
		// 所有怪物将在 1 分钟后消失。 / All monsters will disappear in 1 minute.
		this.sendMessage(1402813, 4 * 60 * 1000);
		prisonTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				TestSubject48012C.get(0).getController().onDelete();
				TestSubject48013C.get(0).getController().onDelete();
				TestSubject48015C.get(0).getController().onDelete();
				TestSubject48023B.get(0).getController().onDelete();
				TestSubject48025B.get(0).getController().onDelete();
				TestSubject48027B.get(0).getController().onDelete();
				TestSubject48039A.get(0).getController().onDelete();
				TestSubject48123A.get(0).getController().onDelete();
				spawn(702717, 331.80203f, 495.63107f, 147.172f, (byte) 44); //Sealed Danuar Mysticarium Exit.
			}
		}, 300000); //5 Minute.
    }
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 219979: //Ancient Danuar Relic.
			    /**
	 * Start Mini Game 3
	 */
				// 在增援到达前，保护古代达努阿尔神器免受贝里特拉部队攻击。 / Protect the ancient Danuar artifacts from Beritra's troops until reinforcements arrive.
				sendMsgByRace(1402821, Race.PC_ALL, 0); 
				// 怪物即将进攻。 / Monsters will attack in a moment.
				sendMsgByRace(1402830, Race.PC_ALL, 3000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        doors.get(4).setOpen(true);
				    }
			    }, 5000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid1();
				    }
			    }, 10000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid2();
					}
			    }, 30000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid3();
					}
			    }, 50000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid4();
					}
			    }, 70000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startMysticariumRaid5();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 90000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid6();
					}
			    }, 110000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid7();
					}
			    }, 130000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
					    startMysticariumRaid8();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 150000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid9();
					}
			    }, 170000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid10();
					}
			    }, 190000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid11();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 210000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid12();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 230000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid13();
					}
			    }, 250000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid14();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 270000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid15();
					}
			    }, 290000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid16();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 310000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid17();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 330000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid18();
					}
			    }, 350000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid19();
						// 贝里特拉部队的进攻迫在眉睫。 / An attack by Beritra's troops is imminent.
						sendMsgByRace(1402822, Race.PC_ALL, 0);
						// 更多贝里特拉部队加入进攻。 / Additional Beritra troops have joined the attack.
						sendMsgByRace(1402824, Race.PC_ALL, 3000);
					}
			    }, 370000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
				        startMysticariumRaid20();
						// 多亏增援，贝里特拉全部部队已被歼灭。 / Thanks to our reinforcements, all Beritra's troops have been annihilated.
						// 看来他们的指挥官正准备最终进攻。 / It appears their Commander is preparing for a final offensive.
						sendMsgByRace(1402827, Race.PC_ALL, 0);
					}
			    }, 390000);
			    raidTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
					    startMysticariumBoss();
						// 贝里特拉部队指挥官摧毁了此地。消灭敌方指挥官！ / The Commander of the Beritra troops has destroyed this place. Eliminate the enemy Commander!
						sendMsgByRace(1402828, Race.PC_ALL, 0);
					}
			    }, 410000);
			break;
		}
	}
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
	   /**
	 * Start Mini Game 1
	 */
	    if (zone.getAreaTemplate().getZoneName() == ZoneName.get("DANUAR_MYSTICARIUM_300480000")) {
		    if (!isStartTimer1) {
			    isStartTimer1 = true;
			    System.currentTimeMillis();
			    instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
			        public void visit(Player player) {
					    if (player.isOnline()) {
						    startAncientBoxTimer();
					        PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					    }
					}
				});
			}
			// 古代箱 1 / Ancient Box 1
			AncientBox1.add((Npc) spawn(702700, 199.0f, 184.0f, 187.97449f, (byte) 77));
            AncientBox1.add((Npc) spawn(702700, 154.0f, 186.0f, 187.8586f, (byte) 76));
            AncientBox1.add((Npc) spawn(702700, 156.35754f, 235.06516f, 233.98196f, (byte) 3));
            AncientBox1.add((Npc) spawn(702700, 187.53642f, 206.76617f, 239.05104f, (byte) 37));
            AncientBox1.add((Npc) spawn(702700, 154.0f, 190.0f, 239.38611f, (byte) 37));
            AncientBox1.add((Npc) spawn(702700, 119.0f, 206.0f, 214.17435f, (byte) 107));
            AncientBox1.add((Npc) spawn(702700, 209.41882f, 243.28488f, 187.8586f, (byte) 89));
            AncientBox1.add((Npc) spawn(702700, 216.0f, 238.0f, 239.35832f, (byte) 89));
            AncientBox1.add((Npc) spawn(702700, 187.40462f, 250.03378f, 220.57263f, (byte) 113));
            AncientBox1.add((Npc) spawn(702700, 189.15202f, 227.8461f, 188.0184f, (byte) 104));
            AncientBox1.add((Npc) spawn(702700, 225.0f, 190.0f, 213.7326f, (byte) 88));
            AncientBox1.add((Npc) spawn(702700, 238.0f, 227.0f, 188.69992f, (byte) 72));
            AncientBox1.add((Npc) spawn(702700, 177.7483f, 248.3171f, 207.84833f, (byte) 67));
            AncientBox1.add((Npc) spawn(702700, 220.70958f, 231.82932f, 213.74495f, (byte) 81));
            AncientBox1.add((Npc) spawn(702700, 239.0f, 189.0f, 188.69992f, (byte) 38));
            AncientBox1.add((Npc) spawn(702700, 228.31386f, 202.82614f, 239.83826f, (byte) 53));
            AncientBox1.add((Npc) spawn(702700, 189.0f, 168.0f, 213.73257f, (byte) 77));
            AncientBox1.add((Npc) spawn(702700, 148.0f, 206.0f, 213.73239f, (byte) 60));
            AncientBox1.add((Npc) spawn(702700, 148.89395f, 219.80585f, 187.8586f, (byte) 110));
            AncientBox1.add((Npc) spawn(702700, 256.0f, 206.0f, 214.492f, (byte) 5));
			// 古代箱 2 / Ancient Box 2
			AncientBox2.add((Npc) spawn(702702, 179.30707f, 249.94772f, 240.14725f, (byte) 116));
            AncientBox2.add((Npc) spawn(702702, 168.0f, 246.0f, 189.56546f, (byte) 58));
            AncientBox2.add((Npc) spawn(702702, 147.0f, 253.0f, 213.97218f, (byte) 62));
            AncientBox2.add((Npc) spawn(702702, 194.73796f, 168.25473f, 239.70554f, (byte) 45));
            AncientBox2.add((Npc) spawn(702702, 201.48772f, 207.74902f, 238.82117f, (byte) 78));
            AncientBox2.add((Npc) spawn(702702, 217.0f, 160.0f, 213.896f, (byte) 78));
            AncientBox2.add((Npc) spawn(702702, 255.0f, 213.0f, 188.69994f, (byte) 69));
		}
	   /**
	 * Start Mini Game 2
	 */
		else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("TEST_SUBJECT_PRISON_300480000")) {
		    if (!isStartTimer2) {
			    isStartTimer2 = true;
			    System.currentTimeMillis();
			    instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
			        public void visit(Player player) {
					    if (player.isOnline()) {
						    startTestSubjectPrisonTimer();
					        PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					    }
					}
				});
			}
			TestSubject48012C.add((Npc) spawn(219963, 212.50705f, 509.4579f, 153.22841f, (byte) 114));
			TestSubject48013C.add((Npc) spawn(219964, 225.22668f, 529.2708f, 153.03906f, (byte) 103));
			TestSubject48015C.add((Npc) spawn(219965, 241.73491f, 541.0339f, 152.59001f, (byte) 97));
			TestSubject48023B.add((Npc) spawn(219966, 261.88104f, 545.0504f, 150.50136f, (byte) 90));
			TestSubject48025B.add((Npc) spawn(219967, 295.09076f, 547.3589f, 148.7211f, (byte) 92));
			TestSubject48027B.add((Npc) spawn(219968, 316.75458f, 543.8562f, 148.79962f, (byte) 84));
			TestSubject48039A.add((Npc) spawn(219969, 336.1346f, 532.2977f, 148.47159f, (byte) 75));
			TestSubject48123A.add((Npc) spawn(219978, 345.93323f, 512.3764f, 148.18051f, (byte) 66));
		}
    }
	
	private void raidMysticarium(final Npc npc) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player: instance.getPlayersInside()) {
						npc.setTarget(player);
						((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
						npc.setState(1);
						npc.getMoveController().moveToTargetObject();
						PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
					}
				}
			}
		}, 1000);
	}
	
	/**
	 * 参考视频来源。 / Source: https://www.youtube.com/watch?v=ppgwb2p_iA0 https://www.youtube.com/watch?v=D6jy0R-BOnI
	 */
	public void startMysticariumRaid1() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219983, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid2。
	 * Handle startMysticariumRaid2.
	 */
	public void startMysticariumRaid2() {
	    raidMysticarium((Npc)spawn(219986, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid3。
	 * Handle startMysticariumRaid3.
	 */
	public void startMysticariumRaid3() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219983, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid4。
	 * Handle startMysticariumRaid4.
	 */
	public void startMysticariumRaid4() {
	    raidMysticarium((Npc)spawn(219986, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid5。
	 * Handle startMysticariumRaid5.
	 */
	public void startMysticariumRaid5() {
	    raidMysticarium((Npc)spawn(219980, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid6。
	 * Handle startMysticariumRaid6.
	 */
	public void startMysticariumRaid6() {
	    raidMysticarium((Npc)spawn(219986, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid7。
	 * Handle startMysticariumRaid7.
	 */
	public void startMysticariumRaid7() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219983, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219983, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid8。
	 * Handle startMysticariumRaid8.
	 */
	public void startMysticariumRaid8() {
	    raidMysticarium((Npc)spawn(219980, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid9。
	 * Handle startMysticariumRaid9.
	 */
	public void startMysticariumRaid9() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid10。
	 * Handle startMysticariumRaid10.
	 */
	public void startMysticariumRaid10() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid11。
	 * Handle startMysticariumRaid11.
	 */
	public void startMysticariumRaid11() {
		raidMysticarium((Npc)spawn(219981, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219982, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid12。
	 * Handle startMysticariumRaid12.
	 */
	public void startMysticariumRaid12() {
	    raidMysticarium((Npc)spawn(219980, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid13。
	 * Handle startMysticariumRaid13.
	 */
	public void startMysticariumRaid13() {
	    raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid14。
	 * Handle startMysticariumRaid14.
	 */
	public void startMysticariumRaid14() {
	    raidMysticarium((Npc)spawn(219980, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid15。
	 * Handle startMysticariumRaid15.
	 */
	public void startMysticariumRaid15() {
		raidMysticarium((Npc)spawn(219982, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid16。
	 * Handle startMysticariumRaid16.
	 */
	public void startMysticariumRaid16() {
		raidMysticarium((Npc)spawn(219981, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219986, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid17。
	 * Handle startMysticariumRaid17.
	 */
	public void startMysticariumRaid17() {
	    raidMysticarium((Npc)spawn(219980, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid18。
	 * Handle startMysticariumRaid18.
	 */
	public void startMysticariumRaid18() {
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid19。
	 * Handle startMysticariumRaid19.
	 */
	public void startMysticariumRaid19() {
		raidMysticarium((Npc)spawn(219981, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219982, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219984, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumRaid20。
	 * Handle startMysticariumRaid20.
	 */
	public void startMysticariumRaid20() {
		raidMysticarium((Npc)spawn(219985, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
		raidMysticarium((Npc)spawn(219986, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	/**
	 * 处理 startMysticariumBoss。
	 * Handle startMysticariumBoss.
	 */
	public void startMysticariumBoss() {
		raidMysticarium((Npc)spawn(219987, 494.27405f, 491.4183f, 100.36539f, (byte) 106));
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
						}
					}
				});
			}
		}, time);
	}
	
	private void sendMessage(final int msgId, long delay) {
        if (delay == 0) {
            this.sendMsg(msgId);
        } else {
            GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
                /**
                 * 处理 run。
                 * Handle run.
                 */
                public void run() {
                    sendMsg(msgId);
                }
            }, delay);
        }
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
}