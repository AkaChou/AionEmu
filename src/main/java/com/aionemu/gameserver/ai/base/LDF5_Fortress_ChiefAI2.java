package com.aionemu.gameserver.ai.base;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 据点/基地相关 NPC AI：LDF5 Fortress Chief（@AIName "ldf5_fortress_chief"），继承 AggressiveNpcAI2。
 * Base-related NPC AI: LDF5 Fortress Chief (@AIName "ldf5_fortress_chief"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("ldf5_fortress_chief")
public class LDF5_Fortress_ChiefAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleSpawned() {
        switch (getNpcId()) {
		    /**
	 * 天族 / Elyos
	 */
			case 251880: //Elyos Hero's Fall Defense Chief.
			    announceLDF5FortressLiCenter();
		    break;
			case 251881: //Elyos Ashen Glade Defense Chief.
			    announceLDF5FortressLiUp();
		    break;
			case 251882: //Elyos Smoldering Crag Defense Chief.
			    announceLDF5FortressLiDown();
		    break;
			/**
	 * 魔族 / Asmodians
	 */
			case 251960: //Asmodians Hero's Fall Defense Chief.
			    announceLDF5FortressDaCenter();
		    break;
			case 251961: //Asmodians Ashen Glade Defense Chief.
			    announceLDF5FortressDaUp();
		    break;
			case 251962: //Asmodians Smoldering Crag Defense Chief.
			    announceLDF5FortressDaDown();
		    break;
			/**
	 * 龙族 / Balaur
	 */
			case 252040: //Balaur Hero's Fall Defense Chief.
			    announceLDF5FortressDrCenter();
		    break;
			case 252041: //Balaur Ashen Glade Defense Chief.
			    announceLDF5FortressDrUp();
		    break;
			case 252042: //Balaur Smoldering Crag Defense Chief.
			    announceLDF5FortressDrDown();
		    break;
		}
		super.handleSpawned();
    }
	
	/**
	 * 天族系统消息 / Elyos system message
	 */
	private void announceLDF5FortressLiCenter() {
		// 天族占领了英雄陨落防御点。 / The Elyos have captured the Hero's Fall Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Li_Center);
	}
	private void announceLDF5FortressLiUp() {
		// 天族占领了灰林防御点。 / The Elyos have captured the Ashen Glade Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Li_Up);
	}
	private void announceLDF5FortressLiDown() {
		// 天族占领了闷燃峭壁防御点。 / The Elyos have captured the Smoldering Crag Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Li_Down);
	}
	
	/**
	 * 魔族系统消息 / Asmodian system message
	 */
	private void announceLDF5FortressDaCenter() {
		// 魔族占领了英雄陨落防御点。 / The Asmodians have captured the Hero's Fall Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Da_Center);
	}
	private void announceLDF5FortressDaUp() {
		// 魔族占领了灰林防御点。 / The Asmodians have captured the Ashen Glade Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Da_Up);
	}
	private void announceLDF5FortressDaDown() {
		// 魔族占领了闷燃峭壁防御点。 / The Asmodians have captured the Smoldering Crag Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Da_Down);
	}
	
	/**
	 * 龙族系统消息 / Balaur system message
	 */
	private void announceLDF5FortressDrCenter() {
		// 龙族占领了英雄陨落防御点。 / The Balaur have captured the Hero's Fall Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Dr_Center);
	}
	private void announceLDF5FortressDrUp() {
		// 龙族占领了灰林防御点。 / The Balaur have captured the Ashen Glade Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Dr_Up);
	}
	private void announceLDF5FortressDrDown() {
		// 龙族占领了闷燃峭壁防御点。 / The Balaur have captured the Smoldering Crag Defense Point.
		announce(SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Fortress_Dr_Down);
	}

	private void announce(SM_SYSTEM_MESSAGE message) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, message));
	}
}
