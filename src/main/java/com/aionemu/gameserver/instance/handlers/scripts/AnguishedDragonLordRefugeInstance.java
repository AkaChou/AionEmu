package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 苦难龙王避难所副本事件处理器。
 * Instance event handler for Anguished Dragon Lord Refuge.
 *
 * @author Encom
 */

@InstanceID(300630000)
public class AnguishedDragonLordRefugeInstance extends GeneralInstanceHandler
{
	/** tiamat buff / tiamat buff */
		private int tiamatBuff;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		spawn(833483, 496.42648f, 516.493f, 240.26653f, (byte) 0); //Kahrun (Reian Leader).
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				spawnIDTiamatDrakanNamed65Al();
			}
		}, 180000);
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
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 702729: //Tiamat's Huge Treasure Crate.
				for (Player player: instance.getPlayersInside()) {
				    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
					if (player.isOnline()) {
					    switch (Rnd.get(1, 2)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053640, 1)); //Balaur Lord's Mythic Weapon Box.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053707, 1)); //Glimmering Treasure Chest Of Balaur Lord Tiamat.
				            break;
						}
					}
				}
			break;
			case 802182: //Dragon Lord's Refuge Opportunity Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
			break;
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
		    case 236713: //Noble Drakan Figther.
			case 236714: //Noble Drakan Wizard.
			case 236715: //Noble Drakan Sorcerer.
			case 236716: //Noble Drakan Clerc.
			case 236717: //Sardha Drakan Figther.
			case 236718: //Sardha Drakan Wizard.
			case 236719: //Sardha Drakan Sorcerer.
			case 236720: //Sardha Drakan Clerc.
			    despawnNpc(npc);
			break;
			case 856483: //龙族通灵师。 / Balaur Spiritualist.
			case 856484: //龙族通灵师。 / Balaur Spiritualist.
			case 856485: //龙族通灵师。 / Balaur Spiritualist.
			case 856486: //龙族通灵师。 / Balaur Spiritualist.
			    despawnNpc(npc);
				// 主神吸收了龙族通灵师的精神能量！ / The Empyrean Lord absorbed the Balaur Spiritualist's mental energy!
				sendMsgByRace(1401551, Race.PC_ALL, 0);
			break;
		    case 236274: //Calindi Flamelord.
			    despawnNpc(npc);
				killNpc(getNpcs(730696)); //Surkana.
				killNpc(getNpcs(283130)); //Blaze Engraving.
				killNpc(getNpcs(283132)); //Blaze Engraving.
			    if (getNpcs(236274).isEmpty()) { //Calindi Flamelord.
				    spawnIDTiamatDragonNamed65Al();
			    } if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
						    sendMovie(player, 882);
						    spawnIDTiamatT1CrackKeyNamed65Al();
							// 进入内部通道，在凯希内尔对付提亚马特时摧毁其化身。 / Enter the Internal Passage and destroy Tiamat's Incarnations while Kaisinel is dealing with Tiamat.
							sendMsgByRace(1401531, Race.ELYOS, 0);
							// 与提亚马特的战斗将在 30 分钟后自动结束。 / The battle with Tiamat will automatically end in 30 minutes.
							sendMsgByRace(1401547, Race.ELYOS, 10000);
							// 主神凯希内尔正全力进攻。 / Empyrean Lord Kaisinel is attacking with all his might.
							sendMsgByRace(1401538, Race.ELYOS, 15000);
							// 消灭龙族通灵师，为主神施加增益。 / Eliminate the Balaur Spiritualist to grant a beneficial effect to the Empyrean Lord.
							sendMsgByRace(1401550, Race.ELYOS, 25000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						        /**
						         * 处理 run。
						         * Handle run.
						         */
						        @Override
						        public void run() {
									startGodKaisinelEvent();
									spawn(283175, 551.78796f, 514.75494f, 417.40436f, (byte) 60); //Kaisinel Teleport.
								}
						    }, 15000);
						    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					            /**
					             * 处理 run。
					             * Handle run.
					             */
					            @Override
					            public void run() {
						            startRushWalkEvent1();
						            spawnIDTiamatFOBJTeleportFuture1();
						            spawn(856486, 463f, 461f, 417.405f, (byte) 17); //龙族通灵师。 / Balaur Spiritualist.
				                }
			                }, 25000);
						break;
						case ASMODIANS:
							sendMovie(player, 884);
							spawnIDTiamatT1CrackKeyNamed65Al();
							// 进入内部通道，在凯希内尔对付提亚马特时摧毁其化身。 / Enter the Internal Passage and destroy Tiamat's Incarnations while Kaisinel is dealing with Tiamat.
							sendMsgByRace(1401532, Race.ASMODIANS, 0);
							// 与提亚马特的战斗将在 30 分钟后自动结束。 / The battle with Tiamat will automatically end in 30 minutes.
							sendMsgByRace(1401547, Race.ASMODIANS, 10000);
							// 主神玛尔库坦正全力进攻。 / Empyrean Lord Marchutan is attacking with all his might.
							sendMsgByRace(1401539, Race.ASMODIANS, 15000);
							// 消灭龙族通灵师，为主神施加增益。 / Eliminate the Balaur Spiritualist to grant a beneficial effect to the Empyrean Lord.
							sendMsgByRace(1401550, Race.ASMODIANS, 25000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						        /**
						         * 处理 run。
						         * Handle run.
						         */
						        @Override
						        public void run() {
									startGodMarchutanEvent();
									spawn(283176, 551.78796f, 514.75494f, 417.40436f, (byte) 60); //Marchutan Teleport.
								}
						    }, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					            /**
					             * 处理 run。
					             * Handle run.
					             */
					            @Override
					            public void run() {
						            startRushWalkEvent1();
						            spawnIDTiamatFOBJTeleportFuture1();
						            spawn(856486, 463f, 461f, 417.405f, (byte) 17); //龙族通灵师。 / Balaur Spiritualist.
					            }
				            }, 25000);
				        break;
					}
			    }
			    instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
					    // 龙主提亚马特以死亡咆哮击败了主神。 / Dragon Lord Tiamat used its Death Roar to defeat the Empyrean Lord.
						sendMsgByRace(1401542, Race.PC_ALL, 0);
						GameEngineServices.skillEngine().applyEffectDirectly(20920, player, player, 30000); //Dragon Lord's Roar.
				    }
			    });
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
							    player.getEffectController().removeEffect(20920); //Dragon Lord's Roar.
						    }
					    });
				    }
			    }, 10000);
			break;
		    case 236278: //Fissurefang.
			    despawnNpc(npc);
				spawnIDTiamatT1GravityKeyNamed65Al();
				Npc tiamatTrue1 = instance.getNpc(236276); //提亚马特。 / Tiamat.
				tiamatBuff++;
				if (tiamatTrue1 != null) {
				    if (tiamatBuff == 1) {
					    tiamatTrue1.getEffectController().removeEffect(20975); //Fissure Incarnate.
				    }
				}
			    // 裂隙化身已崩塌。 / Fissure Incarnate has collapsed.
				sendMsgByRace(1401533, Race.PC_ALL, 0);
			    despawnNpc(getNpc(730673)); //Internal Passage In 1.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
					    startRushWalkEvent2();
					    spawnIDTiamatFOBJTeleportFuture2();
					    spawn(856485, 545f, 461f, 417.405f, (byte) 46); //龙族通灵师。 / Balaur Spiritualist.
				    }
			    }, 5000);
		    break;
		    case 236279: //Graviwing.
			    despawnNpc(npc);
				spawnIDTiamatT1RageKeyNamed65Al();
				Npc tiamatTrue2 = instance.getNpc(236276); //提亚马特。 / Tiamat.
				tiamatBuff++;
				if (tiamatTrue2 != null) {
				    if (tiamatBuff == 2) {
					    tiamatTrue2.getEffectController().removeEffect(20977); //Gravity Incarnate.
				    }
				}
			    // 重力化身已崩塌。 / Gravity Incarnate has collapsed.
				sendMsgByRace(1401535, Race.PC_ALL, 0);
			    despawnNpc(getNpc(730674)); //Internal Passage In 2.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
					    startRushWalkEvent3();
					    spawnIDTiamatFOBJTeleportFuture3();
					    spawn(856484, 463f, 568f, 417.405f, (byte) 105); //龙族通灵师。 / Balaur Spiritualist.
				    }
			    }, 5000);
		    break;
		    case 236280: //Wrathclaw.
			    despawnNpc(npc);
				spawnIDTiamatT1CrystalKeyNamed65Al();
				Npc tiamatTrue3 = instance.getNpc(236276); //提亚马特。 / Tiamat.
				tiamatBuff++;
				if (tiamatTrue3 != null) {
				    if (tiamatBuff == 3) {
					    tiamatTrue3.getEffectController().removeEffect(20976); //Wrath Incarnate.
				    }
				}
				// 愤怒化身已崩塌。 / Wrath Incarnate has collapsed.
				sendMsgByRace(1401534, Race.PC_ALL, 0);
			    despawnNpc(getNpc(730675)); //Internal Passage In 3.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
					    startRushWalkEvent4();
					    spawnIDTiamatFOBJTeleportFuture4();
					    spawn(856483, 545f, 568f, 417.405f, (byte) 78); //龙族通灵师。 / Balaur Spiritualist.
				    }
			    }, 5000);
		    break;
			case 236281: //Petriscale.
			    despawnNpc(npc);
				despawnNpc(getNpc(236276)); //提亚马特。 / Tiamat.
				despawnNpc(getNpc(219488)); //God Kaisinel.
				despawnNpc(getNpc(219491)); //God Marchutan.
				despawnNpc(getNpc(730676)); //Internal Passage In 4.
				Npc tiamatTrue4 = instance.getNpc(236276); //提亚马特。 / Tiamat.
				tiamatBuff++;
				if (tiamatTrue4 != null) {
				    if (tiamatBuff == 4) {
					    tiamatTrue4.getEffectController().removeEffect(20978); //Petrification Incarnate.
						tiamatTrue4.getEffectController().removeEffect(20984); //Unbreakable Wing.
				    }
				}
				// 重力化身已崩塌。 / Gravity Incarnate has collapsed.
				sendMsgByRace(1401536, Race.PC_ALL, 0);
				if (getNpcs(236281).isEmpty()) { //Petriscale.
				    spawnIDTiamatDragonDyingNamed65Al();
			    } if (player != null) {
				    switch (player.getRace()) {
				        case ELYOS:
					        kaisinelLight();
							// 提亚马特的所有化身均已崩塌。 / All of Tiamat's Incarnations have collapsed.
							sendMsgByRace(1401537, Race.ELYOS, 2000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						        /**
						         * 处理 run。
						         * Handle run.
						         */
						        @Override
						        public void run() {
									spawnGodKaisinelGroggy();
									// 主神凯希内尔已力竭。你必须接手对抗提亚马特！ / Empyrean Lord Kaisinel is exhausted. You must take over the fight against Tiamat!
									sendMsgByRace(1401540, Race.ELYOS, 5000);
									Npc godKaisinel1 = getNpc(219489);
									// 我在变弱。守护者们必须接替。 / I am weakening. You Daevas must take your turn.
									GameFeatureServices.npcShoutsService().sendMsg(godKaisinel1, 1500686, godKaisinel1.getObjectId(), 0, 20000);
									// 我们还不算太晚！ / We're not too late!
									GameFeatureServices.npcShoutsService().sendMsg(godKaisinel1, 1500687, godKaisinel1.getObjectId(), 0, 30000);
								}
						    }, 15000);
						break;
						case ASMODIANS:
					        marchutanGrace();
							// 提亚马特的所有化身均已崩塌。 / All of Tiamat's Incarnations have collapsed.
							sendMsgByRace(1401537, Race.ASMODIANS, 2000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						        /**
						         * 处理 run。
						         * Handle run.
						         */
						        @Override
						        public void run() {
									spawnGodMarchutanGroggy();
									// 主神玛尔库坦已力竭。你必须接手对抗提亚马特！ / Empyrean Lord Marchutan is exhausted. You must take over the fight against Tiamat!
									sendMsgByRace(1401541, Race.ASMODIANS, 5000);
									Npc godMarchutan1 = getNpc(219492);
									// 我须稍作休息。期间挡住那条龙！ / I must rest a moment. Hold off the Dragon while I do!
									GameFeatureServices.npcShoutsService().sendMsg(godMarchutan1, 1500690, godMarchutan1.getObjectId(), 0, 20000);
									// 你，你一定是…… / You, you must be...
									GameFeatureServices.npcShoutsService().sendMsg(godMarchutan1, 1500691, godMarchutan1.getObjectId(), 0, 30000);
								}
						    }, 15000);
				        break;
					}
				}
			break;
		    case 236277: //Tiamat Dying.
			    despawnNpc(npc);
				if (player != null) {
				    switch (player.getRace()) {
			            case ELYOS:
				            sendMovie(player, 883);
				            spawn(833486, 504.4801f, 515.12964f, 417.40436f, (byte) 60); //Kaisinel.
							Npc godKaisinel2 = getNpc(833486);
							// 终于结束了。 / It is finally over.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500695, godKaisinel2.getObjectId(), 0, 5000);
							// 即便龙主也可能陷入疯狂。 / Even a Dragon Lord can be driven to madness.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500696, godKaisinel2.getObjectId(), 0, 15000);
							// 不必谢我。现在，把遗物给我，我就走。 / No need to thank me. Now, give me the relics, and I will be gone.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500625, godKaisinel2.getObjectId(), 0, 25000);
							// 是吗？你干得可真漂亮。 / Yes ? And such a fine job you've done.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500626, godKaisinel2.getObjectId(), 0, 35000);
							// 你竟敢在此高声喧哗？安静。 / How dare you raise your voice in this place ? Be silent.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500627, godKaisinel2.getObjectId(), 0, 45000);
							// 伊斯拉菲尔？在这里？ / Israphel? Here?
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500628, godKaisinel2.getObjectId(), 0, 55000);
							//No!
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500629, godKaisinel2.getObjectId(), 0, 65000);
							// 你打算拿那些做什么？ / What do you think you're going to do with those ?
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500630, godKaisinel2.getObjectId(), 0, 75000);
							// 叛徒有一点说对了。我们必须搁置争论去追他。 / The traitor was right about one thing. We must put our argument aside and go after him.
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500631, godKaisinel2.getObjectId(), 0, 85000);
							// 啊，卡伦。是不是有点野心太大了？ / Ah, Kahrun. Getting a little ambitious, don't you think ?
							GameFeatureServices.npcShoutsService().sendMsg(godKaisinel2, 1500632, godKaisinel2.getObjectId(), 0, 95000);
				        break;
			            case ASMODIANS:
				            sendMovie(player, 885);
				            spawn(833487, 504.4801f, 515.12964f, 417.40436f, (byte) 60); //Marchutan.
							Npc godMarchutan2 = getNpc(833487);
							// 终于，提亚马特死了。 / Finally, Tiamat is dead.
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500698, godMarchutan2.getObjectId(), 0, 5000);
							// 在终结前，我看到了它的绝望。 / I saw its despair, before the end.
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500699, godMarchutan2.getObjectId(), 0, 15000);
							// 若要谢我，就把那些遗物交给我保管。 / You can thank me by giving me those relics, for safekeeping.
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500633, godMarchutan2.getObjectId(), 0, 25000);
							// 那个时代结束了，卡伦。 / That time is over, Kahrun.
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500634, godMarchutan2.getObjectId(), 0, 35000);
							// 你连提亚马特都打不过，凭什么以为能阻止我？ / You didn't have the power to fight Tiamat, so what makes you think you can stop me ?
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500635, godMarchutan2.getObjectId(), 0, 45000);
							// 你？你想要什么？ / You ? What do you want ?
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500636, godMarchutan2.getObjectId(), 0, 55000);
							// 停下！ / Stop!
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500637, godMarchutan2.getObjectId(), 0, 65000);
							// 你以为靠那些能走多远？ / How far do you think you'll get with those ?
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500638, godMarchutan2.getObjectId(), 0, 75000);
							// 我们必须联合阻止他——但别以为争论结束了！ / We must unite to stop him--but don't think that this argument is over!
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500639, godMarchutan2.getObjectId(), 0, 85000);
							// 哦？这是什么？争吵？胜利才刚过去就…… / Well well. What's this ? Bickering ? And so soon after your glorious victory.
							GameFeatureServices.npcShoutsService().sendMsg(godMarchutan2, 1500640, godMarchutan2.getObjectId(), 0, 95000);
				        break;
					}
			    }
