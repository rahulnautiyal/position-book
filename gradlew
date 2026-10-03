#!/bin/sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.14.3"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"
ZIP_FILE="${CACHE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "${DIST_DIR}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  if [ ! -f "${ZIP_FILE}" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --retry-delay 2 "${DIST_URL}" -o "${ZIP_FILE}"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "${ZIP_FILE}" "${DIST_URL}"
    else
      echo "ERROR: curl or wget is required to bootstrap Gradle." >&2
      exit 1
    fi
  fi

  TMP_DIR="${CACHE_DIR}/.unpack-$$"
  rm -rf "${TMP_DIR}"
  mkdir -p "${TMP_DIR}"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "${ZIP_FILE}" -d "${TMP_DIR}"
  else
    echo "ERROR: unzip is required to bootstrap Gradle." >&2
    exit 1
  fi
  rm -rf "${DIST_DIR}"
  mv "${TMP_DIR}/gradle-${GRADLE_VERSION}" "${DIST_DIR}"
  rm -rf "${TMP_DIR}"
fi

exec "${DIST_DIR}/bin/gradle" --no-daemon "$@"
