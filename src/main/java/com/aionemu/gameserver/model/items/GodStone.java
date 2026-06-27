/*
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
package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.GodstoneInfo;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;

public class GodStone extends ItemStone {
    private static final Logger log = LoggerFactory.getLogger(GodStone.class);
    
    // 字段声明分组
    private final GodstoneInfo godstoneInfo;
    private final ItemTemplate godItem;
    private final int probability;
    private final int probabilityLeft;
    
    private ActionObserver actionListener;
    private boolean breakProc;
    private int triggerCounter = 0;    
    private volatile boolean isProcessingAttack = false;

    public GodStone(int itemObjId, int itemId, PersistentState persistentState) {
        super(itemObjId, itemId, 0, persistentState);
        ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
        this.godItem = itemTemplate;
        this.godstoneInfo = itemTemplate.getGodstoneInfo();

        // 参数校验前置
        validateGodstoneInfo();
        
        // 初始化概率值
        if (godstoneInfo != null) {
            this.probability = godstoneInfo.getProbability();
            this.probabilityLeft = godstoneInfo.getProbabilityleft();
        } else {
            this.probability = 0;
            this.probabilityLeft = 0;
            log.warn("Godstone info missing for item: {}", itemId);
        }
    }

    private void validateGodstoneInfo() {
        if (godstoneInfo != null) {
            // 双手武器只需要检查主手概率
            if (godItem.isTwoHandWeapon()) {
                if (godstoneInfo.getProbability() < 0 || godstoneInfo.getProbability() > 1000) {
                    throw new IllegalArgumentException("双手武器概率值必须在0-1000之间");
                }
            } 
            // 非双手武器检查主副手概率
            else {
                if (godstoneInfo.getProbability() < 0 || godstoneInfo.getProbability() > 1000 || 
                    godstoneInfo.getProbabilityleft() < 0 || godstoneInfo.getProbabilityleft() > 1000) {
                    throw new IllegalArgumentException("概率值必须在0-1000之间");
                }
            }
            
            if (godstoneInfo.getBreakprob() > 1000) {
                throw new IllegalArgumentException("损坏概率值不能超过1000");
            }
        }
    }

    public void onEquip(final Player player) {
        clearPreviousListener(player);
        
        if (!validateEquipConditions(player)) {
            return;
        }
        
        final Item equippedItem = getEquippedItem(player);
        final int handProbability = calculateHandProbability(equippedItem);
        final float breakChance = calculateBreakChance();
        
        setupAttackListener(player, equippedItem, handProbability, breakChance);
    }

    private boolean validateEquipConditions(Player player) {
        return godstoneInfo != null && godItem != null && getEquippedItem(player) != null;
    }

    private Item getEquippedItem(Player player) {
        return player.getEquipment().getEquippedItemByObjId(getItemObjId());
    }

    private int calculateHandProbability(Item equippedItem) {
        // 双手武器总是使用主手概率
        if (equippedItem.getItemTemplate().isTwoHandWeapon()) {
            return probability;
        }
        boolean isMainHand = equippedItem.getEquipmentSlot() == ItemSlot.MAIN_HAND.getSlotIdMask();
        return isMainHand ? probability : probabilityLeft;
    }

    private float calculateBreakChance() {
        return Math.min(godstoneInfo.getBreakprob() / 1000f, 0.9f) / Math.max(CustomConfig.ILLUSION_GODSTONE_BREAK_RATE, 0.1f);
    }

    private void setupAttackListener(Player player, Item equippedItem, int handProbability, float breakChance) {
        actionListener = new ActionObserver(ObserverType.ATTACK) {
            @Override
            public void attack(Creature creature) {
                handleAttack(player, creature, equippedItem, handProbability, breakChance);
            }
        };
        player.getObserveController().addObserver(actionListener);
    }
    
    private void handleAttack(Player player, Creature creature, Item equippedItem, int handProbability, float breakChance) {
        // 所有武器类型都使用同步锁防止重复触发
        synchronized (this) {
            if (isProcessingAttack) {
                return;
            }
            isProcessingAttack = true;
        }
        
        try {
            boolean shouldTrigger = checkTriggerCondition(handProbability);
            boolean shouldBreak = checkBreakCondition(breakChance);
    
            if (shouldTrigger) {
                // 创建技能并检查是否可以使用（包括距离检查）
                Skill skill = createSkill(player, creature);
                
                // 只有当技能可以使用时才发送提示和应用效果
                if (skill.canUseSkill()) {
                    // 应用技能效果
                    Effect effect = new Effect(player, creature, skill.getSkillTemplate(), 1, 0, godItem
                    );
                    effect.initialize();
                    effect.applyEffect();
                }
            }
    
            if (shouldBreak) {
                handleItemBreak(player, equippedItem);
            }
        } finally {
            synchronized (this) {
                isProcessingAttack = false;
            }
        }
    }

    private void triggerSkillEffect(Player player, Creature creature) {
        // 此方法不再使用
    }

    private boolean checkTriggerCondition(int handProbability) {
        return Rnd.get(0, 1000) < handProbability;
    }

    private boolean checkBreakCondition(float breakChance) {
        return godstoneInfo.getBreakable() && !breakProc && Rnd.get(1000) < (int)(breakChance * 1000);
    }

    private Skill createSkill(Player player, Creature creature) {
        return SkillEngine.getInstance().getSkill(player, godstoneInfo.getSkillid(), godstoneInfo.getSkilllvl(), player.getTarget(), godItem);
    }

    private void applySkillEffect(Player player, Creature creature, Skill skill) {
        Effect effect = new Effect(
            player, creature, skill.getSkillTemplate(), 1, 0, godItem
        );
        effect.initialize();
        effect.applyEffect();
    }

    private void handleItemBreak(Player player, Item equippedItem) {
        breakProc = true;
        notifyBreakMessages(player, equippedItem);
        scheduleItemRemoval(player, equippedItem);
    }

    private void notifyBreakMessages(Player player, Item equippedItem) {
        // 立即发送损坏消息
        PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402536, new DescriptionId(equippedItem.getNameId()), new DescriptionId(godItem.getNameId())));
        
        // 定时消息提醒（保持原有时间间隔）
        sendScheduledMessages(player, equippedItem);
    }

    private void sendScheduledMessages(Player player, Item equippedItem) {
        // 10分钟后提醒
        PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402237, new DescriptionId(equippedItem.getNameId()), new DescriptionId(godItem.getNameId())), 600000);
        
        // 5分钟后提醒
        PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402537, new DescriptionId(equippedItem.getNameId()), new DescriptionId(godItem.getNameId()), 5), 300000);
        
        // 60秒前提醒
        PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402538, new DescriptionId(equippedItem.getNameId()), new DescriptionId(godItem.getNameId()), 60), 540000);
    }

    private void scheduleItemRemoval(Player player, Item equippedItem) {
        GameThreadPoolServices.threadPoolManager().schedule(() -> {
            onUnEquip(player);
            equippedItem.setGodStone(null);
            setPersistentState(PersistentState.DELETED);
            ItemPacketService.updateItemAfterInfoChange(player, equippedItem);
            DAOManager.getDAO(InventoryDAO.class).store(equippedItem, player);
            PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, equippedItem));
        }, 600000); // 10分钟后执行
    }

    public void onUnEquip(Player player) {
        clearPreviousListener(player);
    }

    private void clearPreviousListener(Player player) {
        if (actionListener != null) {
            player.getObserveController().removeObserver(actionListener);
            actionListener = null;
        }
    }
}