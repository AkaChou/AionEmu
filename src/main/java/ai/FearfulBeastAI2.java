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
package ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.ai2.handler.ReturningEventHandler;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;

/****/
/** Author (Encom)
/****/

@AIName("fearful_beast")
public class FearfulBeastAI2 extends GeneralNpcAI2
{
    private static final float TRIGGER_RANGE = 6.0f;
    private static final float FLEE_DISTANCE = 9.0f;
    private static final int FLEE_DURATION = 5000;
    private static final int COOLDOWN_MS = 10000;
    
    private long lastFearTime = 0;
    private boolean isFeared = false;
    
    @Override
    protected void handleCreatureSee(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
        checkAndTriggerFear(creature);
    }
    
    @Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureMoved(this, creature);
        checkAndTriggerFear(creature);
    }
    
    private void checkAndTriggerFear(Creature creature) {
        if (!(creature instanceof Player)) {
            return;
        }
        if (isFeared) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastFearTime < COOLDOWN_MS) {
            return;
        }
        
        Npc npc = getOwner();
        if (npc == null) {
            return;
        }
        if (!MathUtil.isIn3dRange(npc, creature, TRIGGER_RANGE)) {
            return;
        }
        
        lastFearTime = now;
        doFlee(creature);
    }
    
    private void doFlee(Creature player) {
        Npc npc = getOwner();
        if (npc == null) {
            return;
        }
        
        isFeared = true;
        setStateIfNot(AIState.FEAR);
        
        npc.unsetState(CreatureState.WALKING);
        npc.setState(CreatureState.WEAPON_EQUIPPED);
        PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, 0));
        PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.NEUTRALMODE, 0, 0));
        
        float x = npc.getX();
        float y = npc.getY();
        float z = npc.getZ();
        
        byte moveAwayHeading = PositionUtil.getMoveAwayHeading(player, npc);
        int randomOffset = Rnd.get(-45, 45);
        double radian = Math.toRadians(MathUtil.convertHeadingToDegree(moveAwayHeading) + randomOffset);
        
        float targetX = (float) (x + Math.cos(radian) * FLEE_DISTANCE);
        float targetY = (float) (y + Math.sin(radian) * FLEE_DISTANCE);
        
        float[][] path = NavService.getInstance().navigateToLocation(npc, targetX, targetY, z);
        
        if (path != null && path.length > 0) {
            npc.getMoveController().resetMove();
            npc.getMoveController().moveToPoint(path[0][0], path[0][1], path[0][2]);
        } else {
            byte intentions = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId());
            Vector3f closestCollision = GeoService.getInstance().getClosestCollision(npc, targetX, targetY, z, true, intentions);
            npc.getMoveController().resetMove();
            npc.getMoveController().moveToPoint(closestCollision.getX(), closestCollision.getY(), closestCollision.getZ());
        }
        
        ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                onFleeEnd();
            }
        }, FLEE_DURATION);
    }
    
    private void onFleeEnd() {
        isFeared = false;
        Npc npc = getOwner();
        if (npc == null || npc.getLifeStats().isAlreadyDead()) {
            return;
        }
        
        npc.getMoveController().abortMove();
        
        npc.unsetState(CreatureState.WEAPON_EQUIPPED);
        npc.setState(CreatureState.WALKING);
        
        if (!npc.isAtSpawnLocation()) {
            ReturningEventHandler.onNotAtHome(this);
        }
    }
}
