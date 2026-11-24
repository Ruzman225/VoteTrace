# VoteTrace — CNS Viva Guide

VoteTrace is an academic prototype that demonstrates file integrity and
digital signatures with synthetic electoral-roll records. It is not an official
ECI system and does not connect to government services.

## Core concepts

### What is a hash?

A hash function takes input data of any length and produces a fixed-length
digest. It is a one-way summary: the original data is not recovered from the
digest. A small input change should produce a different digest.

### What is SHA-256?

SHA-256 is a member of the SHA-2 family. It produces a 256-bit (32-byte) digest,
usually displayed as 64 hexadecimal characters. VoteTrace uses it to summarize
the exact bytes in a CSV or TXT file.

### What is RSA?

RSA is a public-key cryptographic algorithm. It uses a mathematically related
key pair: one private key and one public key. This project generates a 2048-bit
RSA pair using Java's `KeyPairGenerator`.

### What are public and private keys?

- The **private key** is secret and is used here to create signatures. The app
  saves it in PKCS#8 format and it must remain local.
- The **public key** can be shared and is used to verify signatures. The app
  saves it in X.509 format.

The public key does not reveal the private key in a practical RSA setup.

### What is a digital signature?

A digital signature is cryptographic evidence that data corresponds to the
signing private key and has not changed since signing. VoteTrace uses
`SHA256withRSA`: the signature operation combines SHA-256 processing with RSA
signing. The `.sig` file is detached, so the CSV itself is not modified.

## How verification works

The verifier processes the selected file using SHA256withRSA and checks the
detached signature using the public key. The result is `VALID SIGNATURE` only
when the signature matches both the file bytes and that key. Editing even one
byte, using a signature from another file, or selecting the wrong public key
results in `INVALID SIGNATURE`.

The signature is not encryption: the file contents remain readable.

## Hashing versus encryption

- **Hashing** is a one-way operation used to create a fixed-size digest; there
  is no decryption operation.
- **Encryption** is reversible with the appropriate decryption key and is used
  to keep content confidential.
- **Digital signatures** use keys to provide authenticity and integrity, not
  confidentiality.

## CNS relevance

This project demonstrates integrity checking, public-key cryptography, key
formats, message digests, signing, verification, and the practical effect of
tampering with data. It also shows why the private key must be protected while
the public key can be distributed for verification.

## Simple live demo

1. Open `samples/electoral_roll.csv`, generate RSA keys, and calculate its
   SHA-256 digest.
2. Sign the file and verify it; show `VALID SIGNATURE`.
3. Edit a fictional record in a copy and verify that copy using the original
   `.sig` and public key; show `INVALID SIGNATURE`.
4. Try an unrelated public key to demonstrate a key mismatch.

Use synthetic records only. VoteTrace is a college demonstration, not an
official Election Commission of India system.

## Login and activity records

VoteTrace now requires a local MySQL connection and a provisioned username and
password. Passwords are stored as BCrypt hashes; the application does not
include role-based access yet. Important actions and their outcomes are written
to the `activity_log` table, including sign-in attempts, hashing, key
generation, signing, verification, sign-out, and application close. The audit
record stores a filename rather than a full path and never stores file contents
or private-key material.
