package com.votetrace.lite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class KeyManager {

    private static final String PRIVATE_KEY_FILE = "votetrace-private.pk8";
    private static final String PUBLIC_KEY_FILE = "votetrace-public.x509";
    private static final int RSA_KEY_SIZE = 2048;

    public KeyFiles generateAndSave(Path directory) throws IOException, GeneralSecurityException {
        Files.createDirectories(directory);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        Path privateKeyFile = directory.resolve(PRIVATE_KEY_FILE);
        Path publicKeyFile = directory.resolve(PUBLIC_KEY_FILE);
        writeKeyPair(privateKeyFile, publicKeyFile, keyPair);
        restrictPrivateKeyPermissions(privateKeyFile);
        return new KeyFiles(privateKeyFile, publicKeyFile);
    }

    public PrivateKey loadPrivateKey(Path file) throws IOException, GeneralSecurityException {
        byte[] keyBytes = decodeKeyFile(file);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    public PublicKey loadPublicKey(Path file) throws IOException, GeneralSecurityException {
        byte[] keyBytes = decodeKeyFile(file);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private void writeKeyPair(Path privateKeyFile, Path publicKeyFile, KeyPair keyPair) throws IOException {
        String privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        String publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
        boolean privateKeyCreated = false;
        try {
            Files.writeString(privateKeyFile, privatePem, StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            privateKeyCreated = true;
            Files.writeString(publicKeyFile, publicPem, StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException failure) {
            if (privateKeyCreated) {
                try {
                    Files.deleteIfExists(privateKeyFile);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private byte[] decodeKeyFile(Path file) throws IOException {
        String contents = Files.readString(file, StandardCharsets.US_ASCII);
        String base64 = contents.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private String toPem(String label, byte[] encodedKey) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encodedKey);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    private void restrictPrivateKeyPermissions(Path privateKeyFile) throws IOException {
        Path parent = privateKeyFile.getParent();
        if (parent != null && Files.getFileStore(parent).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(privateKeyFile, java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        }
    }

    public record KeyFiles(Path privateKey, Path publicKey) {
    }
}
