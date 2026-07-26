package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * 被占领的伦图斯基地副本事件处理器。
 * Instance event handler for Occupied Rentus Base.
 *
 * @author Encom
 */

@InstanceID(300620000)
public class OccupiedRentusBaseInstance extends GeneralInstanceHandler {
	/** 已播放动画集合 / played-movie set */
	private final List<Integer> movies = new ArrayList<>();
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		setDoorState(54, true);
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
					setDoorState(145, true);
				}
				break;
			case 236302: //Archmagus Upadi.
				setDoorState(70, true);
				break;
			}
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
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
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
