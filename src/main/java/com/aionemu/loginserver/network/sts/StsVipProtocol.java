package com.aionemu.loginserver.network.sts;

import com.aionemu.boot.i18n.I18n;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/** China-client STS/1.0 framing for membership stage + token auth. */
@Slf4j
public final class StsVipProtocol {

    private static final int MAX_LINE_BYTES = 8 * 1024;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final long MAX_VIP_SCORE = 3759L;
    private static final long[] LEVEL_TO_SCORE = { 0L, 178L, 544L, 1034L, 2069L, 3758L, 3759L };
    private static final SecureRandom RANDOM = new SecureRandom();

    private StsVipProtocol() {
    }

    public static long resolveScore(int vipLevel, long vipExp) {
        if (vipExp > 0) {
            return vipExp;
        }
        if (vipLevel <= 0) {
            return 0L;
        }
        return LEVEL_TO_SCORE[Math.min(vipLevel, LEVEL_TO_SCORE.length - 1)];
    }

    public static ParsedRequest readRequest(InputStream input) throws IOException {
        String requestLine = readLine(input);
        if (requestLine == null) {
            return null;
        }

        String sequence = null;
        int bodyLength = 0;
        while (true) {
            String header = readLine(input);
            if (header == null) {
                throw new EOFException("Unexpected end of STS headers");
            }
            if (header.isEmpty()) {
                break;
            }
            int colon = header.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = header.substring(0, colon).trim();
            String value = header.substring(colon + 1).trim();
            if ("s".equalsIgnoreCase(key)) {
                sequence = value;
            } else if ("l".equalsIgnoreCase(key)) {
                bodyLength = parseBodyLength(value);
            }
        }

        byte[] body = input.readNBytes(bodyLength);
        if (body.length != bodyLength) {
            throw new EOFException("Unexpected end of STS body");
        }
        return new ParsedRequest(requestLine, new String(body, StandardCharsets.UTF_8), sequence);
    }

    public static boolean isConnect(ParsedRequest request) {
        return request.isPath("/Sts/Connect");
    }

    public static boolean isLoginTokenStart(ParsedRequest request) {
        return request.isPath("/Auth/LoginTokenStart");
    }

    public static boolean isTokenKeyData(ParsedRequest request) {
        return request.isPath("/Auth/TokenKeyData");
    }

    public static boolean isLoginFinish(ParsedRequest request) {
        // Client dispatches both the STS path and the internal protocol name.
        return request.isPath("/Auth/LoginFinish")
            || "POST TokenLoginFinish STS/1.0".equalsIgnoreCase(request.requestLine().trim());
    }

    public static boolean isListMyAccounts(ParsedRequest request) {
        return request.isPath("/GameAccount/ListMyAccounts");
    }

    public static boolean isRequestGameToken(ParsedRequest request) {
        return request.isPath("/Auth/RequestGameToken");
    }

    public static boolean isLevelGetLevel(ParsedRequest request) {
        // Client may send /Level/GetLevel or /Grade/GetLevel; membership body is the filter.
        return (request.isPath("/Level/GetLevel") || request.isPath("/Grade/GetLevel"))
            && request.body().toUpperCase(Locale.ROOT).contains("AION_MEMBERSHIP");
    }

