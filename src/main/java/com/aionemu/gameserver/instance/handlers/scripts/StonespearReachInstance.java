/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.StonespearReachReward;
import com.aionemu.gameserver.model.instance.playerreward.StonespearReachPlayerReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/****/
/** Author (Encom)
/****/

@InstanceID(301500000)
public class StonespearReachInstance extends GeneralInstanceHandler {

	private int rank;
	private Race spawnRace;
	private long startTime;
	private Future<?> timerPrepare;
	private Future<?> timerInstance;
	private boolean isInstanceDestroyed;
	
	//Preparation Time.
	private int prepareTimerSeconds = 60000; //...1Min
	//Duration Instance Time.
	private int instanceTimerSeconds = 1800000; //...30Min
	private StonespearReachReward instanceReward;
	private final List<Future<?>> stonespearTask1 = new ArrayList<>();
	private final List<Future<?>> stonespearTask2 = new ArrayList<>();
	private final List<Future<?>> stonespearTask3 = new ArrayList<>();
	private final List<Future<?>> stonespearTask4 = new ArrayList<>();
	private final List<Future<?>> stonespearTask5 = new ArrayList<>();
	
	private static final float[][] SPAWN_POSITIONS = {
		{211.05080f, 264.03802f, 96.53291f, 0},
		{217.06422f, 248.22205f, 96.25f, 17},
		{231.39449f, 243.60184f, 96.36497f, 31},
		{245.20996f, 250.43109f, 96.07562f, 44},
		{251.58972f, 264.37146f, 96.30522f, 59},
		{243.75105f, 279.34222f, 96.25f, 77},
		{230.97932f, 285.57825f, 96.418526f, 89},
		{217.75461f, 277.61115f, 96.02431f, 104}
	};
	
	private static final float[][] KEBABIT_POSITIONS = {
		{208.48062f, 256.79190f, 96.25000f, 5},
		{253.01332f, 275.73624f, 96.23518f, 69},
		{223.37283f, 286.79090f, 96.25000f, 96},
		{236.64775f, 241.84962f, 95.93428f, 30}
	};
	
	private static final float[][] BLASTSTONE_POSITIONS = {
		{251.47273f, 264.46713f, 96.30522f, 61},
		{230.85971f, 285.67032f, 96.41852f, 90},
		{211.20746f, 264.05276f, 96.53291f, 0},
		{231.29951f, 243.66095f, 96.36497f, 29}
	};
	
	private enum RaidType {
		ROUND_1(new int[]{855765, 855766, 855767}, new int[]{855768, 855769, 855770}, new int[]{855771, 855772, 855773}),
		ROUND_2(new int[]{855788, 855789, 855790}, new int[]{855791, 855792, 855793}, new int[]{855794, 855795, 855796}),
		ROUND_3(new int[]{855811, 855812, 855813}, new int[]{855814, 855815, 855816}, new int[]{855817, 855818, 855819}),
		ROUND_4(new int[]{855834, 855835, 855836}, new int[]{855837, 855838, 855839}, new int[]{855840, 855841, 855842});
		
		private final int[] firstWave;
		private final int[] secondWave;
		private final int[] thirdWave;
		
		RaidType(int[] firstWave, int[] secondWave, int[] thirdWave) {
			this.firstWave = firstWave;
			this.secondWave = secondWave;
			this.thirdWave = thirdWave;
		}
		
		public int[] getFirstWave() { return firstWave; }
		public int[] getSecondWave() { return secondWave; }
		public int[] getThirdWave() { return thirdWave; }
	}
	
	protected StonespearReachPlayerReward getPlayerReward(Integer object) {
		return (StonespearReachPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new StonespearReachPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
		}
	}
	
	private void spawnTerritoryManager() {
		final int territoryManager = spawnRace == Race.ASMODIANS ? 833489 : 833488; //Legion Territory Manager.
		spawn(territoryManager, 165.91524f, 264.50375f, 97.454155f, (byte) 0);
    }
	
