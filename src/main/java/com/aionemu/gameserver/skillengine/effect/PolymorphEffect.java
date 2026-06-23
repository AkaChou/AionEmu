/*
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
package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlAttribute; 

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolymorphEffect")
public class PolymorphEffect extends TransformEffect {

    @XmlAttribute(name = "neutral_to_npc")
    private boolean neutralToNpc = false;

	@Override
	public void startEffect(Effect effect) {
        if (neutralToNpc && effect.getEffected() instanceof Player) {
            ((Player) effect.getEffected()).setAdminNeutral(1);
        }
		if ((effect.getEffector() instanceof Player)) {
			if (effect.getEffector().getEffectController().isAbnormalSet(AbnormalState.HIDE)) {
				effect.getEffector().getEffectController().removeHideEffects();
			}
		}
		super.startEffect(effect, AbnormalState.NOFLY);
	}

	@Override
	public void endEffect(Effect effect) {
        if (neutralToNpc && effect.getEffected() instanceof Player) {
            ((Player) effect.getEffected()).setAdminNeutral(0);
        }
		super.endEffect(effect, AbnormalState.NOFLY);
		effect.getEffected().getTransformModel().setActive(false);
	}
}