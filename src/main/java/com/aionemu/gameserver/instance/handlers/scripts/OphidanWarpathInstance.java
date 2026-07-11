package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.EngulfedOphidanBridgeReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 奥菲丹战径副本事件处理器。
 * Instance event handler for Ophidan Warpath.
 *
 * @author Encom
 */

@InstanceID(301670000)
public class OphidanWarpathInstance extends GeneralInstanceHandler
{
	/** 副本时间戳 / instance timestamp */
	private long instanceTime;
	/** 能量发生器 / power generator */
		private int powerGenerator;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
    /** engulfed ophidan bridge reward / engulfed ophidan bridge reward */
        protected EngulfedOphidanBridgeReward engulfedOphidanBridgeReward;
    /** 败方倍率 / losing-group multiplier */
        private float loosingGroupMultiplier = 1;
    /** 副本是否已销毁 / whether the instance is destroyed */
    private boolean isInstanceDestroyed = false;
    /** 副本是否已开始 / whether the instance started */
        protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
    /** warpath 任务 / warpath task */
        private final List<Future<?>> warpathTask = new ArrayList<Future<?>>();
	
    protected EngulfedOphidanBridgePlayerReward getPlayerReward(Player player) {
        engulfedOphidanBridgeReward.regPlayerReward(player);
        return (EngulfedOphidanBridgePlayerReward) engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
    }
	
