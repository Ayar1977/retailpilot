# Local Android build environment for RetailPilot (no Android Studio needed).
# Usage (from the repository root):  docker compose run --rm apk
FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/gradle-cache

RUN apt-get update \
 && apt-get install -y --no-install-recommends unzip curl git \
 && rm -rf /var/lib/apt/lists/*

# Android command-line tools
ARG CMDLINE_TOOLS_VERSION=11076708
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
 && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o /tmp/tools.zip \
 && unzip -q /tmp/tools.zip -d "$ANDROID_HOME/cmdline-tools" \
 && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
 && rm /tmp/tools.zip

ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# Same SDK packages as the GitHub CI workflow
RUN yes | sdkmanager --licenses > /dev/null \
 && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" "build-tools;35.0.0"

WORKDIR /workspace
# Scripts are read from the mounted repository, so editing them needs no image rebuild
CMD ["bash", "/workspace/docker/build.sh"]
