// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Base64

val EMBEDDED_DEBUG_KEYSTORE_B64 =
  "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"

val rootDebugKeystore = file("${rootDir}/debug.keystore")
if (!rootDebugKeystore.exists()) {
  try {
    val rootDebugKeystoreBase64 = file("${rootDir}/debug.keystore.base64")
    val b64 = if (rootDebugKeystoreBase64.exists()) {
      rootDebugKeystoreBase64.readText().replace("\\s+".toRegex(), "")
    } else {
      EMBEDDED_DEBUG_KEYSTORE_B64
    }
    rootDebugKeystore.writeBytes(Base64.getDecoder().decode(b64))
  } catch (e: Exception) {
    logger.warn("Unable to restore debug.keystore: ${e.message}")
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
