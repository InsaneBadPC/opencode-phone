#!/usr/bin/env bash
# ==============================================================================
# OpenCode Android - Semantic Versioning & Release Automation Script
# Usage:
#   ./scripts/bump_version.sh <version> [commit_message]
# Examples:
#   ./scripts/bump_version.sh 1.1.0 "Add YouTube Agent and Termux CLI sync"
#   ./scripts/bump_version.sh 1.0.1 "Fix minor UI layout issue"
# ==============================================================================

set -e

NEW_VERSION="$1"
MESSAGE="${2:-Release version $NEW_VERSION}"

if [ -z "$NEW_VERSION" ]; then
  echo "❌ Chyba: Musíte zadat verzi, např: ./scripts/bump_version.sh 1.1.0"
  echo "Použití: ./scripts/bump_version.sh <1.X.Y> [zpráva commitu]"
  exit 1
fi

# Clean up version string
NEW_VERSION="${NEW_VERSION#v}"

# Parse major, minor, patch
IFS='.' read -r MAJOR MINOR PATCH <<< "$NEW_VERSION"
if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
  echo "❌ Chyba: Verze musí být ve formátu SemVer X.Y.Z (např. 1.1.0 nebo 1.0.1)"
  exit 1
fi

# Calculate versionCode (e.g. 1.1.0 -> 10100, 1.2.3 -> 10203)
VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))

echo "🚀 Aktualizuji OpenCode na verzi: v$NEW_VERSION (versionCode: $VERSION_CODE)"

# 1. Update app/build.gradle.kts
sed -i "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/g" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$NEW_VERSION\"/g" app/build.gradle.kts

echo "✓ Aktualizováno app/build.gradle.kts"

# 2. Git Commit
git add app/build.gradle.kts
if git status --porcelain | grep -q .; then
  git add -A
  git commit -m "chore(release): v$NEW_VERSION - $MESSAGE"
  echo "✓ Vytvořen git commit pro v$NEW_VERSION"
else
  echo "ℹ Žádné změny k uložení do commitu"
fi

# 3. Create Git Tag
TAG_NAME="v$NEW_VERSION"
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
  echo "⚠️ Tag $TAG_NAME již existuje, přepisuji..."
  git tag -d "$TAG_NAME"
fi

git tag -a "$TAG_NAME" -m "OpenCode Release $TAG_NAME: $MESSAGE"
echo "✓ Vytvořen git tag $TAG_NAME"

# 4. Push to Remote if origin is set
if git remote get-url origin >/dev/null 2>&1; then
  echo "📡 Odesílám větve a tagy na vzdálený GitHub repozitář..."
  git push origin master --tags || git push origin main --tags || echo "⚠️ Push na origin selhal, zkontrolujte nastavení repozitáře."
  echo "✓ Push na GitHub dokončen! GitHub Actions nyní automaticky vytvoří Release a sestaví APK."
else
  echo "ℹ Vzdálený repozitář 'origin' zatím není nastaven."
  echo "Pro odeslání na GitHub spusťte:"
  echo "  git remote add origin https://github.com/VASE_JMENO/REPOZITAR.git"
  echo "  git push -u origin master --tags"
fi

echo "🎉 Hotovo! Aplikace OpenCode je verzována jako v$NEW_VERSION."
