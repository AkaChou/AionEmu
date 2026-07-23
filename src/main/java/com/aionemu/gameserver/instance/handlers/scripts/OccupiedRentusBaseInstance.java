package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 被占领的伦图斯基地副本事件处理器。
 * Instance event handler for Occupied Rentus Base.
 *
 * @author Encom
 */

@InstanceID(300620000)
public class OccupiedRentusBaseInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
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
            spawnOccupiedDirectFiringGunIDYun();
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
		doors = instance.getDoors();
		doors.get(54).setOpen(true);
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(final Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 236300: //Brigade General Vasharti.
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 184.69116f, 414.00864f, 260.75488f, (byte) 59); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 184.69116f, 414.00864f, 260.75488f, (byte) 59); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				boostMorale();
				sendMovie(player, 481);
				reianOccupiedSecureBridge();
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Occupied Rentus Base>");
				spawn(730520, 193.6f, 436.5f, 262f, (byte) 86); //Occupied Rentus Base Exit.
			break;
			case 236299: //Umatha The Crazed.
			case 236301: //Ambusher Kiriana.
			    Npc yunNmdPortalFi65Ae = instance.getNpc(236299);
			    Npc yunNmdPortalClew65Ae = instance.getNpc(236301);
			    if (isDead(yunNmdPortalFi65Ae) && isDead(yunNmdPortalClew65Ae)) {
				    deleteNpc(701156);
					doors.get(145).setOpen(true);
			    }
			break;
			case 236302: //Archmagus Upadi.
			    doors.get(70).setOpen(true);
			break;
			case 282394: //Oil Cask.
				despawnNpc(npc);
				spawn(282395, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); //Spilled Oil.
			break;
			case 283000: //Kiss Of Fire.
			case 283001: //Kiss Of Ice.
				despawnNpc(npc);
			break;
		}
	}
	
	private void spawnOccupiedDirectFiringGunIDYun() {
        final int DirectFiringGunIDYun1 = spawnRace == Race.ASMODIANS ? 702677 : 702683;
        final int DirectFiringGunIDYun2 = spawnRace == Race.ASMODIANS ? 702678 : 702684;
        final int DirectFiringGunIDYun3 = spawnRace == Race.ASMODIANS ? 702679 : 702685;
        final int DirectFiringGunIDYun4 = spawnRace == Race.ASMODIANS ? 702680 : 702686;
        final int DirectFiringGunIDYun5 = spawnRace == Race.ASMODIANS ? 702681 : 702687;
        final int DirectFiringGunIDYun6 = spawnRace == Race.ASMODIANS ? 702682 : 702688;
        spawn(DirectFiringGunIDYun1, 451.17743f, 463.36676f, 151.7269f, (byte) 0, 188);
        spawn(DirectFiringGunIDYun2, 447.17374f, 473.42770f, 151.7212f, (byte) 0, 184);
        spawn(DirectFiringGunIDYun3, 443.77615f, 482.98611f, 151.7285f, (byte) 0, 183);
        spawn(DirectFiringGunIDYun4, 423.54575f, 442.81320f, 149.8756f, (byte) 0, 199);
        spawn(DirectFiringGunIDYun5, 417.98672f, 450.97131f, 149.8397f, (byte) 0, 202);
        spawn(DirectFiringGunIDYun6, 412.86597f, 459.53128f, 149.8614f, (byte) 0, 204);
    }
	
	private void reianOccupiedSecureBridge() {
		Npc ariana5 = (Npc) spawn(799670, 183.736f, 391.392f, 260.571f, (byte) 26); //Ariana.
		GameFeatureServices.npcShoutsService().sendMsg(ariana5, 1500417, ariana5.getObjectId(), 0, 5000);
		GameFeatureServices.npcShoutsService().sendMsg(ariana5, 1500418, ariana5.getObjectId(), 0, 8000);
		GameFeatureServices.npcShoutsService().sendMsg(ariana5, 1500419, ariana5.getObjectId(), 0, 11000);
		spawn(800227, 192.56216f, 421.5615f, 260.5717f, (byte) 0); //Reian Warrior.
		spawn(800227, 189.40356f, 423.41653f, 260.57162f, (byte) 0); //Reian Warrior.
		spawn(800228, 195.74078f, 422.42538f, 260.57162f, (byte) 0); //Reian Priest.
		spawn(800228, 188.83278f, 425.67007f, 260.57153f, (byte) 0); //Reian Priest.
		spawn(800229, 194.72948f, 424.4182f, 260.5716f, (byte) 0); //Imprisoned Reian.
		spawn(800229, 190.90623f, 425.9276f, 260.5716f, (byte) 0); //Imprisoned Reian.
		spawn(800230, 193.46213f, 426.45123f, 260.57156f, (byte) 0); //Imprisoned Reian.
		spawn(833048, 188.27031f, 414.384f, 260.75488f, (byte) 83); //Rentus Quality Supplies Storage Box.
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
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
			case 702677: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702678: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702679: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702680: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702681: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702682: //伦图斯攻城武器。 / Rentus Siege Weapon.
			    despawnNpc(npc);
			    GameEngineServices.skillEngine().getSkill(npc, 21806, 60, player).useNoAnimationSkill(); //Mount Anti-Aircraft Gun.
			break;
			case 702683: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702684: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702685: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702686: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702687: //伦图斯攻城武器。 / Rentus Siege Weapon.
			case 702688: //伦图斯攻城武器。 / Rentus Siege Weapon.
			    despawnNpc(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21805, 60, player).useNoAnimationSkill(); //Mount Anti-Aircraft Gun.
			break;
			case 701151: //Reian Combat Ration.
			case 701152: //Reian Emergency Rations.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 5000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 5000);
			break;
			case 701097: //Collapsed Stone Wall.
				despawnNpc(npc);
			break;
			case 701100: //Old Incense Burner.
				if (instance.getNpc(799543) == null) {
					spawn(799543, 506.303f, 613.902f, 158.179f, (byte) 0); //Paudav.
				}
			break;
		}
	}
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
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
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void despawnNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			npc.getController().onDelete();
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private boolean isDead(Npc npc) {
		return (npc == null || npc.getLifeStats().isAlreadyDead());
	}
	
	private void boostMorale() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(19367); //Boost Morale.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
}
