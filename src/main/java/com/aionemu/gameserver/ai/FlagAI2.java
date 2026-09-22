package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_UPDATE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.Future;

/**
 * 旗帜 AI：战场/据点旗帜交互逻辑。
 * Flag AI for battlefield or base flag interaction.
 */
@AIName("flag")
public class FlagAI2 extends NoActionAI2
{
    private Future<?> sendPacketTask;

    /**
     * 处理生成完成事件。
     * Handle post-spawn.
     */
    @Override
    public void handleSpawned() {
        super.handleSpawned();
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> sendPacketTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			if (player.getWorldId() == getOwner().getWorldId()) {
				if (getOwner().isSpawned()) {
					PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, getOwner()));
				}
			}
		}, 1000, 2000));
    }

    /**
     * 处理消失事件。
     * Handle despawn.
     */
    @Override
    protected void handleDespawned() {
        super.handleDespawned();
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> sendPacketTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			if (player.getWorldId() == getOwner().getWorldId()) {
				PacketSendUtility.sendPacket(player, new SM_FLAG_UPDATE(getOwner()));
				AI2Actions.deleteOwner(FlagAI2.this);
			}
		}, 1000, 2000));
    }

}
