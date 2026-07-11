package com.aionemu.loginserver.network.gameserver.serverpackets;

import java.util.Map;

import com.aionemu.loginserver.model.base.BannedMacEntry;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import com.aionemu.loginserver.service.LoginProtectionServices;

/**
 * LS→GS：同步当前 MAC 封禁列表。
 * LS→GS: synchronize the current MAC ban list.
 *
 * @author KID
 */
public class SM_MACBAN_LIST extends GsServerPacket {

    /**
     * MAC 封禁条目映射。
     * Map of banned MAC entries.
     */
    private Map<String, BannedMacEntry> bannedList;

    /**
     * 构造 MAC 封禁列表同步包（从保护服务快照当前列表）。
     * Constructs a MAC ban list sync packet (snapshots the current list from protection services).
     */
    public SM_MACBAN_LIST() {
        this.bannedList = LoginProtectionServices.bannedMacManager().getMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(9);
        writeD(bannedList.size());

        for (BannedMacEntry entry : bannedList.values()) {
            writeS(entry.getMac());
            writeQ(entry.getTime().getTime());
            writeS(entry.getDetails());
        }
    }
}
