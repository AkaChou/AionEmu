package com.aionemu.loginserver.network.sts;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal China-client STS framing helpers for VIP stage display.
 *
 * Client path: POST /Level/GetLevel -> FUN_1087a6e0 -> AccumulateGradeScore -> FUN_10577c40.
 */
public final class StsVipProtocol {

    private static final Pattern HEADER_PATTERN = Pattern.compile("(?im)^([A-Za-z]):\\s*(.+?)\\s*$");
    private static final Pattern XML_FIELD = Pattern.compile("<([A-Za-z0-9_]+)>(.*?)</\\1>", Pattern.DOTALL);

    /** vip_grade_exp.xml grade floors used when only vip_level is known. */
    private static final long[] LEVEL_TO_SCORE = {
        0L,    // 0
        178L,  // 1
        544L,  // 2
        1034L, // 3
        2069L, // 4
        3758L, // 5
        3759L  // 6
    };

    private StsVipProtocol() {
    }

    public static long scoreForLevel(int vipLevel) {
        if (vipLevel <= 0) {
            return 0L;
        }
        if (vipLevel >= LEVEL_TO_SCORE.length) {
            return LEVEL_TO_SCORE[LEVEL_TO_SCORE.length - 1];
        }
        return LEVEL_TO_SCORE[vipLevel];
    }

    public static long resolveScore(int vipLevel, long vipExp, long defaultScore) {
        if (vipExp > 0) {
            return vipExp;
        }
        if (vipLevel > 0) {
            return scoreForLevel(vipLevel);
        }
        return Math.max(0L, defaultScore);
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

    public static String extractHeader(String headers, String key) {
        if (headers == null || key == null || key.isEmpty()) {
            return null;
        }
        Matcher matcher = HEADER_PATTERN.matcher(headers);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(key)) {
                return matcher.group(2).trim();
            }
        }
        return null;
    }

    public static boolean isLevelGetLevel(String requestLine, String headers, String body) {
        String haystack = ((requestLine == null ? "" : requestLine) + "\n"
            + (headers == null ? "" : headers) + "\n"
            + (body == null ? "" : body)).toLowerCase(Locale.ROOT);
        return haystack.contains("/level/getlevel")
            || (haystack.contains("getlevel") && haystack.contains("aion_membership"))
            || haystack.contains("gametierkey");
    }

    public static boolean isConnect(String requestLine, String body) {
        String haystack = ((requestLine == null ? "" : requestLine) + "\n"
            + (body == null ? "" : body)).toLowerCase(Locale.ROOT);
        return haystack.contains("<connect");
    }

    public static String buildLevelReplyBody(String userId, String appGroup, long score) {
        String safeUser = userId == null || userId.isBlank() ? "unknown" : userId;
        String safeGroup = appGroup == null || appGroup.isBlank() ? "AION" : appGroup;
        long safeScore = Math.max(0L, score);
        return "<Reply>"
            + "<UserId>" + escapeXml(safeUser) + "</UserId>"
            + "<AppGroupCode>" + escapeXml(safeGroup) + "</AppGroupCode>"
            + "<LevelId>1</LevelId>"
            + "<AccumulateGradeScore>" + safeScore + "</AccumulateGradeScore>"
            + "<MinimumLevelScore>" + safeScore + "</MinimumLevelScore>"
            + "<MaximumLevelScore>" + safeScore + "</MaximumLevelScore>"
            + "<MaximumGradeScore>" + safeScore + "</MaximumGradeScore>"
            + "</Reply>\r\n";
    }

    public static byte[] buildResponse(String body, String session, String seq) {
        String payload = body == null ? "" : body;
        byte[] bodyBytes = payload.getBytes(StandardCharsets.UTF_8);
        StringBuilder headers = new StringBuilder(96);
        headers.append("STS/1.0 200 OK\r\n");
        if (session != null && !session.isBlank()) {
            headers.append("s:").append(session.trim()).append("\r\n");
        }
        if (seq != null && !seq.isBlank()) {
            headers.append("R:").append(seq.trim()).append("\r\n");
        }
        headers.append("l:").append(bodyBytes.length).append("\r\n\r\n");
        byte[] headBytes = headers.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, out, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, out, headBytes.length, bodyBytes.length);
        return out;
    }

    public static String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /** Best-effort first complete STS/text request from a TCP buffer. */
    public static ParsedRequest tryParse(String buffer) {
        if (buffer == null) {
            return null;
        }
        int sep = buffer.indexOf("\r\n\r\n");
        if (sep < 0) {
            return null;
        }
        String headers = buffer.substring(0, sep);
        String rest = buffer.substring(sep + 4);
        int bodyLen = 0;
        String lengthHeader = extractHeader(headers, "l");
        if (lengthHeader != null) {
            try {
                bodyLen = Integer.parseInt(lengthHeader.trim());
            } catch (NumberFormatException ignored) {
                bodyLen = 0;
            }
        }
        if (bodyLen < 0) {
            bodyLen = 0;
        }
        if (rest.length() < bodyLen) {
            return null;
        }
        String body = bodyLen > 0 ? rest.substring(0, bodyLen) : "";
        String leftover = bodyLen > 0 ? rest.substring(bodyLen) : rest;
        String requestLine = headers;
        int firstBreak = headers.indexOf("\r\n");
        if (firstBreak >= 0) {
            requestLine = headers.substring(0, firstBreak);
            headers = headers.substring(firstBreak + 2);
        }
        return new ParsedRequest(requestLine, headers, body, leftover);
    }

    public static final class ParsedRequest {
        public final String requestLine;
        public final String headers;
        public final String body;
        public final String leftover;

        public ParsedRequest(String requestLine, String headers, String body, String leftover) {
            this.requestLine = requestLine;
            this.headers = headers;
            this.body = body;
            this.leftover = leftover;
        }

        public String session() {
            return extractHeader(headers, "s");
        }

        public String seq() {
            String value = extractHeader(headers, "R");
            return value != null ? value : extractHeader(headers, "r");
        }

        public String userId() {
            String fromBody = extractXmlField(body, "UserId");
            if (fromBody != null && !fromBody.isBlank()) {
                return fromBody;
            }
            // fallback: some clients may only put path/user elsewhere
            Matcher matcher = XML_FIELD.matcher(body == null ? "" : body);
            while (matcher.find()) {
                if ("userid".equalsIgnoreCase(matcher.group(1))) {
                    return matcher.group(2).trim();
                }
            }
            return null;
        }
    }
}
