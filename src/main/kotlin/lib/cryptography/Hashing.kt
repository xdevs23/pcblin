package lib.cryptography

import java.security.MessageDigest

fun hashString(input: String, algorithm: String) =
    MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }

val String.sha256 get() = hashString(this, "SHA-256")
val String.md5 get() = hashString(this, "MD5")
