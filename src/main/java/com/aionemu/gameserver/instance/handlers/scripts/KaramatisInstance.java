package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 卡拉马提斯副本事件处理器。
 * Instance event handler for Karamatis.
 *
 * @author Encom
 */

@InstanceID(310010000)
public class KaramatisInstance extends GeneralInstanceHandler
{
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * @param player 玩家 / player
	 * @param zone 区域 / zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("AFIRA_OBELISK_310010000")) {
            belpartanBlessing();
	    }
    }
	
	private void belpartanBlessing() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(281); //Belpartan's Blessing.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
}