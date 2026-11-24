package com.votetrace.lite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

public final class FileSigner {

    private final CryptoService cryptoService;
    private final KeyManager keyManager;

    public FileSigner(CryptoService cryptoService, KeyManager keyManager) {
        this.cryptoService = cryptoService;
        this.keyManager = keyManager;
    }

    public void sign(Path file, Path privateKeyFile, Path signatureFile)
            throws IOException, GeneralSecurityException {
        if (file.toAbsolutePath().normalize().equals(signatureFile.toAbsolutePath().normalize())) {
            throw new IOException("The signature output must not replace the file being signed.");
        }
        byte[] signature = cryptoService.sign(file, keyManager.loadPrivateKey(privateKeyFile));
        Files.write(signatureFile, signature);
    }

    public boolean verify(Path file, Path signatureFile, Path publicKeyFile)
            throws IOException, GeneralSecurityException {
        byte[] signature = Files.readAllBytes(signatureFile);
        return cryptoService.verify(file, signature, keyManager.loadPublicKey(publicKeyFile));
    }
}
