package com.aionemu.loginserver.network.sts;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.service.VipService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * STS 端点：向国服客户端返回 account_vip 数据。
 * STS endpoint that returns account_vip data to the China client.
 * TODO: STS 服务尚未实现完成，当前仅用于联调。 / STS is not fully implemented and is for integration testing only.
 */
@Slf4j
public final class StsVipServer {

    static final long ACCOUNT_BINDING_TTL_MILLIS = 5 * 60_000L;

    private static final Object LOCK = new Object();
    private static final Map<String, AccountBinding> AUTHENTICATED_ACCOUNTS = new ConcurrentHashMap<>();
    private static StsVipServer instance;

    private final AtomicBoolean running = new AtomicBoolean();
    private final String serviceContext;
    private final VipService vipService;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    private StsVipServer() {
        serviceContext = ServiceContext.current();
        vipService = new VipService();
    }

    /**
     * 配置启用时启动 STS 服务（幂等）。
     * Starts the STS server when enabled by config (idempotent).
     */
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

    /**
     * 停止 STS 服务并清空已认证账号缓存。
     * Stops the STS server and clears the authenticated-account cache.
     */
    public static void shutdownIfStarted() {
        StsVipServer server;
        synchronized (LOCK) {
            server = instance;
            instance = null;
        }
        if (server != null) {
            server.stop();
        }
        AUTHENTICATED_ACCOUNTS.clear();
    }

    public static void rememberAuthenticatedAccount(String ip, int accountId) {
        rememberAuthenticatedAccount(ip, accountId, System.currentTimeMillis());
    }

    static void rememberAuthenticatedAccount(String ip, int accountId, long nowMillis) {
        String key = normalizeIp(ip);
        if (key == null || accountId <= 0) {
            return;
        }
        // ponytail: O(n) cleanup on login; schedule it only if login volume makes this measurable.
        AUTHENTICATED_ACCOUNTS.entrySet().removeIf(entry -> entry.getValue().expired(nowMillis));
        AUTHENTICATED_ACCOUNTS.put(key, new AccountBinding(accountId, nowMillis));
    }

    static Integer authenticatedAccountId(String ip, long nowMillis) {
        String key = normalizeIp(ip);
        if (key == null) {
            return null;
        }
        AccountBinding binding = AUTHENTICATED_ACCOUNTS.get(key);
        if (binding == null) {
            return null;
        }
        if (binding.expired(nowMillis)) {
            AUTHENTICATED_ACCOUNTS.remove(key, binding);
            return null;
        }
        return binding.accountId();
    }

    static void clearAuthenticatedAccountsForTests() {
        AUTHENTICATED_ACCOUNTS.clear();
    }

