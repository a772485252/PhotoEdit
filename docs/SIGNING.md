# Personal debug signing

PhotoEdit uses standard Android debug signing with one persistent keystore. No release signing configuration or password secrets are needed. Standard password: android; alias: androiddebugkey.

One-time setup on a trusted machine with JDK keytool:
```sh
keytool -genkeypair -keystore photoedit-debug.keystore -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

Keep a backup of this file. Never regenerate it for later builds. Add its base64 content as the single repository Actions secret PHOTOEDIT_DEBUG_KEYSTORE_BASE64, then run Android APK manually. PowerShell conversion:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\photoedit-debug.keystore)))
```

Never commit the keystore or its base64 content to the public repository. The password is a standard debug password, not an additional secret. Push/manual publication requires the fixed key and stops if missing. PR builds use temporary debug keys for checks only and never publish latest-apk. Runner key is removed after compilation. Signature verification runs before upload.

Application ID stays com.photoedit.app; version 0.5.1/code 6. Increment versionCode for future releases. Switching from a lost old CI key may require one uninstall/reinstall; preserve API keys for re-entry first. Future builds using this fixed key can update each other. Device update/data preservation still needs testing.
