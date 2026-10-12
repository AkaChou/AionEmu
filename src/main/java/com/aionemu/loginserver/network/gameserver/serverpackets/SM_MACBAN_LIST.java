package com.aionemu.loginserver.network.gameserver.serverpackets;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.model.base.BannedMacEntry;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：同步当前 MAC 封禁列表。
 * LS→GS: synchronize the current MAC ban list.
 * @author KID
 */
@RequiredArgsConstructor
public class SM_MACBAN_LIST extends GsServerPacket {

    private final BannedMacManager bannedMacManager;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        Map<String, BannedMacEntry> bannedList = bannedMacManager.getMap();

        writeC(9);
        writeD(bannedList.size());

        for (BannedMacEntry entry : bannedList.values()) {
            writeS(entry.getMac());
            writeQ(entry.getTime().getTime());
            writeS(entry.getDetails());
        }
    }
}
