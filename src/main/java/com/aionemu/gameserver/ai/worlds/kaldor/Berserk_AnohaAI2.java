package com.aionemu.gameserver.ai.worlds.kaldor;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kaldor 区域 NPC AI：Berserk Anoha（@AIName "anoha"），继承 AggressiveNpcAI2。
 * Kaldor zone NPC AI: Berserk Anoha (@AIName "anoha"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("anoha")
public class Berserk_AnohaAI2 extends AggressiveNpcAI2
{
	public Player player;
	private Future<?> phaseTask;
	private Future<?> thinkTask;
	private boolean think = true;
	private int curentPercent = 100;
	private Future<?> specialSkillTask;
	private List<Integer> percents = new ArrayList<Integer>();
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	public boolean canThink() {
		return think;
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
		addPercent();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			        @Override
			        public void visit(Player player) {
						AI2Actions.deleteOwner(Berserk_AnohaAI2.this);
						// 狂暴阿诺哈已消失。 / Berserk Anoha has disappeared.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Anoha_DeSpawn);
			        }
				});
			}
		}, 1800000); //30 Minutes.
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			startSpecialSkillTask();
			// 谁敢唤醒我？ / Who dares awaken me ?
			sendMsg(1501392, getObjectId(), false, 0);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private synchronized void checkPercentage(int hpPercentage) {
		curentPercent = hpPercentage;
		for (Integer percent : percents) {
			if (hpPercentage <= percent) {
				switch (percent) {
					case 90:
					case 70:
					case 44:
					case 23:
						think = false;
						EmoteManager.emoteStopAttacking(getOwner());
						GameEngineServices.skillEngine().getSkill(getOwner(), 21765, 60, getOwner()).useNoAnimationSkill();
						// 我为埃雷什基伽尔而战。你不得打扰她。 / I fight for Ereshkigal. You shall not distrub her.
						sendMsg(1501393, getObjectId(), false, 0);
						// 离开埃雷什基伽尔的要塞！ / Leave Ereshkigal's fortress!
						sendMsg(1501394, getObjectId(), false, 5000);
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								if (!isAlreadyDead()) {
									GameEngineServices.skillEngine().getSkill(getOwner(), 21767, 60, getOwner()).useNoAnimationSkill();
									startThinkTask();
									int total = explosiveSacrifice(855262); //Explosive Sacrifice.
									if (total == 0 || (6 - total) != 0) {
										for (int i = 0; i < (6 - total); i++) {
											rndSpawn(855262); //Explosive Sacrifice.
										}
									}
								}
							}
						}, 3500);
					break;
					case 84:
					case 79:
					case 75:
					case 72:
					case 67:
					case 59:
					case 53:
					case 47:
					case 43:
					case 39:
					case 35:
					case 30:
					case 26:
					case 21:
					case 16:
					case 11:
					case 6:
						startPhaseTask();
					break;
				}
				percents.remove(percent);		
				break;
			}
		}
	}
	
	private void startThinkTask() {
		thinkTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					think = true;
					Creature creature = getAggroList().getMostHated();
					if (creature == null || creature.getLifeStats().isAlreadyDead() || !getOwner().canSee(creature)) {
						setStateIfNot(AIState.FIGHT);
						think();
					} else {
						getMoveController().abortMove();
						getOwner().setTarget(creature);
						getOwner().getGameStats().renewLastAttackTime();
						getOwner().getGameStats().renewLastAttackedTime();
						getOwner().getGameStats().renewLastChangeTargetTime();
						getOwner().getGameStats().renewLastSkillTime();
						setStateIfNot(AIState.FIGHT);
						handleMoveValidate();
						startSpecialSkillTask();
					}
				}
			}
		}, 20000);
	}
	
	private void startPhaseTask() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 21755, 60, getOwner()).useNoAnimationSkill();
		// 你一定是想找死。那就如你所愿！ / You must want to perish. So be it!
		sendMsg(1501395, getObjectId(), false, 0);
		// 冻结并面对遗忘！ / Freeze and face oblivion!
		sendMsg(1501397, getObjectId(), false, 5000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					deleteNpcs(282746); //Lava.
                    int total = explosiveSacrifice(282746); //Lava.
                    if (total == 0 || (8 - total) != 0) {
					    for (int i = 0; i < (8 - total); i++) {
					    	rndSpawn(282746); //Lava.
					    }
                    }
					startSpecialSkillTask();
				}
			}
		}, 4000);
	}
	
	private void startSpecialSkillTask() {
		specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 21761, 60, getOwner()).useNoAnimationSkill();
					specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							if (!isAlreadyDead()) {
								GameEngineServices.skillEngine().getSkill(getOwner(), 21762, 60, getOwner()).useNoAnimationSkill();
								specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									@Override
									public void run() {
										if (!isAlreadyDead()) {
											GameEngineServices.skillEngine().getSkill(getOwner(), 21763, 60, getOwner()).useNoAnimationSkill();
											if (curentPercent <= 63) {
												specialSkillTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
													@Override
													public void run() {
														if (!isAlreadyDead()) {
															GameEngineServices.skillEngine().getSkill(getOwner(), 21764, 60, getOwner()).useNoAnimationSkill();
															// 哈哈哈！见识真正的力量！ / Muhahaha! Experience true power!
															sendMsg(1501157, getObjectId(), false, 0);
															GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
																@Override
																public void run() {
																	if (!isAlreadyDead()) {
																		deleteNpcs(282747); //Lava.
																		rndSpawn(282747); //Lava.
																		rndSpawn(282747); //Lava.
																	}
																}
															}, 2000);
														}
													}
												}, 21000);
											}
										}
									}
								}, 3500);
							}
						}
					}, 1500);
				}
			}
		}, 12000);
	}
	
	private void cancelSpecialSkillTask() {
		if (specialSkillTask != null && !specialSkillTask.isDone()) {
			specialSkillTask.cancel(true);
		}
	}
	
	private void deleteNpcs(final int npcId) {
		if (getKnownList() != null) {
			getKnownList().doOnAllNpcs(new Visitor<Npc>() {
				@Override
				public void visit(Npc npc) {
					if (npc.getNpcId() == npcId) {
						NpcActions.delete(npc);
					}
				}
			});
		}
	}
	
    private int explosiveSacrifice(final int npcId) {
        final AtomicInteger total = new AtomicInteger();
        if (getKnownList() != null) {
            getKnownList().doOnAllNpcs(new Visitor<Npc>() {
                @Override
                public void visit(Npc npc) {
                    if (npc.getNpcId() == npcId) {
                        total.incrementAndGet();
                    }
                }
            });
        }
        return total.get();
    }
	
	private void cancelPhaseTask() {
		if (phaseTask != null && !phaseTask.isDone()) {
			phaseTask.cancel(true);
		}
	}
	
	private void cancelThinkTask() {
		if (thinkTask != null && !thinkTask.isDone()) {
			thinkTask.cancel(true);
		}
	}
	
	private void rndSpawn(int npcId) {
		float direction = Rnd.get(0, 199) / 100f;
		int distance = Rnd.get(1, 25);
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		spawn(npcId, 1191.4962f + x1, 360.13733f + y1, 128.5f, p.getHeading());
	}
	
	private void addPercent() {
		percents.clear();
		Collections.addAll(percents, 90, 84, 79, 75, 72, 70, 67, 59, 53, 47, 44, 43, 39, 35, 30, 26, 23, 21, 16, 11, 6);
	}
	
	@Override
	protected void handleDespawned() {
		cancelThinkTask();
		cancelPhaseTask();
		cancelSpecialSkillTask();
		percents.clear();
		super.handleDespawned();
	}
	
	@Override
	protected void handleDied() {
		// 呃……我……还会回来…… / Urgh… I... shall return...
		sendMsg(1501396, getObjectId(), false, 0);
		cancelThinkTask();
		cancelPhaseTask();
		cancelSpecialSkillTask();
		percents.clear();
		deleteNpcs(282746); //Lava.
		deleteNpcs(282747); //Lava.
		deleteNpcs(855262); //Explosive Sacrifice.
		final WorldPosition p = getPosition();
		if (p != null) {
			sendBerserkAnohaGuide();
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 狂暴阿诺哈已被击败。 / Berserk Anoha has been defeated.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Anoha_Die);
			}
		});
		AI2Actions.deleteOwner(this);
		super.handleDied();
	}
	
	private void sendBerserkAnohaGuide() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					HTMLService.sendGuideHtml(player, "Berserk_Anoha");
				}
			}
		});
	}
	
	@Override
	protected void handleBackHome() {
		think = true;
		cancelThinkTask();
		cancelPhaseTask();
		cancelSpecialSkillTask();
		addPercent();
		curentPercent = 100;
		deleteNpcs(282746); //Lava.
		deleteNpcs(282747); //Lava.
		deleteNpcs(855262); //Explosive Sacrifice.
		isAggred.set(false);
		super.handleBackHome();
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
