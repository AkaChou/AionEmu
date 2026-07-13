package com.aionemu.loginserver.network.sts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.service.VipService;

import lombok.extern.slf4j.Slf4j;

/**
 * Tiny STS endpoint that answers China-client /Level/GetLevel with account VIP score.
 *
 * This is intentionally separate from the Aion game protocol listeners.
 */
@Slf4j
public final class StsVipServer {

    private static final Object LOCK = new Object();
    private static StsVipServer instance;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService acceptPool;
    private ExecutorService clientPool;
    private VipService vipService;

    private StsVipServer() {
    }

    public static void startIfEnabled() {
        if (!VipConfig.STS_ENABLE) {
            return;
        }
        VipConfig.validate();
        synchronized (LOCK) {
            if (instance != null && instance.running.get()) {
                return;
            }
            StsVipServer server = new StsVipServer();
            server.start();
            instance = server;
        }
    }

    public static void shutdownIfStarted() {
        StsVipServer server;
        synchronized (LOCK) {
            server = instance;
            instance = null;
        }
        if (server != null) {
            server.stop();
        }
    }

    private void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            InetAddress bindAddress = resolveBindAddress(VipConfig.STS_HOST);
            serverSocket = new ServerSocket(VipConfig.STS_PORT, 50, bindAddress);
            serverSocket.setReuseAddress(true);
            vipService = new VipService();
            acceptPool = Executors.newSingleThreadExecutor(namedFactory("sts-vip-accept"));
            clientPool = Executors.newCachedThreadPool(namedFactory("sts-vip-client"));
            acceptPool.execute(this::acceptLoop);
            log.info("STS VIP server listening on {}:{}",
                bindAddress == null ? "*" : bindAddress.getHostAddress(),
                VipConfig.STS_PORT);
        } catch (Exception e) {
            running.set(false);
            closeQuietly();
            throw new IllegalStateException("Failed to start STS VIP server on port " + VipConfig.STS_PORT, e);
        }
    }

    private void stop() {
        running.set(false);
        closeQuietly();
        if (acceptPool != null) {
            acceptPool.shutdownNow();
        }
        if (clientPool != null) {
            clientPool.shutdownNow();
        }
        log.info("STS VIP server stopped");
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                clientPool.execute(() -> handleClient(socket));
            } catch (SocketException closed) {
                if (running.get()) {
                    log.warn("STS VIP accept socket error", closed);
                }
                break;
            } catch (Throwable t) {
                if (running.get()) {
                    log.warn("STS VIP accept failed", t);
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        String peer = socket.getRemoteSocketAddress() == null
            ? "unknown"
            : socket.getRemoteSocketAddress().toString();
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(30_000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            StringBuilder buffer = new StringBuilder(1024);
            byte[] chunk = new byte[4096];
            while (running.get()) {
                int read = in.read(chunk);
                if (read < 0) {
                    break;
                }
                buffer.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                while (true) {
                    StsVipProtocol.ParsedRequest request = StsVipProtocol.tryParse(buffer.toString());
                    if (request == null) {
                        break;
                    }
                    buffer.setLength(0);
                    if (request.leftover != null && !request.leftover.isEmpty()) {
                        buffer.append(request.leftover);
                    }
                    byte[] response = buildResponse(request);
                    out.write(response);
                    out.flush();
                    log.debug("STS VIP reply to {} for {}", peer, request.requestLine);
                }
            }
        } catch (SocketException ignored) {
            // client closed
        } catch (Throwable t) {
            log.warn("STS VIP client error from {}", peer, t);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private byte[] buildResponse(StsVipProtocol.ParsedRequest request) {
        String session = request.session();
        String seq = request.seq();
        if (StsVipProtocol.isLevelGetLevel(request.requestLine, request.headers, request.body)) {
            String userId = request.userId();
            long score = resolveScore(userId);
            String body = StsVipProtocol.buildLevelReplyBody(userId, VipConfig.STS_APP_GROUP, score);
            log.info("STS Level/GetLevel userId={} score={}", userId, score);
            return StsVipProtocol.buildResponse(body, session, seq);
        }
        if (StsVipProtocol.isConnect(request.requestLine, request.body)) {
            return StsVipProtocol.buildResponse("<Reply/>\r\n", session, seq);
        }
        // Keep the session alive for other STS methods we do not implement yet.
        return StsVipProtocol.buildResponse("<Reply/>\r\n", session, seq);
    }

    private long resolveScore(String userId) {
        if (userId != null && !userId.isBlank() && DAOManager.isInitialized()) {
            try {
                Account account = DAOManager.getDAO(AccountDAO.class).getAccount(userId);
                if (account != null && account.getId() != null) {
                    Vip vip = vipService.findByAccountId(account.getId());
                    if (vip != null) {
                        return StsVipProtocol.resolveScore(vip.getLevel(), vip.getExperience(), VipConfig.STS_DEFAULT_SCORE);
                    }
                }
            } catch (Throwable t) {
                log.warn("STS VIP account lookup failed for userId={}", userId, t);
            }
        }
        return Math.max(0L, VipConfig.STS_DEFAULT_SCORE);
    }

    private static InetAddress resolveBindAddress(String host) throws IOException {
        if (host == null || host.isBlank() || "*".equals(host) || "0.0.0.0".equals(host)) {
            return null;
        }
        return InetAddress.getByName(host);
    }

    private void closeQuietly() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + n.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
