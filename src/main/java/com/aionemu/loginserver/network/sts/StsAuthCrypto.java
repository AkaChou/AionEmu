package com.aionemu.loginserver.network.sts;

import com.aionemu.boot.i18n.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import lombok.extern.slf4j.Slf4j;

/**
 * STS token-auth 加密工具：私钥须与打进 Game.dll 0xE4D270 处的 NC 公钥 blob 匹配（见 scripts/patch_game_dll_sts_auth_key.py）。
 * STS token-auth crypto: the private key must match the NC public blob patched into Game.dll
 * at 0xE4D270 (see scripts/patch_game_dll_sts_auth_key.py).
 */
@Slf4j
public final class StsAuthCrypto {

    private static final String PRIVATE_KEY_RESOURCE = "/sts/sts_private.pem";
    private static final String PUBLIC_B64_RESOURCE = "/sts/sts_public_nc.b64";

    private static volatile StsAuthCrypto INSTANCE;
    private static final Object LOAD_LOCK = new Object();

    private final PrivateKey privateKey;
    private final String publicKeyB64;

    private StsAuthCrypto(PrivateKey privateKey, String publicKeyB64) {
        this.privateKey = privateKey;
        this.publicKeyB64 = publicKeyB64;
    }

    public static StsAuthCrypto get() {
        StsAuthCrypto local = INSTANCE;
        if (local != null) {
            return local;
        }
        synchronized (LOAD_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = load();
            }
            return INSTANCE;
        }
    }

    public static boolean isAvailable() {
        return get() != null;
    }

    public String publicKeyB64() {
        return publicKeyB64;
    }

    public String publicKeyFingerprint() {
        // 前 24 个字符即可确认 DLL 与服务器密钥匹配。 / first 24 chars is enough to confirm DLL/server match
        return publicKeyB64.length() <= 24 ? publicKeyB64 : publicKeyB64.substring(0, 24);
    }

    /** Game.dll expects little-endian raw-RSA(MD5(decoded ServerPublicKey)), without PKCS#1 padding. */
    public byte[] signServerPublicKey() throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("MD5").digest(Base64.getDecoder().decode(publicKeyB64));
        byte[] input = new byte[256];
        for (int i = 0; i < digest.length; i++) {
            input[input.length - 1 - i] = digest[i];
        }
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] signature = cipher.doFinal(input);
        reverse(signature);
        return signature;
    }

    private static StsAuthCrypto load() {
        try {
            String pem = readPrivatePem();
            String b64 = readPublicB64();
            PrivateKey key = readPkcs8Pem(pem);
            byte[] raw = Base64.getDecoder().decode(b64);
            if (raw.length != 268 || b64.length() != 360) {
                throw new IllegalStateException(
                    "STS public NC blob must be 268 bytes / 360 Base64, got "
                        + raw.length + " / " + b64.length()
                );
            }
            StsAuthCrypto crypto = new StsAuthCrypto(key, b64);
            if (crypto.signServerPublicKey().length != 256) {
                throw new IllegalStateException("unexpected signature length");
            }
            log.info(I18n.get("log.63fcf9357a62", crypto.publicKeyFingerprint()));
            return crypto;
        } catch (Exception e) {
            log.error(I18n.get("log.5f0f615f330b", e.toString()));
            return null;
        }
    }

    private static String readPrivatePem() throws IOException {
        String fromRes = readResourceOrNull(PRIVATE_KEY_RESOURCE);
        if (fromRes != null) {
            return fromRes;
        }
        for (Path path : candidatePrivatePaths()) {
            if (Files.isRegularFile(path)) {
                log.debug("STS private key loaded from file {}", path.toAbsolutePath());
                return Files.readString(path, StandardCharsets.US_ASCII);
            }
        }
        throw new IOException("missing " + PRIVATE_KEY_RESOURCE + " and no file fallback");
    }

    private static String readPublicB64() throws IOException {
        String fromRes = readResourceOrNull(PUBLIC_B64_RESOURCE);
        if (fromRes != null) {
            return fromRes.trim().split("\\R", 2)[0].trim();
        }
        for (Path path : candidatePublicPaths()) {
            if (Files.isRegularFile(path)) {
                log.debug("STS public blob loaded from file {}", path.toAbsolutePath());
                return Files.readString(path, StandardCharsets.US_ASCII).trim().split("\\R", 2)[0].trim();
            }
        }
        throw new IOException("missing " + PUBLIC_B64_RESOURCE + " and no file fallback");
    }

    private static Path[] candidatePrivatePaths() {
        Path cwd = Path.of("").toAbsolutePath();
        return new Path[] {
            cwd.resolve("scripts/sts_auth/sts_private.pem"),
            cwd.resolve("sts_auth/sts_private.pem"),
            cwd.resolve("sts_private.pem"),
        };
    }

    private static Path[] candidatePublicPaths() {
        Path cwd = Path.of("").toAbsolutePath();
        return new Path[] {
            cwd.resolve("scripts/sts_auth/sts_public_nc.b64"),
            cwd.resolve("sts_auth/sts_public_nc.b64"),
            cwd.resolve("sts_public_nc.b64"),
        };
    }

    private static String readResourceOrNull(String path) throws IOException {
        try (InputStream in = StsAuthCrypto.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private static PrivateKey readPkcs8Pem(String pem) throws GeneralSecurityException {
        String normalized = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static void reverse(byte[] bytes) {
        for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
            byte value = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = value;
        }
    }
}
