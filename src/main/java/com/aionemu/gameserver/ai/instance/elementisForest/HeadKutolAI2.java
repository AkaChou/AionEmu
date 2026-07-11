package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * Elementis Forest 副本 NPC AI：Head Kutol（@AIName "kutol"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Head Kutol (@AIName "kutol"), extends AggressiveNpcAI2.
 *
 * @author Romanz
 */
@AIName("kutol")
public class HeadKutolAI2 extends AggressiveNpcAI2 {

    @Override
    protected void handleAttack(Creature creature) {
        super.handleAttack(creature);

        if (Rnd.get(1, 100) < 1) {
            spawnClone();
        }
    }

    private void spawnClone() {
        Npc KutolClone = getPosition().getWorldMapInstance().getNpc(282302);
        int random = Rnd.get(1, 3);
        if (KutolClone == null) {
            switch (random) {
                case 1:
                    spawn(282302, getOwner().getX(), getOwner().getY(), getOwner().getZ() + 2, (byte) 3);
                    break;
                case 2:
                    spawn(282302, getOwner().getX(), getOwner().getY(), getOwner().getZ() + 2, (byte) 3);
                    spawn(282302, getOwner().getX() - 5, getOwner().getY() - 3, getOwner().getZ() + 2, (byte) 3);
                    break;
                default:
                    spawn(282302, getOwner().getX(), getOwner().getY(), getOwner().getZ() + 2, (byte) 3);
                    spawn(282302, getOwner().getX() - 5, getOwner().getY() - 3, getOwner().getZ() + 2, (byte) 3);
                    spawn(282302, getOwner().getX() + 5, getOwner().getY() - 3, getOwner().getZ() + 2, (byte) 3);
                    break;
            }
        }
    }
}
