package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * @author
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TemperingProtectionEffect")
public class TemperingProtectionEffect extends BuffEffect {
    
    @Override
    public void calculate(Effect effect) {
        effect.addSucessEffect(this);
    }
    
    @Override
    public void applyEffect(Effect effect) {
        effect.addToEffectedController();
    }
}