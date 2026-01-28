#!/bin/sh
set -e

########################################
# CONFIGURATION
########################################

# Directory where plugin JARs will be stored
PLUGINS_DIR=${PLUGIN_LOADER_PATH:-./docker/integration/resources/plugins}

# Maven Central base URL
MAVEN_REPO=https://repo1.maven.org/maven2

# List of plugins to download
# Format: groupId:artifactId:version
PLUGINS="
io.github.linagora.linid.im:hpp:0.1.1
"

########################################
# LOGIC
########################################

mkdir -p "$PLUGINS_DIR"

download_plugin() {
  GROUP=$(echo "$1" | cut -d: -f1)
  ARTIFACT=$(echo "$1" | cut -d: -f2)
  VERSION=$(echo "$1" | cut -d: -f3)

  GROUP_PATH=$(echo "$GROUP" | tr '.' '/')
  JAR_NAME="${ARTIFACT}-${VERSION}.jar"
  TARGET_FILE="$PLUGINS_DIR/$JAR_NAME"
  DOWNLOAD_URL="$MAVEN_REPO/$GROUP_PATH/$ARTIFACT/$VERSION/$JAR_NAME"

  if [ -f "$TARGET_FILE" ]; then
    echo "[SKIP] $JAR_NAME already exists"
    return
  fi

  echo "[DOWNLOAD] $ARTIFACT:$VERSION"
  curl -fL "$DOWNLOAD_URL" -o "$TARGET_FILE"
}

echo "$PLUGINS" | while read -r plugin; do
  [ -z "$plugin" ] && continue
  download_plugin "$plugin"
done

echo "All plugins are available in $PLUGINS_DIR"