    public static String extractXmlField(String body, String tag) {
        if (body == null || tag == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(
            "<" + Pattern.quote(tag) + ">(.*?)</" + Pattern.quote(tag) + ">",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        ).matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    /**
     * LoginTokenStart reply with private-server trust-anchor.
     * Requires Game.dll patched with the matching public NC blob.
     */
    public static String buildLoginTokenStartReplyBody() {
        if (!StsAuthCrypto.isAvailable()) {
            log.error(I18n.get("log.c9e58ac1d427"));
            // ServerRand-only keeps client alive but will not finish auth.
            return buildServerRandOnlyReply();
        }
        try {
            byte[] serverRand = new byte[32];
            RANDOM.nextBytes(serverRand);
            StsAuthCrypto crypto = StsAuthCrypto.get();
            byte[] signature = crypto.signServerPublicKey();
            String body = "<Reply>"
                + "<ServerRand>" + Base64.getEncoder().encodeToString(serverRand) + "</ServerRand>"
                + "<ServerPublicKey>" + crypto.publicKeyB64() + "</ServerPublicKey>"
                + "<ServerSignature>" + Base64.getEncoder().encodeToString(signature) + "</ServerSignature>"
                + "</Reply>\r\n";
            log.info(I18n.get(
                "log.3c7011fd3dcf",
                serverRand.length,
                signature.length,
                crypto.publicKeyFingerprint()
            ));
            return body;
        } catch (GeneralSecurityException e) {
            log.error(I18n.get("log.a627ce227b9d"), e);
            return buildServerRandOnlyReply();
        }
    }

    private static String buildServerRandOnlyReply() {
        byte[] serverRand = new byte[32];
        RANDOM.nextBytes(serverRand);
        return "<Reply>"
            + "<ServerRand>" + Base64.getEncoder().encodeToString(serverRand) + "</ServerRand>"
            + "</Reply>\r\n";
    }

    public static String buildTokenKeyDataReplyBody() {
        return "<Reply/>\r\n";
    }

    public static String buildLoginFinishReplyBody(String userId) {
        String safeUser = userId == null || userId.isBlank() ? "unknown" : escapeXml(userId);
        return "<Reply>"
            + "<UserId>" + safeUser + "</UserId>"
            + "<UserName>" + safeUser + "</UserName>"
            + "</Reply>\r\n";
    }

    /** Client walks Reply/GameAccount/Alias after LoginFinish. */
    public static String buildListMyAccountsReplyBody(String alias) {
        String safe = alias == null || alias.isBlank() ? "unknown" : escapeXml(alias);
        return "<Reply>"
            + "<GameAccount>"
            + "<Alias>" + safe + "</Alias>"
            + "</GameAccount>"
            + "</Reply>\r\n";
    }

    /** Client requires Reply/Token after ListMyAccounts. */
    public static String buildRequestGameTokenReplyBody(String token) {
        String safe = token == null || token.isBlank() ? "local-token" : escapeXml(token);
        return "<Reply>"
            + "<Token>" + safe + "</Token>"
            + "</Reply>\r\n";
    }

    public static String buildLevelReplyBody(long score) {
        long safeScore = Math.max(0L, score);
        long maximumScore = Math.max(MAX_VIP_SCORE, safeScore);
        return "<Reply>"
            + "<LevelId>1</LevelId>"
            + "<AppGroupCode>AION</AppGroupCode>"
            + "<MaximumLevelScore>" + maximumScore + "</MaximumLevelScore>"
            + "<MaximumGradeScore>" + maximumScore + "</MaximumGradeScore>"
            + "<AccumulateGradeScore>" + safeScore + "</AccumulateGradeScore>"
            + "<MinimumLevelScore>0</MinimumLevelScore>"
            + "<ReservedScore>0</ReservedScore>"
            + "<PolicyVersion>1</PolicyVersion>"
            + "<EffectiveTimeSec>0</EffectiveTimeSec>"
            + "</Reply>\r\n";
    }

    public static byte[] buildResponse(String body, String sequence) {
        byte[] bodyBytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        StringBuilder header = new StringBuilder(64).append("STS/1.0 200 OK\r\n");
        if (sequence != null && !sequence.isBlank()) {
            header.append("s:").append(sequence.trim()).append("\r\n");
        }
        header.append("l:").append(bodyBytes.length).append("\r\n\r\n");
        byte[] headerBytes = header.toString().getBytes(StandardCharsets.US_ASCII);
        byte[] response = new byte[headerBytes.length + bodyBytes.length];
        System.arraycopy(headerBytes, 0, response, 0, headerBytes.length);
        System.arraycopy(bodyBytes, 0, response, headerBytes.length, bodyBytes.length);
        return response;
    }

    public static String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private static int parseBodyLength(String value) throws IOException {
        try {
            int length = Integer.parseInt(value);
            if (length < 0 || length > MAX_BODY_BYTES) {
                throw new IOException("STS body length out of range: " + length);
            }
            return length;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid STS body length: " + value, e);
        }
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream(64);
        while (true) {
            int value = input.read();
            if (value < 0) {
                if (line.size() == 0) {
                    return null;
                }
                throw new EOFException("Unexpected end of STS line");
            }
            if (value == '\n') {
                byte[] bytes = line.toByteArray();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') {
                    length--;
                }
                return new String(bytes, 0, length, StandardCharsets.US_ASCII);
            }
            if (line.size() >= MAX_LINE_BYTES) {
                throw new IOException("STS header line too long");
            }
            line.write(value);
        }
    }

    public record ParsedRequest(String requestLine, String body, String sequence) {

        boolean isPath(String path) {
            return ("POST " + path + " STS/1.0").equalsIgnoreCase(requestLine.trim());
        }
    }
}