    private void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            StsAuthCrypto.isAvailable(); // eager-load keys before accepting clients
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(bindAddress(), 50);
            acceptThread = Thread.ofPlatform().daemon().name("sts-vip-accept")
                .start(ServiceContext.wrap(this::acceptLoop, serviceContext));
            String host = VipConfig.STS_HOST == null || VipConfig.STS_HOST.isBlank() ? "*" : VipConfig.STS_HOST;
            log.info(I18n.get("log.cee114d726d8", host, VipConfig.STS_PORT));
        } catch (IOException e) {
            running.set(false);
            closeServerSocket();
            throw new IllegalStateException("Failed to start STS VIP server on port " + VipConfig.STS_PORT, e);
        }
    }

    private void stop() {
        running.set(false);
        closeServerSocket();
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().name("sts-vip-client")
                    .start(ServiceContext.wrap(() -> handleClient(socket), serviceContext));
            } catch (SocketException e) {
                if (running.get()) {
                    log.warn(I18n.get("log.d6ba62c67e76"), e);
                }
                return;
            } catch (IOException e) {
                if (running.get()) {
                    log.warn(I18n.get("log.d6ba62c67e76"), e);
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        String peer = socket.getRemoteSocketAddress() == null ? "unknown" : socket.getRemoteSocketAddress().toString();
        try (socket) {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout((int) ACCOUNT_BINDING_TTL_MILLIS);
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            while (running.get()) {
                StsVipProtocol.ParsedRequest request = StsVipProtocol.readRequest(input);
                if (request == null) {
                    return;
                }
                String peerIp = socket.getInetAddress().getHostAddress();
                log.info(I18n.get("log.4bf46ab6a57e", peerIp, request.requestLine()));
                output.write(buildResponse(request, peerIp));
                output.flush();
            }
        } catch (SocketException | SocketTimeoutException e) {
            log.debug("STS client closed: {}", peer);
        } catch (IOException e) {
            log.warn(I18n.get("log.f207e5d32e9a", peer), e);
        }
    }

    private byte[] buildResponse(StsVipProtocol.ParsedRequest request, String clientIp) {
        if (StsVipProtocol.isLoginTokenStart(request)) {
            String reply = StsVipProtocol.buildLoginTokenStartReplyBody();
            return StsVipProtocol.buildResponse(reply, request.sequence());
        }
        if (StsVipProtocol.isTokenKeyData(request)) {
            return StsVipProtocol.buildResponse(
                StsVipProtocol.buildTokenKeyDataReplyBody(),
                request.sequence()
            );
        }
        if (StsVipProtocol.isLoginFinish(request)) {
            String userId = resolveUserId(clientIp);
            return StsVipProtocol.buildResponse(
                StsVipProtocol.buildLoginFinishReplyBody(userId),
                request.sequence()
            );
        }
        if (StsVipProtocol.isListMyAccounts(request)) {
            String alias = resolveUserId(clientIp);
            return StsVipProtocol.buildResponse(
                StsVipProtocol.buildListMyAccountsReplyBody(alias),
                request.sequence()
            );
        }
        if (StsVipProtocol.isRequestGameToken(request)) {
            String alias = resolveUserId(clientIp);
            // ponytail: opaque token; client only stores/logs it
            String token = "local-" + alias;
            return StsVipProtocol.buildResponse(
                StsVipProtocol.buildRequestGameTokenReplyBody(token),
                request.sequence()
            );
        }
        if (StsVipProtocol.isLevelGetLevel(request)) {
            Integer accountId = authenticatedAccountId(clientIp, System.currentTimeMillis());
            long score = resolveScore(accountId);
            log.info(I18n.get("log.64d0e3575bc9", clientIp, accountId, score));
            return StsVipProtocol.buildResponse(StsVipProtocol.buildLevelReplyBody(score), request.sequence());
        }
        // Keep unknown STS calls alive with empty OK so the client can advance.
        log.debug("STS unhandled path from {}: {}", clientIp, request.requestLine());
        return StsVipProtocol.buildResponse("<Reply/>\r\n", request.sequence());
    }

    private String resolveUserId(String clientIp) {
        Integer accountId = authenticatedAccountId(clientIp, System.currentTimeMillis());
        if (accountId == null || !DAOManager.isInitialized()) {
            return "unknown";
        }
        try {
            Account account = DAOManager.getDAO(AccountDAO.class).getAccount(accountId);
            if (account != null && account.getName() != null && !account.getName().isBlank()) {
                return account.getName();
            }
        } catch (RuntimeException e) {
            log.warn(I18n.get("log.eab3b20d42d7", accountId), e);
        }
        return String.valueOf(accountId);
    }

    private long resolveScore(Integer accountId) {
        if (accountId == null || !DAOManager.isInitialized()) {
            return 0L;
        }
        try {
            Vip vip = vipService.findByAccountId(accountId);
            long now = System.currentTimeMillis() / 1000L;
            if (vip == null || !vip.isActive(now)) {
                return 0L;
            }
            return StsVipProtocol.resolveScore(vip.getLevel(), vip.getExperience());
        } catch (RuntimeException e) {
            log.warn(I18n.get("log.eab3b20d42d7", accountId), e);
            return 0L;
        }
    }

    private static InetSocketAddress bindAddress() {
        String host = VipConfig.STS_HOST;
        if (host == null || host.isBlank() || "*".equals(host) || "0.0.0.0".equals(host)) {
            return new InetSocketAddress(VipConfig.STS_PORT);
        }
        return new InetSocketAddress(host, VipConfig.STS_PORT);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        return ip.trim().toLowerCase(Locale.ROOT);
    }

    private void closeServerSocket() {
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        serverSocket = null;
    }

    private record AccountBinding(int accountId, long authenticatedAtMillis) {

        boolean expired(long nowMillis) {
            return nowMillis - authenticatedAtMillis >= ACCOUNT_BINDING_TTL_MILLIS;
        }
    }
}