    private boolean containPlayer(Integer object) {
        return engulfedOphidanBridgeReward.containPlayer(object);
    }
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
        }
    }
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(0, storage.getItemCountByItemId(0));
	}
	
    protected void startInstanceTask() {
    	instanceTime = System.currentTimeMillis();
        engulfedOphidanBridgeReward.setInstanceStartTime();
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!engulfedOphidanBridgeReward.isRewarded()) {
				    openFirstDoors();
				    // 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
				    sendMsgByRace(1401181, Race.PC_ALL, 5000);
					// 贝里特拉能量发生器几乎充满。 / The Beritra Power Generator is almost completely charged.
				    sendMsgByRace(1403624, Race.PC_ALL, 20000);
                    engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
                    startInstancePacket();
                    engulfedOphidanBridgeReward.sendPacket(4, null);
					sp(806391, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 3); //North Idle Power Generator.
					sp(806392, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 42); //South Idle Power Generator.
					sp(833935, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 3); //Beritra Army Power Generator.
					sp(833936, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 42); //Beritra Army Power Generator.
					sp(806272, 758.85846f, 566.28235f, 577.43921f, (byte) 0, 2); //Southern Cave Teleporter.
					sp(806273, 314.84390f, 489.72495f, 597.13184f, (byte) 0, 32); //Northern Cave Teleporter.
					sp(806274, 586.42255f, 477.52847f, 620.75189f, (byte) 0, 155); //Cave Teleport Statue.
					sp(806275, 617.93579f, 508.27386f, 592.09863f, (byte) 0, 156); //Cave Teleport Statue.
				}
            }
        }, 90000)); //...1 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 150000)); //...2 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 210000)); //...3 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 270000)); //...4 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 330000)); //...5 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 390000)); //...6 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 450000)); //...7 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 510000)); //...8 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 570000)); //...9 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 630000)); //...10 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 690000)); //...11 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 750000)); //...12 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 810000)); //...13 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 870000)); //...14 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 930000)); //...15 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 990000)); //...16 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 1050000)); //...17 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 天族现已拥有封印的雷安遗物。 / The Elyos now own the Sealed Reian Relic.
				sendMsgByRace(1403561, Race.PC_ALL, 0);
				spawnChestPartElyos();
				spawnMechanicalElyos();
            }
        }, 1110000)); //...18 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                sendPacket(false);
                engulfedOphidanBridgeReward.sendPacket(4, null);
				// 魔族现已控制封印的雷安遗物。 / The Asmodians now control the Sealed Reian Relic.
				sendMsgByRace(1403560, Race.PC_ALL, 0);
				spawnChestPartAsmodians();
				spawnMechanicalAsmodians();
            }
        }, 1170000)); //...19 Minutes 30s
		warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!engulfedOphidanBridgeReward.isRewarded()) {
					Race winnerRace = engulfedOphidanBridgeReward.getWinnerRaceByScore();
					stopInstance(winnerRace);
				}
            }
        }, 1200000));
    }
	
   /**
	 * 天族 / Elyos
	 */
	private void spawnMechanicalElyos() {
	    spawn(833950, 600.0f, 423.0f, 609.1875f, (byte) 68);
		spawn(833950, 677.0f, 485.0f, 599.625f, (byte) 7);
		spawn(833950, 549.0f, 461.0f, 615.9375f, (byte) 40);
		spawn(833950, 634.0f, 539.0f, 589.1875f, (byte) 3);
		spawn(833950, 678.0f, 449.0f, 599.98193f, (byte) 56);
		spawn(833950, 612.0f, 522.0f, 591.48346f, (byte) 81);
		spawn(833950, 472.0f, 530.0f, 604.875f, (byte) 55);
		spawn(833950, 493.0f, 463.0f, 606.5625f, (byte) 27);
		spawn(833950, 605.0f, 513.0f, 591.6789f, (byte) 114);
		spawn(833950, 651.0f, 424.0f, 605.6841f, (byte) 25);
		spawn(833950, 578.0f, 460.0f, 620.15216f, (byte) 111);
		spawn(833950, 694.0f, 479.0f, 599.9584f, (byte) 86);
		spawn(833950, 524.0f, 426.0f, 613.0f, (byte) 23);
		spawn(833950, 573.7f, 482.3f, 620.81024f, (byte) 31);
		spawn(833950, 643.0f, 440.0f, 605.625f, (byte) 23);
		spawn(833950, 562.0f, 534.0f, 599.875f, (byte) 74);
		spawn(833950, 595.0f, 379.0f, 609.57855f, (byte) 108);
		spawn(833950, 573.0f, 397.0f, 609.1875f, (byte) 56);
		spawn(833950, 668.0f, 454.0f, 599.75f, (byte) 106);
		spawn(833950, 670.0f, 521.0f, 595.875f, (byte) 17);
		spawn(833950, 579.0f, 421.0f, 609.7527f, (byte) 110);
		spawn(833950, 680.0f, 468.0f, 599.75f, (byte) 48);
		spawn(833950, 628.0f, 430.0f, 607.125f, (byte) 72);
		spawn(833950, 597.0f, 395.0f, 609.25104f, (byte) 14);
	}
	private void spawnChestPartElyos() {
	    spawn(833951, 614.75696f, 508.87222f, 592.0906f, (byte) 32);
		spawn(833951, 626.0f, 519.0f, 592.29364f, (byte) 67);
		spawn(833951, 570.29816f, 425.61432f, 611.41455f, (byte) 99);
		spawn(833951, 618.9085f, 522.03625f, 591.74426f, (byte) 71);
		spawn(833951, 624.8603f, 515.6118f, 592.44324f, (byte) 52);
		spawn(833951, 686.7708f, 490.0117f, 599.86646f, (byte) 59);
		spawn(833951, 570.1571f, 480.35446f, 620.5303f, (byte) 3);
		spawn(833951, 570.1781f, 468.12268f, 620.2185f, (byte) 11);
		spawn(833951, 611.78326f, 407.5259f, 608.51807f, (byte) 43);
		spawn(833951, 668.7937f, 463.2208f, 599.5267f, (byte) 97);
		spawn(833951, 608.2965f, 509.28085f, 591.5489f, (byte) 15);
		spawn(833951, 454.84802f, 506.42538f, 604.50684f, (byte) 107);
		spawn(833951, 615.49066f, 523.0673f, 591.66815f, (byte) 86);
		spawn(833951, 604.1456f, 547.00757f, 590.5f, (byte) 90);
		spawn(833951, 606.0356f, 423.58078f, 607.99335f, (byte) 88);
		spawn(833951, 611.50806f, 566.4567f, 590.71704f, (byte) 83);
		spawn(833951, 598.4454f, 381.3276f, 609.57855f, (byte) 53);
		spawn(833951, 672.76544f, 469.15033f, 599.32715f, (byte) 6);
		spawn(833951, 490.71436f, 482.9249f, 605.1118f, (byte) 63);
		spawn(833951, 656.13776f, 466.9984f, 600.2328f, (byte) 119);
		spawn(833951, 489.4269f, 509.61548f, 605.0651f, (byte) 74);
		spawn(833951, 591.1686f, 427.19696f, 610.3221f, (byte) 90);
		spawn(833951, 591.27454f, 382.10886f, 609.125f, (byte) 9);
		spawn(833951, 473.67172f, 502.37726f, 605.25f, (byte) 43);
		spawn(833951, 474.93942f, 475.15845f, 604.25f, (byte) 6);
		spawn(833951, 589.9174f, 545.2889f, 590.67975f, (byte) 5);
		spawn(833951, 689.20935f, 453.58746f, 599.9777f, (byte) 38);
		spawn(833951, 659.10974f, 458.06223f, 601.0303f, (byte) 18);
		spawn(833951, 466.38913f, 482.13803f, 604.44025f, (byte) 17);
		spawn(833951, 577.8969f, 388.76083f, 609.20245f, (byte) 20);
		spawn(833951, 583.5908f, 461.02527f, 620.1254f, (byte) 44);
		spawn(833951, 609.1258f, 384.5054f, 609.57855f, (byte) 20);
		spawn(833951, 671.76294f, 489.84125f, 599.75f, (byte) 98);
		spawn(833951, 612.0762f, 395.11655f, 609.19476f, (byte) 48);
		spawn(833951, 479.1732f, 499.45947f, 605.0f, (byte) 4);
		spawn(833951, 594.2503f, 569.0778f, 590.91034f, (byte) 95);
		spawn(833951, 459.89664f, 483.47577f, 604.4813f, (byte) 31);
		spawn(833951, 568.2626f, 478.05176f, 620.2774f, (byte) 92);
		spawn(833951, 620.1892f, 567.508f, 591.2927f, (byte) 66);
		spawn(833951, 681.99725f, 457.24365f, 599.9777f, (byte) 39);
		spawn(833951, 665.2871f, 478.41504f, 599.21014f, (byte) 39);
		spawn(833951, 662.5696f, 474.53754f, 599.1606f, (byte) 53);
		spawn(833951, 489.15237f, 489.4455f, 605.0f, (byte) 62);
		spawn(833951, 620.2688f, 543.25995f, 590.75f, (byte) 40);
		spawn(833951, 574.73724f, 454.57498f, 620.34515f, (byte) 49);
		spawn(833951, 480.36932f, 516.76117f, 604.70245f, (byte) 58);
		spawn(833951, 582.88556f, 481.7509f, 620.74567f, (byte) 84);
	}
	
   /**
	 * 魔族 / Asmodians
	 */
	private void spawnMechanicalAsmodians() {
	    spawn(833960, 600.0f, 423.0f, 609.1875f, (byte) 68);
		spawn(833960, 677.0f, 485.0f, 599.625f, (byte) 7);
		spawn(833960, 549.0f, 461.0f, 615.9375f, (byte) 40);
		spawn(833960, 634.0f, 539.0f, 589.1875f, (byte) 3);
		spawn(833960, 678.0f, 449.0f, 599.98193f, (byte) 56);
		spawn(833960, 612.0f, 522.0f, 591.48346f, (byte) 81);
		spawn(833960, 472.0f, 530.0f, 604.875f, (byte) 55);
		spawn(833960, 493.0f, 463.0f, 606.5625f, (byte) 27);
		spawn(833960, 605.0f, 513.0f, 591.6789f, (byte) 114);
		spawn(833960, 651.0f, 424.0f, 605.6841f, (byte) 25);
		spawn(833960, 578.0f, 460.0f, 620.15216f, (byte) 111);
		spawn(833960, 694.0f, 479.0f, 599.9584f, (byte) 86);
		spawn(833960, 524.0f, 426.0f, 613.0f, (byte) 23);
		spawn(833960, 573.7f, 482.3f, 620.81024f, (byte) 31);
		spawn(833960, 643.0f, 440.0f, 605.625f, (byte) 23);
		spawn(833960, 562.0f, 534.0f, 599.875f, (byte) 74);
		spawn(833960, 595.0f, 379.0f, 609.57855f, (byte) 108);
		spawn(833960, 573.0f, 397.0f, 609.1875f, (byte) 56);
		spawn(833960, 668.0f, 454.0f, 599.75f, (byte) 106);
		spawn(833960, 670.0f, 521.0f, 595.875f, (byte) 17);
		spawn(833960, 579.0f, 421.0f, 609.7527f, (byte) 110);
		spawn(833960, 680.0f, 468.0f, 599.75f, (byte) 48);
		spawn(833960, 628.0f, 430.0f, 607.125f, (byte) 72);
		spawn(833960, 597.0f, 395.0f, 609.25104f, (byte) 14);
	}
	private void spawnChestPartAsmodians() {
	    spawn(833961, 614.75696f, 508.87222f, 592.0906f, (byte) 32);
		spawn(833961, 626.0f, 519.0f, 592.29364f, (byte) 67);
		spawn(833961, 570.29816f, 425.61432f, 611.41455f, (byte) 99);
		spawn(833961, 618.9085f, 522.03625f, 591.74426f, (byte) 71);
		spawn(833961, 624.8603f, 515.6118f, 592.44324f, (byte) 52);
		spawn(833961, 686.7708f, 490.0117f, 599.86646f, (byte) 59);
		spawn(833961, 570.1571f, 480.35446f, 620.5303f, (byte) 3);
		spawn(833961, 570.1781f, 468.12268f, 620.2185f, (byte) 11);
		spawn(833961, 611.78326f, 407.5259f, 608.51807f, (byte) 43);
		spawn(833961, 668.7937f, 463.2208f, 599.5267f, (byte) 97);
		spawn(833961, 608.2965f, 509.28085f, 591.5489f, (byte) 15);
		spawn(833961, 454.84802f, 506.42538f, 604.50684f, (byte) 107);
		spawn(833961, 615.49066f, 523.0673f, 591.66815f, (byte) 86);
		spawn(833961, 604.1456f, 547.00757f, 590.5f, (byte) 90);
		spawn(833961, 606.0356f, 423.58078f, 607.99335f, (byte) 88);
		spawn(833961, 611.50806f, 566.4567f, 590.71704f, (byte) 83);
		spawn(833961, 598.4454f, 381.3276f, 609.57855f, (byte) 53);
		spawn(833961, 672.76544f, 469.15033f, 599.32715f, (byte) 6);
		spawn(833961, 490.71436f, 482.9249f, 605.1118f, (byte) 63);
		spawn(833961, 656.13776f, 466.9984f, 600.2328f, (byte) 119);
		spawn(833961, 489.4269f, 509.61548f, 605.0651f, (byte) 74);
		spawn(833961, 591.1686f, 427.19696f, 610.3221f, (byte) 90);
		spawn(833961, 591.27454f, 382.10886f, 609.125f, (byte) 9);
		spawn(833961, 473.67172f, 502.37726f, 605.25f, (byte) 43);
		spawn(833961, 474.93942f, 475.15845f, 604.25f, (byte) 6);
		spawn(833961, 589.9174f, 545.2889f, 590.67975f, (byte) 5);
		spawn(833961, 689.20935f, 453.58746f, 599.9777f, (byte) 38);
		spawn(833961, 659.10974f, 458.06223f, 601.0303f, (byte) 18);
		spawn(833961, 466.38913f, 482.13803f, 604.44025f, (byte) 17);
		spawn(833961, 577.8969f, 388.76083f, 609.20245f, (byte) 20);
		spawn(833961, 583.5908f, 461.02527f, 620.1254f, (byte) 44);
		spawn(833961, 609.1258f, 384.5054f, 609.57855f, (byte) 20);
		spawn(833961, 671.76294f, 489.84125f, 599.75f, (byte) 98);
		spawn(833961, 612.0762f, 395.11655f, 609.19476f, (byte) 48);
		spawn(833961, 479.1732f, 499.45947f, 605.0f, (byte) 4);
		spawn(833961, 594.2503f, 569.0778f, 590.91034f, (byte) 95);
		spawn(833961, 459.89664f, 483.47577f, 604.4813f, (byte) 31);
		spawn(833961, 568.2626f, 478.05176f, 620.2774f, (byte) 92);
		spawn(833961, 620.1892f, 567.508f, 591.2927f, (byte) 66);
		spawn(833961, 681.99725f, 457.24365f, 599.9777f, (byte) 39);
		spawn(833961, 665.2871f, 478.41504f, 599.21014f, (byte) 39);
		spawn(833961, 662.5696f, 474.53754f, 599.1606f, (byte) 53);
		spawn(833961, 489.15237f, 489.4455f, 605.0f, (byte) 62);
		spawn(833961, 620.2688f, 543.25995f, 590.75f, (byte) 40);
		spawn(833961, 574.73724f, 454.57498f, 620.34515f, (byte) 49);
		spawn(833961, 480.36932f, 516.76117f, 604.70245f, (byte) 58);
		spawn(833961, 582.88556f, 481.7509f, 620.74567f, (byte) 84);
	}
	
    protected void stopInstance(Race race) {
        stopInstanceTask();
        engulfedOphidanBridgeReward.setWinnerRace(race);
        engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        reward();
        engulfedOphidanBridgeReward.sendPacket(5, null);
    }
	
    /**
     * 玩家进入副本时处理。
     * Handle a player entering the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onEnterInstance(final Player player) {
        if (!containPlayer(player.getObjectId())) {
            engulfedOphidanBridgeReward.regPlayerReward(player);
        }
        sendEnterPacket(player);
    }
	
    private void sendEnterPacket(final Player player) {
    	instance.doOnAllPlayers(new Visitor<Player>() {
            /**
             * 处理 visit。
             * Handle visit.
             *
             * opponent
             */
            @Override
            public void visit(Player opponent) {
                if (player.getRace() != opponent.getRace()) {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime2(), getInstanceReward(), player.getObjectId()));
                    PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime2(), getInstanceReward(), opponent.getObjectId()));
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime2(), getInstanceReward(),  player.getObjectId()));
                } else {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime2(), getInstanceReward(), opponent.getObjectId()));
                    if (player.getObjectId() != opponent.getObjectId()) {
                        PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime2(), getInstanceReward(), player.getObjectId(), 20, 0));
                    }
                }
            }
        });
    	sendPacket(true);
    	sendPacket(false);
        PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(4, getTime2(), getInstanceReward(), player.getObjectId(), 20, 0));
    }
	
    private void startInstancePacket() {
    	instance.doOnAllPlayers(new Visitor<Player>() {
            /**
             * 处理 visit。
             * Handle visit.
             *
             * @param player 玩家 / player
             */
            @Override
            public void visit(Player player) {
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime2(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(3, getTime2(), engulfedOphidanBridgeReward, player.getObjectId(), 0, 0));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime2(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
            	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime2(), getInstanceReward(), player.getObjectId()));
            }
        });
    }
	
    private void sendPacket(boolean isObjects) {
    	if (isObjects) {
    		instance.doOnAllPlayers(new Visitor<Player>() {
                /**
                 * 处理 visit。
                 * Handle visit.
                 *
                 * @param player 玩家 / player
                 */
                @Override
                public void visit(Player player) {
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime2(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
                }
            });
    	} else {
    		instance.doOnAllPlayers(new Visitor<Player>() {
                /**
                 * 处理 visit。
                 * Handle visit.
                 *
                 * @param player 玩家 / player
                 */
                @Override
                public void visit(Player player) {
                	PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime2(), engulfedOphidanBridgeReward, instance.getPlayersInside(), true));
                }
            });
    	}
    }
	
    /**
     * 副本创建时初始化逻辑。
     * Initialize logic when the instance is created.
     *
     * @param instance 世界地图实例 / world-map instance
     */
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        engulfedOphidanBridgeReward = new EngulfedOphidanBridgeReward(mapId, instanceId, instance);
        engulfedOphidanBridgeReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        doors = instance.getDoors();
        startInstanceTask();
    }
	
	protected void reward() {
        int ElyosPvPKills = getPvpKillsByRace(Race.ELYOS).intValue();
        int ElyosPoints = getPointsByRace(Race.ELYOS).intValue();
        int AsmoPvPKills = getPvpKillsByRace(Race.ASMODIANS).intValue();
        int AsmoPoints = getPointsByRace(Race.ASMODIANS).intValue();
        for (Player player : instance.getPlayersInside()) {
            if (PlayerActions.isAlreadyDead(player)) {
				PlayerReviveService.duelRevive(player);
			}
			EngulfedOphidanBridgePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
			int abyssPoint = 3163;
			int gloryPoint = 150;
			int expPoint = 10000;
			playerReward.setRewardAp((int) abyssPoint);
            playerReward.setRewardGp((int) gloryPoint);
			playerReward.setRewardExp((int) expPoint);
			if (player.getRace().equals(engulfedOphidanBridgeReward.getWinnerRace())) {
                abyssPoint += engulfedOphidanBridgeReward.AbyssReward(true, true);
                gloryPoint += engulfedOphidanBridgeReward.GloryReward(true, true);
				expPoint += engulfedOphidanBridgeReward.ExpReward(true, true);
                playerReward.setBonusAp(engulfedOphidanBridgeReward.AbyssReward(true, true));
                playerReward.setBonusGp(engulfedOphidanBridgeReward.GloryReward(true, true));
				playerReward.setBonusExp(engulfedOphidanBridgeReward.ExpReward(true, true));
				playerReward.setBrokenSpinel(188100391);
				playerReward.setBonusReward(186000243);
				playerReward.setAdditionalReward(188055394);
			} else {
                abyssPoint += engulfedOphidanBridgeReward.AbyssReward(false, false);
                gloryPoint += engulfedOphidanBridgeReward.GloryReward(false, false);
				expPoint += engulfedOphidanBridgeReward.ExpReward(false, false);
				playerReward.setRewardAp(engulfedOphidanBridgeReward.AbyssReward(false, false));
                playerReward.setRewardGp(engulfedOphidanBridgeReward.GloryReward(false, false));
				playerReward.setRewardExp(engulfedOphidanBridgeReward.ExpReward(false, false));
				playerReward.setBrokenSpinel(188100391);
				playerReward.setBonusReward(186000243);
            }
			ItemService.addItem(player, 188055394, 1);
            ItemService.addItem(player, 188100391, 750); //5.5
			ItemService.addItem(player, 186000243, 1);
            AbyssPointsService.addAp(player, (int) abyssPoint);
            AbyssPointsService.addGp(player, (int) gloryPoint);
            player.getCommonData().addExp(expPoint, RewardType.HUNTING);
        }
        for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player : instance.getPlayersInside()) {
						onExitInstance(player);
					}
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
				}
			}
		}, 60000);
    }
	
    private int getTime2() {
        long result = System.currentTimeMillis() - instanceTime;
        if (result < 90000) {
            return (int) (90000 - result);
        } else if (result < 1200000) { //20-Mins
            return (int) (1200000 - (result - 90000));
        }
        return 0;
    }
	
    /**
     * 处理玩家复活事件。
     * Handle a player revive event.
     *
     * 玩家 / player
     * result
     */
    @Override
    public boolean onReviveEvent(Player player) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PlayerReviveService.revive(player, 100, 100, false, 0);
        player.getGameStats().updateStatsAndSpeedVisually();
        engulfedOphidanBridgeReward.portToPosition(player);
        return true;
    }
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * 玩家 / player
     * @param lastAttacker 最后攻击者 / last attacker
     * result
     */
    @Override
    public boolean onDie(Player player, Creature lastAttacker) {
		EngulfedOphidanBridgePlayerReward ownerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player);
        int points = 60;
        if (lastAttacker instanceof Player) {
            if (lastAttacker.getRace() != player.getRace()) {
                InstancePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
				if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
                    points *= loosingGroupMultiplier;
                } else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
                    points = 0;
                }
                updateScore((Player) lastAttacker, player, points, true);
            }
        }
        updateScore(player, player, -points, false);
        return true;
    }
	
	private MutableInt getPvpKillsByRace(Race race) {
        return engulfedOphidanBridgeReward.getPvpKillsByRace(race);
    }
	
    private MutableInt getPointsByRace(Race race) {
        return engulfedOphidanBridgeReward.getPointsByRace(race);
    }
	
    private void addPointsByRace(Race race, int points) {
        engulfedOphidanBridgeReward.addPointsByRace(race, points);
    }
	
    private void addPvpKillsByRace(Race race, int points) {
        engulfedOphidanBridgeReward.addPvpKillsByRace(race, points);
    }
	
    private void addPointToPlayer(Player player, int points) {
        engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId()).addPoints(points);
    }
	
    private void addPvPKillToPlayer(Player player) {
        engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId()).addPvPKillToPlayer();
    }
	
    protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
        if (points == 0) {
            return;
        }
        addPointsByRace(player.getRace(), points);
        List<Player> playersToGainScore = new ArrayList<Player>();
        if (target != null && player.isInGroup2()) {
            for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
                if (member.getLifeStats().isAlreadyDead()) {
                    continue;
                } if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
                    playersToGainScore.add(member);
                }
            }
        } else {
            playersToGainScore.add(player);
        }
        for (Player playerToGainScore : playersToGainScore) {
            addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
            if (target instanceof Npc) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
            } else if (target instanceof Player) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
            }
        }
        int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
        if (pointDifference < 0) {
            pointDifference *= -1;
        } if (pointDifference >= 3000) {
            loosingGroupMultiplier = 10;
        } else if (pointDifference >= 1000) {
            loosingGroupMultiplier = 1.5f;
        } else {
            loosingGroupMultiplier = 1;
        } if (pvpKill && points > 0) {
            addPvpKillsByRace(player.getRace(), 1);
            addPvPKillToPlayer(player);
        }
        engulfedOphidanBridgeReward.sendPacket(11, player.getObjectId());
        if (engulfedOphidanBridgeReward.hasCapPoints()) {
            stopInstance(engulfedOphidanBridgeReward.getWinnerRaceByScore());
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
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("NORTH_POST_301670000")) {
            powerGenerator = 1;
	    } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("SOUTH_POST_301670000")) {
			powerGenerator = 2;
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
        int point = 0;
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 243962: //  .
			case 243963: //  ?.
			case 243964: //?   ?.
			    point = 25;
				despawnNpc(npc);
            break;
        }
        updateScore(mostPlayerDamage, npc, point, false);
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
		int point = 0;
		switch (npc.getNpcId()) {
			case 701947: //Elyos Field Gun.
			case 701949: //Elyos Field Gun.
                GameEngineServices.skillEngine().getSkill(npc, 21065, 1, player).useNoAnimationSkill();
            break;
			case 701948: //Asmodians Field Gun.
			case 701950: //Asmodians Field Gun.
                GameEngineServices.skillEngine().getSkill(npc, 21066, 1, player).useNoAnimationSkill();
            break;
			case 833935: //? ? .
				point = 1000;
				despawnNpc(npc);
				deleteNpc(806425);
				deleteNpc(806391);
				if (powerGenerator == 1) {
					switch (player.getRace()) {
						case ELYOS:
						    // 天族已激活贝里特拉能量发生器。 / The Elyos have activated the Beritra Power Generator.
							sendMsgByRace(1403449, Race.PC_ALL, 0);
						    sp(802036, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 0); //North Post Flag.
							sp(806391, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 3); //North Power Generator.
							GameEngineServices.skillEngine().getSkill(npc, 21336, 1, player).useNoAnimationSkill(); //Shugo Alchemical Enhancement Device.
						break;
					    case ASMODIANS:
						    // 魔族已激活贝里特拉能量发生器。 / The Asmodians have activated the Beritra Power Generator.
							sendMsgByRace(1403450, Race.PC_ALL, 0);
						    sp(802037, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 0); //North Post Flag.
						    sp(806391, 589.974180f, 407.85278f, 610.20313f, (byte) 0, 3); //North Power Generator.
							GameEngineServices.skillEngine().getSkill(npc, 21337, 1, player).useNoAnimationSkill(); //Shugo Alchemical Enhancement Device.
						break;
					}
				}
				// 贝里特拉能量发生器已充满，可以使用。 / The Beritra Power Generator is completely charged and can be used.
				// 装置即将过载，无法再充能。 / The device is close to being overloaded and cannot be charged anymore.
				sendMsgByRace(1403453, Race.PC_ALL, 5000);
			break;
			case 833936: //? ? .
				point = 1000;
				despawnNpc(npc);
				deleteNpc(806425);
				deleteNpc(806392);
				if (powerGenerator == 2) {
					switch (player.getRace()) {
						case ELYOS:
						    // 天族已激活贝里特拉能量发生器。 / The Elyos have activated the Beritra Power Generator.
							sendMsgByRace(1403449, Race.PC_ALL, 0);
						    sp(802039, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 0); //South Post Flag.
						    sp(806392, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 42); //South Power Generator.
							GameEngineServices.skillEngine().getSkill(npc, 21336, 1, player).useNoAnimationSkill(); //Shugo Alchemical Enhancement Device.
						break;
					    case ASMODIANS:
						    // 魔族已激活贝里特拉能量发生器。 / The Asmodians have activated the Beritra Power Generator.
							sendMsgByRace(1403450, Race.PC_ALL, 0);
						    sp(802040, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 0); //South Post Flag.
						    sp(806392, 605.049130f, 553.60150f, 591.49310f, (byte) 0, 42); //South Idle Power Generator.
							GameEngineServices.skillEngine().getSkill(npc, 21337, 1, player).useNoAnimationSkill(); //Shugo Alchemical Enhancement Device.
						break;
					}
				}
				// 贝里特拉能量发生器已充满，可以使用。 / The Beritra Power Generator is completely charged and can be used.
				// 装置即将过载，无法再充能。 / The device is close to being overloaded and cannot be charged anymore.
				sendMsgByRace(1403453, Race.PC_ALL, 5000);
			break;
			case 833950: //Mechanical Weapon Test Part.
			case 833960: //Mechanical Weapon Test Part.
                point = 200;
				despawnNpc(npc);
				// 你已从奇异奥菲丹进阶路线取回机械武器试验部件。 / Youve retrieved the Mechanical Weapon Test Parts from the Odd Ophidan Advanced Route.
				sendMsgByRace(1403555, Race.PC_ALL, 0);
            break;
			case 833951: //Mechanical Weapon Test Part Box.
			case 833961: //Mechanical Weapon Test Part Box.
                point = 2000;
				despawnNpc(npc);
            break;
        }
		updateScore(player, npc, point, false);
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
	
    /**
     * 副本销毁时清理资源。
     * Clean up resources when the instance is destroyed.
     */
    @Override
    public void onInstanceDestroy() {
        engulfedOphidanBridgeReward.clear();
        isInstanceDestroyed = true;
        stopInstanceTask();
        doors.clear();
    }
	
    protected void openFirstDoors() {
        openDoor(176);
		openDoor(177);
    }
	
    protected void openDoor(int doorId) {
        StaticDoor door = doors.get(doorId);
        if (door != null) {
            door.setOpen(true);
        }
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
    }
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        warpathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        }, time));
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
	
    private void stopInstanceTask() {
        for (Future<?> task : warpathTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	
    /**
     * 返回本副本奖励对象。
     * Return this instance's reward object.
     *
     * result
     */
    @Override
    public InstanceReward<?> getInstanceReward() {
        return engulfedOphidanBridgeReward;
    }
	
    /**
     * 玩家请求退出副本时处理。
     * Handle a player exit request.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onExitInstance(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
	
    /**
     * 玩家离开副本时处理。
     * Handle a player leaving the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onLeaveInstance(Player player) {
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
		EngulfedOphidanBridgePlayerReward playerReward = engulfedOphidanBridgeReward.getPlayerReward(player.getObjectId());
		playerReward.endBoostMoraleEffect(player);
		removeItems(player);
    }
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}
	
    /**
     * 玩家登录到该副本时处理。
     * Handle a player logging into this instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onPlayerLogin(Player player) {
        engulfedOphidanBridgeReward.sendPacket(10, player.getObjectId());
    }
}
