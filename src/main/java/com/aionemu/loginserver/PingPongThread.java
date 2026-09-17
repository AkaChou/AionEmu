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

    private final GsConnection connection;
    /**
     * 线程是否继续运行。
     * Whether the thread should keep running.
     */
    public volatile boolean uptime = true;
    private final SM_PING ping;
    private byte requests = 0;
    private int serverPID = -1;
    private final boolean killProcess = false;

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
                log.warn(I18n.get("log.28e4e10e61fd", e));
                Thread.currentThread().interrupt();
                return;
            }

            if (!uptime || validateResponse()) {
                return;
            }

            try {
                connection.sendPacket(ping);
                requests++;
                GameServerInfo info = this.connection.getGameServerInfo();
                if (info != null) {
                    updateSvStatsOnline(info.getId(), info.getCurrentPlayers(), info.getMaxPlayers());
                }
            } catch (Exception ex) {
                log.error(I18n.get("log.2d2a3f2b47fe", connection.getGameServerInfo().getId(), ex));
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

            GameServerInfo timedOutInfo = connection.getGameServerInfo();
            if (timedOutInfo != null) {
                updateSvStatsOffline(timedOutInfo.getId());
            }
            connection.close(false);
            if (killProcess && serverPID != -1) {
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
                    try {
                        new ProcessBuilder("taskkill", "/pid", String.valueOf(serverPID), "/f").start();
                    } catch (IOException e) {
                        log.error(I18n.get("log.8ab4345d6c71", serverPID, e));
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
        if (gameServerInfo != null) {
            updateSvStatsOffline(gameServerInfo.getId());
        }
    }

    /**
     * 上报游戏服在线人数（在线统计）。
     * Reports the game server's player counts (online).
     *
     * <p>DAO 注册表可能已经不存在：同一进程内 login/game/chat 共用一个 JVM，登录服的关闭流程会
     * {@code DAOManager.shutdown()} 清空注册表，而 game 服侧的断开清理可能在那之后才跑到这里。
     * 此时必须跳过（等价于"统计服务已下线"），否则会抛 {@code DAONotFoundException} 并让
     * {@code GsConnection.onDisconnect()} 的剩余清理中断。
     * The DAO registry may already be gone: login/game/chat share one JVM, the login shutdown calls
     * {@code DAOManager.shutdown()}, and the game-side disconnect cleanup can run afterwards. Skipping is
     * equivalent to "the stats service is down" and used to throw DAONotFoundException, which aborted the
     * rest of {@code GsConnection.onDisconnect()}.</p>
     *
     * @param serverId 游戏服 ID / game server id
     * @param currentPlayer 当前在线人数 / current players
     * @param currentMax 人数上限 / max players
     */
    static void updateSvStatsOnline(int serverId, int currentPlayer, int currentMax) {
        if (!SvStatsConfig.SVSTATS_ENABLE || !DAOManager.isInitialized()) {
            return;
        }
        DAOManager.getDAO(SvStatsDAO.class).update_SvStats_Online(serverId, 1, currentPlayer, currentMax);
    }

    /**
     * 上报游戏服离线状态（范式同 {@link #updateSvStatsOnline(int, int, int)}：DAO 注册表已清空时跳过）。
     * Reports the game server as offline, skipping when the DAO registry is gone.
     *
     * @param serverId 游戏服 ID / game server id
     */
    static void updateSvStatsOffline(int serverId) {
        if (!SvStatsConfig.SVSTATS_ENABLE || !DAOManager.isInitialized()) {
            return;
        }
        DAOManager.getDAO(SvStatsDAO.class).update_SvStats_Offline(serverId, 0, 0);
    }
}
