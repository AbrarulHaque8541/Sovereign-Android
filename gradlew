#!/bin/bash
# Minimal gradle wrapper bootstrap for Termux / Linux
# Downloads gradle if missing and executes

set -e

GRADLE_VERSION="8.7"
GRADLE_DIST="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_DIST}"
GRADLE_HOME="${HOME}/.gradle/wrapper/dists/${GRADLE_VERSION}"

# Find or download gradle
find_gradle() {
    if [ -f "${GRADLE_HOME}/bin/gradle" ]; then
        echo "${GRADLE_HOME}/bin/gradle"
        return 0
    fi

    return 1
}

download_gradle() {
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "${GRADLE_HOME}"
    cd "${GRADLE_HOME}"
    if command -v curl >/dev/null; then
        curl -L -o "${GRADLE_DIST}" "${GRADLE_URL}"
    elif command -v wget >/dev/null; then
        wget -O "${GRADLE_DIST}" "${GRADLE_URL}"
    else
        echo "Error: curl or wget required to download Gradle"
        exit 1
    fi
    unzip -q "${GRADLE_DIST}"
    mv gradle-${GRADLE_VERSION}/* .
    rm -rf gradle-${GRADLE_VERSION} "${GRADLE_DIST}"
    chmod +x bin/gradle
    echo "${GRADLE_HOME}/bin/gradle"
}

GRADLE_BIN=$(find_gradle) || GRADLE_BIN=$(download_gradle)

exec "$GRADLE_BIN" "$@"