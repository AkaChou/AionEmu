/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.loginserver.network.gameserver.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.loginserver.controller.AccountController;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.ReconnectingAccount;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_ACCOUNT_RECONNECT_KEY;

/**
 * This packet is sended by GameServer when player is requesting fast reconnect
 * to login server. LoginServer in response will send reconectKey.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class CM_ACCOUNT_RECONNECT_KEY extends GsClientPacket {

    /**
     * accoundId of account that will be reconnecting.
     */
    private int accountId;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readImpl() {
        accountId = readD();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void runImpl() {
        int reconectKey = Rnd.nextInt();
        Account acc = this.getConnection().getGameServerInfo().removeAccountFromGameServer(accountId);
        if (acc == null) {
            log.info("This shouldnt happend! [Error]");
        } else {
            AccountController.addReconnectingAccount(new ReconnectingAccount(acc, reconectKey));
        }
        sendPacket(new SM_ACCOUNT_RECONNECT_KEY(accountId, reconectKey));
    }
}
