package com.votetrace.lite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HexFormat;

public final class CryptoService {

    private static final int BUFFER_SIZE = 8192;

    public String sha256Hex(Path file) throws IOException, GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        updateFromFile(file, (bytes, length) -> digest.update(bytes, 0, length));
        return HexFormat.of().formatHex(digest.digest());
    }

    public byte[] sign(Path file, PrivateKey privateKey) throws IOException, GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        updateFromFile(file, (bytes, length) -> signature.update(bytes, 0, length));
        return signature.sign();
    }

    public boolean verify(Path file, byte[] signedBytes, PublicKey publicKey)
            throws IOException, GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        updateFromFile(file, (bytes, length) -> signature.update(bytes, 0, length));
        try {
            return signature.verify(signedBytes);
        } catch (java.security.SignatureException malformedSignature) {
            return false;
        }
    }

    private void updateFromFile(Path file, ByteConsumer consumer)
            throws IOException, GeneralSecurityException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                consumer.accept(buffer, bytesRead);
            }
        }
    }

    @FunctionalInterface
    private interface ByteConsumer {
        void accept(byte[] bytes, int length) throws GeneralSecurityException;
    }
}