/* 				spawnAbbeyNobleBox(); */
				spawnTiamatHugeTreasureCrate();
			    killNpc(getNpcs(701502)); //Siel's Relic.
				despawnNpc(getNpc(219489)); //God Kaisinel Tired.
				despawnNpc(getNpc(219492)); //God Marchutan Tired.
			    killNpc(getNpcs(730694)); //Tiamat Aetheric Field.
			    spawn(800430, 500.61713f, 507.2179f, 417.40436f, (byte) 0); //Kahrun.
			    spawn(800464, 546.452f, 516.3783f, 417.40436f, (byte) 111);  //Reian Sorcerer.
			    spawn(800465, 546.79755f, 512.78314f, 417.40436f, (byte) 10); //Reian Sorcerer.
				spawn(802182, 487.20517f, 507.40265f, 417.40436f, (byte) 8); //Dragon Lord's Refuge Opportunity Bundle.
				spawn(833482, 548.29999f, 514.59998f, 420.04001f, (byte) 0, 23); //Dragon Lord's Refuge Exit.
				spawn(730704, 437.54105f, 513.48688f, 415.82394f, (byte) 0, 17); //Collapsed Debris.
		    break;
			case 219488: //God Kaisinel.
			    if (!getNpcs(236276).isEmpty()) //提亚马特。 / Tiamat.
				    despawnNpc(getNpc(236276));
			    if (!getNpcs(236278).isEmpty()) //Fissurefang.
				    despawnNpc(getNpc(236278));
			    if (!getNpcs(236279).isEmpty()) //Graviwing.
				    despawnNpc(getNpc(236279));
			    if (!getNpcs(236280).isEmpty()) //Wrathclaw.
				    despawnNpc(getNpc(236280));
			    if (!getNpcs(236281).isEmpty()) //Petriscale.
				    despawnNpc(getNpc(236281));
			    instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
					    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.IDTIAMAT_TIAMAT_COUNTDOWN_OVER);
				    }
			    });
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
							    onExitInstance(player);
						    }
					    });
					    onInstanceDestroy();
				    }
			    }, 10000);
			break;
		    case 219491: //God Marchutan.
			    if (!getNpcs(236276).isEmpty()) //提亚马特。 / Tiamat.
				    despawnNpc(getNpc(236276));
			    if (!getNpcs(236278).isEmpty()) //Fissurefang.
				    despawnNpc(getNpc(236278));
			    if (!getNpcs(236279).isEmpty()) //Graviwing.
				    despawnNpc(getNpc(236279));
			    if (!getNpcs(236280).isEmpty()) //Wrathclaw.
				    despawnNpc(getNpc(236280));
			    if (!getNpcs(236281).isEmpty()) //Petriscale.
				    despawnNpc(getNpc(236281));
			    instance.doOnAllPlayers(new Visitor<Player>() {
				    /**
				     * 处理 visit。
				     * Handle visit.
				     *
				     * @param player 玩家 / player
				     */
				    @Override
				    public void visit(Player player) {
					    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.IDTIAMAT_TIAMAT_COUNTDOWN_OVER);
				    }
			    });
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
							    onExitInstance(player);
						    }
					    });
					    onInstanceDestroy();
				    }
			    }, 10000);
		    break;
	    }
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(20932); //Kaisinel's Light.
		effectController.removeEffect(20936); //Marchutan's Grace.
	}
	
	// 凯希内尔之光。 / Kaisinel's Light.
	private void kaisinelLight() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(20932); //Kaisinel's Light.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
	
	// 玛尔库坦的恩典。 / Marchutan's Grace.
	private void marchutanGrace() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(20936); //Marchutan's Grace.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
	
	// 阶段：提亚马特。 / PHASE TIAMAT.
	private void spawnIDTiamatDrakanNamed65Al() {
		spawn(236275, 470.5909f, 515.02856f, 417.40436f, (byte) 119); //提亚马特。 / Tiamat.
	}
	private void spawnIDTiamatDragonNamed65Al() {
		spawn(236276, 457.7215f, 514.4464f, 417.53998f, (byte) 0); //IDTiamat_Dragon_Named_65_Al.
	}
	private void spawnIDTiamatDragonDyingNamed65Al() {
		spawn(236277, 458.36316f, 514.46686f, 417.40436f, (byte) 0); //Tiamat Dying.
	}
	private void spawnTiamatHugeTreasureCrate() {
		spawn(702729, 485.79965f, 514.46466f, 417.40436f, (byte) 119); //Tiamat's Huge Treasure Crate.
	}
	private void spawnAbbeyNobleBox() {
		switch (Rnd.get(1, 2)) {
		    case 1:
				spawn(702658, 488.25827f, 505.1509f, 417.40436f, (byte) 11); //修道院箱子。 / Abbey Box.
			break;
			case 2:
				spawn(702659, 488.25827f, 505.1509f, 417.40436f, (byte) 11); //高级修道院箱子。 / Noble Abbey Box.
			break;
		}
	}
	
	private void eventGodAttack(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	// 阶段：主神凯希内尔。 / PHASE GOD KASINEL.
	private void startGodKaisinelEvent() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				eventGodAttack((Npc)spawn(219488, 551.78796f, 514.75494f, 417.40436f, (byte) 60), 480.363f, 514.3989f, 417.40436f, false); //God Kaisinel.
			}
		}, 1000);
	}
	private void spawnGodKaisinelGroggy() {
		spawn(219489, 507.17175f, 513.7484f, 417.40436f, (byte) 59); //God Kaisinel Tired.
	}
	
	// 阶段：主神玛尔库坦。 / PHASE GOD MARCHUTAN.
	private void startGodMarchutanEvent() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
		        eventGodAttack((Npc)spawn(219491, 551.78796f, 514.75494f, 417.40436f, (byte) 60), 480.363f, 514.3989f, 417.40436f, false); //God Marchutan.
			}
		}, 1000);
	}
	private void spawnGodMarchutanGroggy() {
		spawn(219492, 507.17175f, 513.7484f, 417.40436f, (byte) 59); //God Marchutan Tired.
	}
	
	// 阶段 4 龙。 / PHASE 4 DRAGON.
	private void spawnIDTiamatT1CrackKeyNamed65Al() {
		spawn(236278, 196.67767f, 176.11638f, 246.07117f, (byte) 8); //Fissurefang.
	}
	private void spawnIDTiamatT1GravityKeyNamed65Al() {
		spawn(236279, 799.8529f, 176.94928f, 246.07117f, (byte) 39); //Graviwing.
	}
	private void spawnIDTiamatT1RageKeyNamed65Al() {
		spawn(236280, 199.11307f, 848.60956f, 246.07117f, (byte) 110); //Wrathclaw.
	}
	private void spawnIDTiamatT1CrystalKeyNamed65Al() {
		spawn(236281, 796.535f, 849.48615f, 246.07117f, (byte) 72); //Petriscale.
	}
	
	// 传送者。 / TELEPORTER.
	private void spawnIDTiamatFOBJTeleportFuture1() {
		spawn(730673, 461.24423f, 458.91919f, 416.62000f, (byte) 0, 35); //Internal Passage I.
	}
	private void spawnIDTiamatFOBJTeleportFuture2() {
		spawn(730674, 546.12146f, 459.33582f, 416.62000f, (byte) 0, 33); //Internal Passage II.
	}
	private void spawnIDTiamatFOBJTeleportFuture3() {
		spawn(730675, 461.45767f, 570.08691f, 416.61667f, (byte) 0, 31); //Internal Passage III.
	}
	private void spawnIDTiamatFOBJTeleportFuture4() {
		spawn(730676, 546.47882f, 570.13873f, 416.62000f, (byte) 0, 32); //Internal Passage IV.
	}
	
	// 阶段：突击。 / PHASE RUSH.
	private void rushWalk(final Npc npc) {
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
	 * 处理 startRushWalkEvent1。
	 * Handle startRushWalkEvent1.
	 */
	
	public void startRushWalkEvent1() {
		rushWalk((Npc)spawn(236719, 468.89908f, 463.28857f, 417.40436f, (byte) 16)); //Sardha Drakan Sorcerer.
		rushWalk((Npc)spawn(236720, 467.41974f, 466.10922f, 417.40436f, (byte) 13)); //Sardha Drakan Clerc.
		rushWalk((Npc)spawn(236714, 544.04144f, 469.6464f, 417.40436f, (byte) 52)); //Noble Drakan Wizard.
	}
	/**
	 * 处理 startRushWalkEvent2。
	 * Handle startRushWalkEvent2.
	 */
	
	public void startRushWalkEvent2() {
		rushWalk((Npc)spawn(236713, 540.9507f, 466.07214f, 417.40436f, (byte) 42)); //Noble Drakan Figther.
		rushWalk((Npc)spawn(236714, 544.04144f, 469.6464f, 417.40436f, (byte) 52)); //Noble Drakan Wizard.
		rushWalk((Npc)spawn(236715, 536.7774f, 463.96362f, 417.40436f, (byte) 33)); //Noble Drakan Sorcerer.
	}
	
	private void startRushWalkEvent3() {
		rushWalk((Npc)spawn(236716, 462.77353f, 562.71106f, 417.40436f, (byte) 77)); //Noble Drakan Clerc.
		rushWalk((Npc)spawn(236717, 467.94543f, 567.6658f, 417.40436f, (byte) 85)); //Sardha Drakan Figther.
		rushWalk((Npc)spawn(236718, 464.2729f, 566.56067f, 417.40436f, (byte) 67)); //Sardha Drakan Wizard.
	}
	/**
	 * 处理 startRushWalkEvent4。
	 * Handle startRushWalkEvent4.
	 */
	
	public void startRushWalkEvent4() {
		rushWalk((Npc)spawn(236716, 542.7636f, 565.65045f, 417.40436f, (byte) 77)); //Noble Drakan Clerc.
		rushWalk((Npc)spawn(236717, 538.6315f, 566.12714f, 417.40436f, (byte) 85)); //Sardha Drakan Figther.
		rushWalk((Npc)spawn(236718, 544.4505f, 561.9321f, 417.40436f, (byte) 67)); //Sardha Drakan Wizard.
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	/**
	 * 移除指定 NPC。
	 * Despawn the given NPC.
	 *
	 * npc
	 */
	
	protected void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 处理 despawnNpcs。
	 * Handle despawnNpcs.
	 *
	 * npcs
	 */
	
	protected void despawnNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 返回 npc。
	 * Return the npc.
	 *
	 * NPC
	 * result
	 */
	
	protected Npc getNpc(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpc(npcId);
		}
		return null;
	}
	/**
	 * 返回 npcs。
	 * Return the npcs.
	 *
	 * NPC
	 * result
	 */
	
	protected List<Npc> getNpcs(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpcs(npcId);
		}
		return null;
	}
	/**
	 * 处理 killNpc。
	 * Handle killNpc.
	 *
	 * npcs
	 */
	
	protected void killNpc(List<Npc> npcs) {
        for (Npc npc: npcs) {
            npc.getController().die();
        }
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
	
	private void sendMovie(Player player, int movie) {
		if (!movies.contains(movie)) {
			movies.add(movie);
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
		}
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		movies.clear();
	}
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}