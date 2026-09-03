// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Base64

val rootDebugKeystore = file("${rootDir}/debug.keystore")
val rootDebugKeystoreBase64 = file("${rootDir}/debug.keystore.base64")
if (!rootDebugKeystore.exists() && rootDebugKeystoreBase64.exists()) {
  try {
    val cleanBase64 = rootDebugKeystoreBase64.readText().replace("\\s+".toRegex(), "")
    rootDebugKeystore.writeBytes(Base64.getDecoder().decode(cleanBase64))
  } catch (e: Exception) {
    logger.warn("Unable to restore debug.keystore from base64: ${e.message}")
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
