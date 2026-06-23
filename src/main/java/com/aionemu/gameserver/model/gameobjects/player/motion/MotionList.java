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
package com.aionemu.gameserver.model.gameobjects.player.motion;

import java.util.Collections;
import java.util.Map;
import java.util.List;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.MotionDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @Rework: MATTY
 */

public class MotionList {
    private static final Logger log = LoggerFactory.getLogger(MotionList.class); // Добавьте это
    private Player owner;
    private Map<Integer, Motion> activeMotions;
    private Map<Integer, Motion> motions;

    public MotionList(Player owner) {
        this.owner = owner;
    }

    public Map<Integer, Motion> getActiveMotions() {
        if (activeMotions == null) {
            return Collections.emptyMap();
        }
        return activeMotions;
    }

    public Map<Integer, Motion> getMotions() {
        if (motions == null) {
            return Collections.emptyMap();
        }
        return motions;
    }

    public void add(Motion motion, boolean persist) {
        if (motions == null) {
            motions = new FastMap<Integer, Motion>();
        }
        if (motions.containsKey(motion.getId()) && motion.getExpireTime() == 0) {
            remove(motion.getId());
        }
        motions.put(motion.getId(), motion);
        if (motion.isActive()) {
            if (activeMotions == null) {
                activeMotions = new FastMap<Integer, Motion>();
            }
            Motion old = activeMotions.put(Motion.motionType.get(motion.getId()), motion);
            if (old != null) {
                old.setActive(false);
                DAOManager.getDAO(MotionDAO.class).updateMotion(owner.getObjectId(), old);
            }
        }
        if (persist) {
            if (motion.getExpireTime() != 0) {
                ExpireTimerTask.getInstance().addTask(motion, owner);
            }
            DAOManager.getDAO(MotionDAO.class).storeMotion(owner.getObjectId(), motion);
        }
    }

    public boolean remove(int motionId) {
        Motion motion = motions.remove(motionId);
        if (motion != null) {
            PacketSendUtility.sendPacket(owner, new SM_MOTION((short) motionId));
            DAOManager.getDAO(MotionDAO.class).deleteMotion(owner.getObjectId(), motionId);
            if (motion.isActive()) {
                activeMotions.remove(Motion.motionType.get(motionId));
                return true;
            }
        }
        return false;
    }

    public void setActive(int motionId, int motionType) {
        if (motionId != 0) {
            Motion motion = motions.get(motionId);
            if (motion == null || motion.isActive()) {
                return;
            }
            if (activeMotions == null) {
                activeMotions = new FastMap<Integer, Motion>();
            }
            Motion old = activeMotions.put(motionType, motion);
            if (old != null) {
                old.setActive(false);
                DAOManager.getDAO(MotionDAO.class).updateMotion(owner.getObjectId(), old);
            }
            motion.setActive(true);
            DAOManager.getDAO(MotionDAO.class).updateMotion(owner.getObjectId(), motion);
        } else if (activeMotions != null) {
            Motion old = activeMotions.remove(motionType);
            if (old == null) {
                return;
            }
            old.setActive(false);
            DAOManager.getDAO(MotionDAO.class).updateMotion(owner.getObjectId(), old);
        }
        PacketSendUtility.sendPacket(owner, new SM_MOTION((short) motionId, (byte) motionType));
        PacketSendUtility.broadcastPacket(owner, new SM_MOTION(owner.getObjectId(), activeMotions), true);
    }

    /**
     *  Проверяет, есть ли у игрока анимация с заданным ID.
     *  @param motionId ID анимации для проверки.
     *  @return true, если анимация существует, false в противном случае.
     */
    public boolean hasMotion(Integer motionId) {
       if (motions == null) {
           // log.warn("hasMotion: motions == null, возвращаем false");
           return false;
       }
       // log.warn("hasMotion: Проверяем наличие motionId: {}", motionId);
       boolean containsKey = motions.containsKey(motionId);
       // log.warn("hasMotion: motions.containsKey({}) вернул: {}", motionId, containsKey);
       return containsKey;
    }

    public void loadMotionsFromDatabase() {
        List<Motion> loadedMotions = DAOManager.getDAO(MotionDAO.class).loadMotions(owner.getObjectId());
        if (loadedMotions != null) {
            for (Motion motion : loadedMotions) {
                add(motion, false); // Не сохраняем повторно, просто загружаем
            }
        }
    }
}