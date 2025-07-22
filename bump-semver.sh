#!/bin/bash
set -e

echo "Fetching latest tags..."
git fetch --tags

# Get latest tag (default to v1.0.0 if none)
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v1.0.0")
echo "Latest tag: $LATEST_TAG"

# Remove 'v' prefix
VERSION=${LATEST_TAG#v}
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION"

# Default bump type
BUMP_TYPE="patch"

# Try to get PR labels from GitHub API if GITHUB_TOKEN and PR number are available
if [[ -n "$GITHUB_TOKEN" && -n "$PR_NUMBER" ]]; then
  LABELS=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER" | jq -r '.labels[].name // empty')
  if [ -n "$LABELS" ]; then
    if echo "$LABELS" | grep -q "bump:major"; then
      BUMP_TYPE="major"
    elif echo "$LABELS" | grep -q "bump:minor"; then
      BUMP_TYPE="minor"
    elif echo "$LABELS" | grep -q "bump:patch"; then
      BUMP_TYPE="patch"
    fi
  fi
fi

echo "Bump type: $BUMP_TYPE"

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
NEW_TAG="v$NEW_VERSION"
echo "New version: $NEW_VERSION"

# Update all pom.xml files
mvn versions:set -DnewVersion=$NEW_VERSION
mvn versions:update-child-modules

git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add pom.xml */pom.xml

git commit -m "chore: bump version to $NEW_VERSION"
git tag $NEW_TAG
git push origin HEAD --tags 