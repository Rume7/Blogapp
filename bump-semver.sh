#!/bin/bash
set -e

# Get current version from parent pom.xml
CURRENT_VERSION=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
echo "Current version: $CURRENT_VERSION"

# Parse version
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Default bump type
BUMP_TYPE="patch"

# Optionally, set BUMP_TYPE from an argument or environment variable
if [ -n "$1" ]; then
  BUMP_TYPE="$1"
fi

case $BUMP_TYPE in
  major)
    MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0;;
  minor)
    MINOR=$((MINOR+1)); PATCH=0;;
  patch)
    PATCH=$((PATCH+1));;
  *)
    echo "Unknown bump type: $BUMP_TYPE"; exit 1;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "Bumping to new version: $NEW_VERSION"

# Update all pom.xml files
mvn versions:set -DnewVersion=$NEW_VERSION
mvn versions:update-child-modules

# Optionally, commit and push
git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add pom.xml */pom.xml
git commit -m "chore: bump version to $NEW_VERSION"
git push 