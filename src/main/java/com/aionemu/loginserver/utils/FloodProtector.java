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


package com.aionemu.loginserver.utils;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.network.aion.clientpackets.CM_LOGIN;
import com.aionemu.loginserver.service.LoginProtectionServices;

/**
 * @author Mr. Poke
 *
 */
public class FloodProtector {

    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(CM_LOGIN.class);
    private final Map<String, Long> flood = new ConcurrentHashMap<>();
    private final Map<String, Long> ban = new ConcurrentHashMap<>();

    @Deprecated(since = "boot-migration")
    public static final FloodProtector getInstance() {
        return SingletonHolder.instance;
    }

    @Deprecated
    public boolean addIp_nn(String ip) {
        Long time = flood.get(ip);
        if (time == null || System.currentTimeMillis() - time > Config.FAST_RECONNECTION_TIME) {
            flood.put(ip, System.currentTimeMillis());
            return false;
        }
        Timestamp newTime = new Timestamp(System.currentTimeMillis() + Config.WRONG_LOGIN_BAN_TIME * 60000);
        if (!LoginProtectionServices.bannedIpService().isBanned(ip)) {
            log.info("[AUDIT]FloodProtector:" + ip + " IP banned for " + Config.WRONG_LOGIN_BAN_TIME + " min");
            return LoginProtectionServices.bannedIpService().banIp(ip, newTime);
        }
        //in this case this ip is already banned

        return true;
    }

    public boolean tooFast(String ip) {
        String[] exclIps = Config.EXCLUDED_IP.split(",");
        for (String exclIp : exclIps) {
            if (ip.equals(exclIp)) {
                return false;
            }
        }
        Long banned = ban.get(ip);
        if (banned != null) {
            if (System.currentTimeMillis() < banned) {
                return true;
            } else {
                ban.remove(ip);
                return false;
            }
        }
        Long time = flood.get(ip);
        if (time == null) {
            flood.put(ip, System.currentTimeMillis() + Config.FAST_RECONNECTION_TIME * 1000);
            return false;
        } else {
            if (time > System.currentTimeMillis()) {
                log.info("[AUDIT]FloodProtector:" + ip + " IP too fast connection attemp. blocked for " + Config.WRONG_LOGIN_BAN_TIME + " min");
                ban.put(ip, System.currentTimeMillis() + Config.WRONG_LOGIN_BAN_TIME * 60000);
                return true;
            } else {
                return false;
            }
        }
    }

    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {

        protected static final FloodProtector instance = new FloodProtector();
    }
}
