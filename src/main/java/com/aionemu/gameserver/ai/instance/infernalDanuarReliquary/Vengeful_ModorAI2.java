package com.aionemu.gameserver.ai.instance.infernalDanuarReliquary;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Infernal Danuar Reliquary 副本 NPC AI：Vengeful Modor（@AIName "vengeful_modor"），继承 AggressiveNpcAI2。
 * Infernal Danuar Reliquary instance NPC AI: Vengeful Modor (@AIName "vengeful_modor"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("vengeful_modor")
public class Vengeful_ModorAI2 extends AggressiveNpcAI2
{
	private Future<?> skillTask;
	private boolean canThink = true;
	private AtomicBoolean isHome = new AtomicBoolean(true);
	private List<Integer> percents = new ArrayList<Integer>();
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 25) {
				if (startedEvent.compareAndSet(false, true)) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 19246, 60, getOwner()).useNoAnimationSkill();
					// 很久没有人试图窃取我的研究成果了…… / It's been quite a while since someone tried to steal the fruits of my research...
					// 或许有趣。我给你 15 分钟。去吧！ / Could be interesting. I'll give you 15 minutes. Go!
					sendMsg(1500737, getObjectId(), false, 3000);
					// 我唯一的儿子终于安全了……我的儿子？他在哪？ / My only son is safe at last.... My son? Where is he?
					sendMsg(1500738, getObjectId(), false, 6000);
					// 你别想妨碍我的制作！ / You will not hinder my craft!
					sendMsg(1500740, getObjectId(), false, 9000);
				}
			}
		}
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
		if (isHome.compareAndSet(true, false)) {
            // 滚开！ / Be gone!
			sendMsg(1500739, getObjectId(), false, 0);
            startSkillTask();
		}
	}
	
    private void addPercent() {
        percents.clear();
        Collections.addAll(percents, new Integer[]{75, 70, 65, 60, 50});
    }
	
    private void checkPercentage(int hpPercentage) {
        for (Integer percent : percents) {
            if (hpPercentage <= percent) {
                switch (percent) {
                    case 75:
                        Teleport();
						//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
						announceAnotherDimension();
                    break;
                    case 70:
                        Teleport2();
						//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
						announceAnotherDimension();
                    break;
                    case 65:
					    Teleport3();
						startSkillTask();
						//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
						announceAnotherDimension();
                    break;
                    case 60:
                        Teleport4();
						//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
						announceAnotherDimension();
                    break;
                    case 50:
                        Teleport5();
						//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
						announceAnotherDimension();
                    break;
                }
                percents.remove(percent);
                break;
            }
        }
    }
	
	private void startSkillTask() {
		skillTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
				    cancelTask();
				} else {
					chooseRandomEvent();
				}
			}
		}, 5000, 30000);
	}
	
	private void cancelTask() {
		if (skillTask != null && !skillTask.isCancelled()) {
			skillTask.cancel(true);
		}
	}
	
    private void chooseRandomEvent() {
        switch (Rnd.get(1, 2)) {
            case 1:
                AI2Actions.targetSelf(Vengeful_ModorAI2.this);
                GameEngineServices.skillEngine().getSkill(getOwner(), 21171, 60, getOwner()).useNoAnimationSkill();
            break;
            case 2:
                AI2Actions.targetSelf(Vengeful_ModorAI2.this);
                GameEngineServices.skillEngine().getSkill(getOwner(), 21229, 60, getOwner()).useNoAnimationSkill();
            break;
        }
    }
	
	private void Teleport() {
		if (!isAlreadyDead()) {
			// 起来，我的孩子们，起来！ / Rise, my children, rise!
			sendMsg(1500749, getObjectId(), false, 2000);
			GameEngineServices.skillEngine().getSkill(getOwner(), 21165, 60, getOwner()).useNoAnimationSkill();
		    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				public void run() {
					if (!isAlreadyDead()) {
                        spawn(284380, 244.12497f, 276.17401f, 242.625f, (byte) 0); // 莫多尔的保镖 / Modor's Bodyguard.
                        spawn(284381, 263.12497f, 276.17401f, 242.625f, (byte) 0); // 复仇死神 / Vengeful Reaper.
						spawn(284382, 253.12497f, 277.17401f, 242.625f, (byte) 0); // 霜白阿克伦龙兽 / Hoarfrost Acheron Drake.
						com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(getOwner(), 284.34036f, 262.9162f, 248.851f, (byte) 63);
				        PacketSendUtility.broadcastPacketAndReceive(getOwner(), new SM_FORCED_MOVE(getOwner(), getOwner()));
					}
				}
			}, 2000);
		}
	}
	
    private void Teleport2() {
        AI2Actions.targetSelf(Vengeful_ModorAI2.this);
        GameEngineServices.skillEngine().getSkill(getOwner(), 21165, 60, getOwner()).useNoAnimationSkill();
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
			    float pos1[][] = {
                    {
                        232.426f, 263.818f, 248.6419f, 115
                    }, {
                        271.426f, 230.243f, 250.9022f, 38
                    }, {
                        240.130f, 235.219f, 251.1553f, 17
                    }
                };
                float pos[] = pos1[Rnd.get(0, 2)];
                com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(getOwner(), pos[0], pos[1], pos[2], (byte) pos[3]);
                PacketSendUtility.broadcastPacketAndReceive(getOwner(), new SM_FORCED_MOVE(getOwner(), getOwner()));
            }
        }, 2000);
    }
	
	private void Teleport3() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 21165, 60, getOwner()).useNoAnimationSkill();
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run(){
                float pos1[][] = {
                    {
                        245.426f, 261.818f, 242.1f, 114
                    }, {
                        251.426f, 247.243f, 242.1f, 20
                    }, {
                        261.130f, 247.219f, 242.1f, 40
                    }, {
                        267.426f, 260.243f, 242.1f, 65
                    }, {
                        256.426f, 269.243f, 242.1f, 90
                    }
                };
                float pos[] = pos1[Rnd.get(0, 4)];
                com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(getOwner(), pos[0], pos[1], pos[2], (byte) pos[3]);
                PacketSendUtility.broadcastPacketAndReceive(getOwner(), new SM_FORCED_MOVE(getOwner(), getOwner()));
            }
        }, 2000);
	}
	
	private void Teleport4() {
		AI2Actions.targetSelf(Vengeful_ModorAI2.this);
		GameEngineServices.skillEngine().getSkill(getOwner(), 21165, 60, getOwner()).useNoAnimationSkill();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(getOwner(), 256.4457f, 257.6867f, 242.30f, (byte) 115);
				PacketSendUtility.broadcastPacketAndReceive(getOwner(), new SM_FORCED_MOVE(getOwner(), getOwner()));
			}
		}, 2000);
	}
	
	private void Teleport5() {
		AI2Actions.targetSelf(Vengeful_ModorAI2.this);
		// 哪一个，哪一个…… / Which one, which one...
		sendMsg(1500743, getObjectId(), false, 0);
		// 看看你如何应对这个！ / Let's see how you handle this!
		sendMsg(1500744, getObjectId(), false, 2000);
		GameEngineServices.skillEngine().getSkill(getOwner(), 21165, 60, getOwner()).useNoAnimationSkill();
        EmoteManager.emoteStopAttacking(getOwner());
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			public void run() {
				despawnNpcs(234690); // 复仇的莫多尔 / Vengeful Modor.
				spawn(855244, 255.12497f, 293.17401f, 257.625f, (byte) 22);
				spawn(855244, 284.12497f, 262.17401f, 249.625f, (byte) 0);
				spawn(855244, 271.12497f, 230.17401f, 251.625f, (byte) 0);
				spawn(855244, 240.12497f, 235.17401f, 252.625f, (byte) 0);
				spawn(855244, 232.12497f, 263.17401f, 249.625f, (byte) 0);
			}
		}, 2000);
	}
	
	@Override
	protected void handleCreatureAggro(Creature creature) {
		super.handleCreatureAggro(creature);
	}
	
    @Override
    protected void handleSpawned() {
        super.handleSpawned();
        addPercent();
    }
	
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
		percents.clear();
	}
	
	@Override
	protected void handleBackHome() {
		addPercent();
		super.handleBackHome();
		isHome.set(true);
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
	
	private void announceAnotherDimension() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					//莫多尔已消失到另一维度。 / Modor has disappeared into another dimension.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5_Under_Rune_User_Kill);
				}
			}
		});
	}
	
	private void despawnNpcs(int npcId) {
		List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
}
