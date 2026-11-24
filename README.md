# HEM Prompt Enhancer (100% Offline Android)

Native Android Java application built for **100% offline use**.
- **Zero API keys**
- **Zero internet permissions** (`AndroidManifest.xml` does not even request `INTERNET`)
- **Zero pre-baked rules** (starts completely flat at zero — you control every rule and banned word)
- **3 Local Profile Slots** (switch between different rule sets in one tap)
- **Local Banned-Word / Cliché Stripper** + **3 Output Modes** (`STRUCTURED`, `INLINE`, `SCRUB ONLY`)

## Build the APK in GitHub Actions
1. Create a new GitHub repository (public or private).
2. Unzip `HEM-PromptEnhancer-Offline-Android.zip` and upload **all files and folders** directly to the **root** of the `main` branch (make sure `.github/workflows/android.yml` is at the root, not inside a subfolder).
3. Open the **Actions** tab in GitHub -> **Build Offline Android APK** -> click **Run workflow** (it also triggers automatically on push to `main`).
4. When the green checkmark finishes, click the workflow run and download the **HEM-PromptEnhancer-Offline-APK** artifact.
5. Unzip it and install `app-debug.apk` on your Android phone.
