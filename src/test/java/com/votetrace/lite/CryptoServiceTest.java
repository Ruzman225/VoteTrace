package com.votetrace.lite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceTest {

    @TempDir
    Path temporaryDirectory;

    private final CryptoService cryptoService = new CryptoService();
    private final KeyManager keyManager = new KeyManager();

    @Test
    void sha256MatchesKnownDigest() throws Exception {
        Path file = temporaryDirectory.resolve("abc.txt");
        Files.writeString(file, "abc");

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                cryptoService.sha256Hex(file));
    }

    @Test
    void hashesSignsAndDetectsModifiedFile() throws Exception {
        Path originalFile = temporaryDirectory.resolve("electoral_roll.csv");
        Files.writeString(originalFile, "roll_id,name,ward\n001,Asha,North\n");
        String originalHash = cryptoService.sha256Hex(originalFile);
        KeyPair keyPair = generateKeyPair();
        byte[] signature = cryptoService.sign(originalFile, keyPair.getPrivate());

        assertEquals(64, originalHash.length());
        assertTrue(cryptoService.verify(originalFile, signature, keyPair.getPublic()));

        Files.writeString(originalFile, "roll_id,name,ward\n001,Asha,South\n");
        assertNotEquals(originalHash, cryptoService.sha256Hex(originalFile));
        assertFalse(cryptoService.verify(originalFile, signature, keyPair.getPublic()));
    }

    @Test
    void wrongPublicKeyAndMalformedSignatureDoNotVerify() throws Exception {
        Path file = temporaryDirectory.resolve("electoral_roll.txt");
        Files.writeString(file, "synthetic voter roll");
        KeyPair signingPair = generateKeyPair();
        KeyPair otherPair = generateKeyPair();
        byte[] signature = cryptoService.sign(file, signingPair.getPrivate());

        assertFalse(cryptoService.verify(file, signature, otherPair.getPublic()));
        assertFalse(cryptoService.verify(file, new byte[]{1, 2, 3}, signingPair.getPublic()));
    }

    @Test
    void generatedKeysAreSavedAndCanSignAndVerify() throws Exception {
        Path keyDirectory = temporaryDirectory.resolve("keys");
        KeyManager.KeyFiles files = keyManager.generateAndSave(keyDirectory);
        Path file = temporaryDirectory.resolve("roll.csv");
        Path signature = temporaryDirectory.resolve("roll.sig");
        Files.writeString(file, "id,name\n001,Meera\n");

        FileSigner fileSigner = new FileSigner(cryptoService, keyManager);
        fileSigner.sign(file, files.privateKey(), signature);

        assertTrue(Files.exists(files.privateKey()));
        assertTrue(Files.exists(files.publicKey()));
        assertTrue(Files.exists(signature));
        assertTrue(fileSigner.verify(file, signature, files.publicKey()));
    }

    @Test
    void signingCannotReplaceTheSourceFile() throws Exception {
        Path file = temporaryDirectory.resolve("protected.csv");
        Files.writeString(file, "synthetic roll");
        String originalContent = Files.readString(file);
        KeyPair keyPair = generateKeyPair();
        Path privateKey = temporaryDirectory.resolve("unused.pk8");
        FileSigner fileSigner = new FileSigner(cryptoService, keyManager);

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> fileSigner.sign(file, privateKey, file));

        assertEquals(originalContent, Files.readString(file));
    }

    @Test
    void missingInputFilesAreReported() {
        Path missing = temporaryDirectory.resolve("missing.csv");

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> cryptoService.sha256Hex(missing));
    }

    @Test
    void missingSignatureAndPublicKeyFilesAreReported() throws Exception {
        Path file = temporaryDirectory.resolve("existing.csv");
        Path missingSignature = temporaryDirectory.resolve("missing.sig");
        Path missingPublicKey = temporaryDirectory.resolve("missing.x509");
        Files.writeString(file, "synthetic roll");
        FileSigner fileSigner = new FileSigner(cryptoService, keyManager);

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> fileSigner.verify(file, missingSignature, missingPublicKey));

        Path signature = temporaryDirectory.resolve("present.sig");
        Files.write(signature, new byte[]{1, 2, 3});
        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> fileSigner.verify(file, signature, missingPublicKey));
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
