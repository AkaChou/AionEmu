package com.aionemu.loginserver;

import java.io.IOException;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.dao.SvStatsDAO;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.serverpackets.SM_PING;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服心跳检测线程（ping/pong）。
 * GameServer heartbeat thread (ping/pong).
 *
 * @author KID
 */
@Slf4j
public class PingPongThread implements Runnable {

    private GsConnection connection;
    /**
     * 线程是否继续运行。
     * Whether the thread should keep running.
     */
    public volatile boolean uptime = true;
    private SM_PING ping;
    private byte requests = 0;
    private int serverPID = -1;
    private boolean killProcess = false;

    /**
     * 为指定游戏服连接创建心跳线程。
     * Create a heartbeat thread for the given GameServer connection.
     *
     * @param connection 游戏服连接 / GameServer connection
     */
    public PingPongThread(GsConnection connection) {
        this.uptime = true;
        this.connection = connection;
        this.ping = new SM_PING();
    }

    /**
     * 周期发送 ping，并在超时未响应时断开连接。
     * Periodically send ping and close the connection when responses time out.
     */
    @Override
    public void run() {
        log.info(I18n.get("log.edfb7eb94901", this.connection.getGameServerInfo().getId()));
        while (uptime) {
            try {
                Thread.sleep(Config.PINGPONG_DELAY);
            } catch (InterruptedException e) {
                log.warn(I18n.get("log.28e4e10e61fd", e), e);
                Thread.currentThread().interrupt();
                return;
            }

            if (!uptime || validateResponse()) {
                return;
            }

            try {
                connection.sendPacket(ping);
                requests++;
                if (SvStatsConfig.SVSTATS_ENABLE) {
                    int currentID = this.connection.getGameServerInfo().getId();
                    int currentPlayer = this.connection.getGameServerInfo().getCurrentPlayers();
                    int currentMax = this.connection.getGameServerInfo().getMaxPlayers();
                    DAOManager.getDAO(SvStatsDAO.class).update_SvStats_Online(currentID, 1, currentPlayer, currentMax);
                }
            } catch (Exception ex) {
                log.error(I18n.get("log.2d2a3f2b47fe", connection.getGameServerInfo().getId(), ex), ex);
            }
        }
    }

    /**
     * 处理 pong 响应。
     * Handle a pong response.
     *
     * @param pid 游戏服进程 ID / GameServer process id
     */
    public void onResponse(int pid) {
        requests--;
        this.serverPID = pid;
    }

    /**
     * 校验未响应次数；超限则关闭连接（可选杀进程）。
     * Validate outstanding requests; close connection when exceeded (optionally kill process).
     *
     * @return 已判定超时关闭则为 true / true if timed out and closed
     */
    public boolean validateResponse() {
        if (requests >= 2) {
            uptime = false;
            log.info(I18n.get("log.3054e06f2cb1", connection.getGameServerInfo().getId(), this.serverPID));

            if (SvStatsConfig.SVSTATS_ENABLE) {
                int currentID = connection.getGameServerInfo().getId();
                DAOManager.getDAO(SvStatsDAO.class).update_SvStats_Offline(currentID, 0, 0);
            }
            connection.close(false);
            if (killProcess && serverPID != -1) {
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
                    try {
                        new ProcessBuilder("taskkill", "/pid", String.valueOf(serverPID), "/f").start();
                    } catch (IOException e) {
                        log.error(I18n.get("log.8ab4345d6c71", serverPID, e), e);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 停止心跳并更新 SvStats 离线状态。
     * Stop heartbeat and mark SvStats offline.
     */
    public void closeMe() {
        uptime = false;

        GameServerInfo gameServerInfo = connection.getGameServerInfo();
        if (SvStatsConfig.SVSTATS_ENABLE && gameServerInfo != null) {
            int currentID = gameServerInfo.getId();
            DAOManager.getDAO(SvStatsDAO.class).update_SvStats_Offline(currentID, 0, 0);
        }
    }
}
