package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.model.BannedIP;
import java.sql.Timestamp;

public class LoginBannedIpService {

    public void start() {
        BannedIpController.start();
    }

    public boolean isBanned(String ip) {
        return BannedIpController.isBanned(ip);
    }

    public boolean banIp(String ip) {
        return BannedIpController.banIp(ip);
    }

    public boolean banIp(String ip, Timestamp expireTime) {
        return BannedIpController.banIp(ip, expireTime);
    }

    public boolean addOrUpdateBan(BannedIP ipBan) {
        return BannedIpController.addOrUpdateBan(ipBan);
    }

    public boolean unbanIp(String ip) {
        return BannedIpController.unbanIp(ip);
    }
}