	private void spawnGuardianStone() {
		final int guardianStone = spawnRace == Race.ASMODIANS ? 856466 : 855763; //Guardian Stone.
		spawn(guardianStone, 231.26677f, 264.4961f, 95.7781f, (byte) 60);
    }
	
	private void spawnKebabit() {
		for (float[] pos : KEBABIT_POSITIONS) {
			spawn(856303, pos[0], pos[1], pos[2], (byte) pos[3]);
		}
	}
	
	private void spawnBlaststones(int npcId) {
		for (float[] pos : BLASTSTONE_POSITIONS) {
			spawn(npcId, pos[0], pos[1], pos[2], (byte) pos[3]);
		}
	}
	
	private void spawnRaidWave(final int npcId, int delay, final List<Future<?>> taskList) {
		taskList.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				for (float[] pos : SPAWN_POSITIONS) {
					stoneSpearRaid((Npc)spawn(npcId, pos[0], pos[1], pos[2], (byte) pos[3]));
				}
			}
		}, delay));
	}
	
	private void spawnRepeatingRaid(int npcId, List<Future<?>> taskList) {
		int[] delays = {1000, 10000, 20000, 30000, 40000, 50000, 60000};
		for (int delay : delays) {
			spawnRaidWave(npcId, delay, taskList);
		}
	}
	
	@Override
	public void onDie(Npc npc) {
		int points = 0;
		int npcId = npc.getNpcId();
		Player player = npc.getAggroList().getMostPlayerDamage();
		
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 856303: //Agitated Kebbit.
			    points = 1500;
			    break;
			case 856305: //Macadamic Jester.
			    points = 12000;
			    break;
			
			//** ROUND 1 **//
			case 855765:
			case 855766:
			case 855767:
			case 855768:
			case 855769:
			case 855770:
			case 855771:
			case 855772:
			case 855773:
			    points = 100;
			    break;
			case 855764: //Aetheric Field Blaststone.
			    points = 500;
			    break;
			case 855774:
			case 855775:
			case 855776:
			    points = 12000;
				stopInstanceTask1();
				//The second battle will begin in 2 minutes.
				sendMsgByRace(1402868, Race.PC_ALL, 2000);
				scheduleNextRound(60000, new Runnable() {
					@Override
					public void run() {
						startInstanceTask2();
					}
				});
				break;
			
			//** ROUND 2 **//
			case 855788:
			case 855789:
			case 855790:
			case 855791:
			case 855792:
			case 855793:
			case 855794:
			case 855795:
			case 855796:
			    points = 200;
			    break;
			case 855787: //Aetheric Field Blaststone.
			    points = 1000;
			    break;
			case 855797:
			case 855798:
			case 855799:
			    points = 21000;
				stopInstanceTask2();
				//The third battle will begin in 3 minutes.
				sendMsgByRace(1402869, Race.PC_ALL, 2000);
				scheduleNextRound(60000, new Runnable() {
					@Override
					public void run() {
						startInstanceTask3();
					}
				});
				break;
			
			//** ROUND 3 **//
			case 855811:
			case 855812:
			case 855813:
			case 855814:
			case 855815:
			case 855816:
			case 855817:
			case 855818:
			case 855819:
			    points = 300;
			    break;
			case 855810: //Aetheric Field Blaststone.
			    points = 1500;
			    break;
			case 855820:
			case 855821:
			case 855822:
			    points = 30000;
				stopInstanceTask3();
				//The fourth battle will begin in 4 minutes.
				sendMsgByRace(1402870, Race.PC_ALL, 2000);
				scheduleNextRound(60000, new Runnable() {
					@Override
					public void run() {
						startInstanceTask4();
					}
				});
				break;
			
			//** ROUND 4 **//
			case 855834:
			case 855835:
			case 855836:
			case 855837:
			case 855838:
			case 855839:
			case 855840:
			case 855841:
			case 855842:
			    points = 400;
			    break;
			case 855833: //Aetheric Field Blaststone.
			    points = 2000;
			    break;
			case 855843: //Vision Of Guardian General.
			    points = 42000;
				ThreadPoolManager.getInstance().schedule(new Runnable() {
					@Override
					public void run() {
						instance.doOnAllPlayers(new Visitor<Player>() {
							@Override
							public void visit(Player player) {
								stopInstance(player);
							}
						});
					}
				}, 5000);
				break;
			default:
				points = 0;
				break;
		}
		
		if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}
	
	private void scheduleNextRound(long delay, Runnable task) {
		ThreadPoolManager.getInstance().schedule(task, delay);
	}
	
	protected void startInstanceTask1() {
    	stonespearTask1.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				//The player has 1 min to prepare !!! [Timer Red]
				if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
					//Start the instance time !!! [Timer White]
					startCountDown();
					startMainInstanceTimer();
				}
				deleteNpc(833284);
				spawnGuardianStone();
				//The Aetheric Field is deactivated. The battle will now begin!
				sendMsgByRace(1402867, Race.PC_ALL, 0);
				//Protect the Guardian Stone for 2 minutes.
				sendMsgByRace(1402924, Race.PC_ALL, 2000);
				spawn(856305, 206.64789f, 263.70578f, 96.25f, (byte) 94); //Macadamic Jester.
				
				startRaidRound(RaidType.ROUND_1, 0);
			}
        }, 60000)); //...1Min
		
		stonespearTask1.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				deleteNpc(855763);
				deleteNpc(856466);
				//You have successfully protected the Guardian Stone and the stone has disappeared.
				sendMsgByRace(1402925, Race.PC_ALL, 0);
				
				startRaidRound(RaidType.ROUND_1, 1);
				spawnKebabit();
			}
        }, 120000)); //...2Min
		
		stonespearTask1.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnKebabit();
			}
		}, 180000)); //...3Min
		
		stonespearTask1.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				startRaidRound(RaidType.ROUND_1, 2);
				spawnBlaststones(855764);
				spawnKebabit();
			}
        }, 240000)); //...4Min
		
		stonespearTask1.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnBoss(855774, 855775, 855776);
				deleteNpc(856305);
				//The Guardian Stone and its attackers have all disappeared!
				sendMsgByRace(1402901, Race.PC_ALL, 0);
			}
        }, 300000)); //...5Min
	}
	
	protected void startInstanceTask2() {
    	stonespearTask2.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnGuardianStone();
				//Protect the Guardian Stone for 2 minutes.
				sendMsgByRace(1402924, Race.PC_ALL, 2000);
				spawn(856305, 206.64789f, 263.70578f, 96.25f, (byte) 94); //Macadamic Jester.
				startRaidRound(RaidType.ROUND_2, 0);
			}
        }, 60000)); //...1Min
		
		stonespearTask2.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				deleteNpc(855763);
				deleteNpc(856466);
				//You have successfully protected the Guardian Stone and the stone has disappeared.
				sendMsgByRace(1402925, Race.PC_ALL, 0);
				startRaidRound(RaidType.ROUND_2, 1);
				spawnKebabit();
			}
        }, 120000)); //...2Min
		
		stonespearTask2.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnKebabit();
			}
		}, 180000)); //...3Min
		
		stonespearTask2.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				startRaidRound(RaidType.ROUND_2, 2);
				spawnBlaststones(855787);
				spawnKebabit();
			}
        }, 240000)); //...4Min
		
		stonespearTask2.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnBoss(855797, 855798, 855799);
				deleteNpc(856305);
				//The Guardian Stone and its attackers have all disappeared!
				sendMsgByRace(1402901, Race.PC_ALL, 0);
			}
        }, 300000)); //...5Min
	}
	
	protected void startInstanceTask3() {
    	stonespearTask3.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnGuardianStone();
				//Protect the Guardian Stone for 2 minutes.
				sendMsgByRace(1402924, Race.PC_ALL, 2000);
				spawn(856305, 206.64789f, 263.70578f, 96.25f, (byte) 94); //Macadamic Jester.
				startRaidRound(RaidType.ROUND_3, 0);
			}
        }, 60000)); //...1Min
		
		stonespearTask3.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				deleteNpc(855763);
				deleteNpc(856466);
				//You have successfully protected the Guardian Stone and the stone has disappeared.
				sendMsgByRace(1402925, Race.PC_ALL, 0);
				startRaidRound(RaidType.ROUND_3, 1);
				spawnKebabit();
			}
        }, 120000)); //...2Min
		
		stonespearTask3.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnKebabit();
			}
		}, 180000)); //...3Min
		
		stonespearTask3.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				startRaidRound(RaidType.ROUND_3, 2);
				spawnBlaststones(855810);
				spawnKebabit();
			}
        }, 240000)); //...4Min
		
		stonespearTask3.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnBoss(855820, 855821, 855822);
				deleteNpc(856305);
				//The Guardian Stone and its attackers have all disappeared!
				sendMsgByRace(1402901, Race.PC_ALL, 0);
			}
        }, 300000)); //...5Min
	}
	
	protected void startInstanceTask4() {
    	stonespearTask4.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnGuardianStone();
				//Protect the Guardian Stone for 2 minutes.
				sendMsgByRace(1402924, Race.PC_ALL, 2000);
				spawn(856305, 206.64789f, 263.70578f, 96.25f, (byte) 94); //Macadamic Jester.
				startRaidRound(RaidType.ROUND_4, 0);
			}
        }, 60000)); //...1Min
		
		stonespearTask4.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				deleteNpc(855763);
				deleteNpc(856466);
				//You have successfully protected the Guardian Stone and the stone has disappeared.
				sendMsgByRace(1402925, Race.PC_ALL, 0);
				startRaidRound(RaidType.ROUND_4, 1);
				spawnKebabit();
			}
        }, 120000)); //...2Min
		
		stonespearTask4.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				spawnKebabit();
			}
		}, 180000)); //...3Min
		
		stonespearTask4.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				startRaidRound(RaidType.ROUND_4, 2);
				spawnBlaststones(855833);
				spawnKebabit();
			}
        }, 240000)); //...4Min
		
		stonespearTask4.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				deleteNpc(856305);
				//The Guardian Stone and its attackers have all disappeared!
				sendMsgByRace(1402901, Race.PC_ALL, 0);
				spawn(855843, 231.35631f, 264.5710f, 95.77810f, (byte) 58); //Vision Of Guardian General.
			}
        }, 300000)); //...5Min
	}
	
	private void startRaidRound(RaidType round, int waveIndex) {
		int[] npcIds;
		switch (waveIndex) {
			case 0:
				npcIds = round.getFirstWave();
				break;
			case 1:
				npcIds = round.getSecondWave();
				break;
			case 2:
				npcIds = round.getThirdWave();
				break;
			default:
				return;
		}
		int npcId = npcIds[Rnd.get(0, npcIds.length - 1)];
		spawnRepeatingRaid(npcId, getTaskListForRound(round, waveIndex));
	}
	
	private List<Future<?>> getTaskListForRound(RaidType round, int waveIndex) {
		if (round == RaidType.ROUND_1) {
			return stonespearTask1;
		} else if (round == RaidType.ROUND_2) {
			return stonespearTask2;
		} else if (round == RaidType.ROUND_3) {
			return stonespearTask3;
		} else {
			return stonespearTask4;
		}
	}
	
	private void spawnBoss(int... bossIds) {
		int bossId = bossIds[Rnd.get(0, bossIds.length - 1)];
		spawn(bossId, 231.35631f, 264.5710f, 95.77810f, (byte) 58);
	}
	
	protected void startCountDown() {
		stonespearTask5.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						stopInstance(player);
					}
				});
			}
        }, 1800000));
    }
	
	private void stoneSpearRaid(final Npc npc) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {
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
	
	private int getTime() {
		long result = (int) (System.currentTimeMillis() - startTime);
		return instanceTimerSeconds - (int) result;
	}
	
	private void sendPacket(final int nameId, final int point) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (nameId != 0) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), point));
				}
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
			}
		});
	}
	
	private int checkRank(int totalPoints) {
		if (totalPoints >= 71600) { //Rank S.
			rank = 1;
		} else if (totalPoints >= 41000) { //Rank A.
			rank = 2;
		} else if (totalPoints >= 26000) { //Rank B.
			rank = 3;
		} else if (totalPoints >= 14000) { //Rank C.
			rank = 4;
		} else if (totalPoints >= 8800) { //Rank D.
			rank = 5;
		} else {
			rank = 6;
		}
		return rank;
	}
	
	@Override
	public void onEnterInstance(final Player player) {
		super.onEnterInstance(player);
		if (!instanceReward.containPlayer(player.getObjectId())) {
			addPlayerReward(player);
		}
		StonespearReachPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		if (spawnRace == null) {
			spawnRace = player.getRace();
			spawnTerritoryManager();
		}
		startPrepareTimer();
	}
	
	private void startPrepareTimer() {
		if (timerPrepare == null) {
			timerPrepare = ThreadPoolManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					startMainInstanceTimer();
				}
			}, prepareTimerSeconds);
		}
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(prepareTimerSeconds, instanceReward, null));
			}
		});
	}
	
	private void startMainInstanceTimer() {
		if (!timerPrepare.isDone()) {
			timerPrepare.cancel(false);
		}
		startTime = System.currentTimeMillis();
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendPacket(0, 0);
	}
	
	protected void stopInstance(Player player) {
		stopInstanceTask1();
		stopInstanceTask2();
		stopInstanceTask3();
		stopInstanceTask4();
		instanceReward.setRank(checkRank(instanceReward.getPoints()));
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward(player);
		sendPacket(0, 0);
	}
	
	@Override
	public void doReward(Player player) {
		StonespearReachPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int reachRank = instanceReward.getRank();
			switch (reachRank) {
				case 1: //Rank S
				break;
				case 2: //Rank A
				break;
				case 3: //Rank B
				break;
				case 4: //Rank C
				break;
				case 5: //Rank D
				break;
			}
		}
	}
	
	@Override
	public void onExitInstance(Player player) {
		if (player.isInGroup2()) {
            PlayerGroupService.removePlayer(player);
        }
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		//"Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}
	
	private void stopInstanceTask1() {
		for (Future<?> task : stonespearTask1) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	private void stopInstanceTask2() {
		for (Future<?> task : stonespearTask2) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	private void stopInstanceTask3() {
		for (Future<?> task : stonespearTask3) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	private void stopInstanceTask4() {
		for (Future<?> task : stonespearTask4) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	private void stopInstanceTask5() {
		for (Future<?> task : stonespearTask5) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new StonespearReachReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		startInstanceTask1();
	}
	
	@Override
	public void onInstanceDestroy() {
		if (timerInstance != null) {
			timerInstance.cancel(false);
		}
		if (timerPrepare != null) {
			timerPrepare.cancel(false);
		}
		stopInstanceTask1();
		stopInstanceTask2();
		stopInstanceTask3();
		stopInstanceTask4();
		stopInstanceTask5();
		isInstanceDestroyed = true;
		instanceReward.clear();
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
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
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
	
	@Override
	public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INSTANT_DUNGEON_RESURRECT, 0, 0));
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player p) {
				if (p.getObjectId() == player.getObjectId()) {
					//You were killed during the Stonespear Seige. You will be moved to the waiting area.
					PacketSendUtility.sendPacket(p, new SM_SYSTEM_MESSAGE(1402910));
				} else {
					//"Player Name" has been killed and will be moved to the waiting area.
					PacketSendUtility.sendPacket(p, new SM_SYSTEM_MESSAGE(1402911, player.getName()));
				}
			}
		});
		return TeleportService2.teleportTo(player, mapId, instanceId, 196.80058f, 264.41388f, 97.461075f, (byte) 0);
	}
}
