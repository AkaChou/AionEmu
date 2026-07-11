package com.aionemu.gameserver.ai.instance.IDEvent_Def;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.*;
import com.aionemu.gameserver.world.*;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * ID Event Def 副本 NPC AI：IDEVENT Def Mob（@AIName "IDEVENT_Def_Mob"），继承 AggressiveNpcAI2。
 * ID Event Def instance NPC AI: IDEVENT Def Mob (@AIName "IDEVENT_Def_Mob"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("IDEVENT_Def_Mob")
public class IDEVENT_Def_MobAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			sendWarPoints();
		}
		super.handleDied();
	}
	
	private void sendWarPoints() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 100)) {
					ItemService.addItem(player, 186000470, 1); //战争点数。 / War Points.
				}
			}
		});
	}
}
