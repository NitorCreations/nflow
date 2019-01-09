#!/bin/bash

prompt_continue() {
  echo
  echo "Next: $1"
  read -p "Press enter to continue..."
}

valid_version() {
  if [[ ! $1 =~ ^([0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3})$ ]]; then
    return 1
  fi
  return 0
}

GIT_STATUS=$(git status --porcelain)
if [[ $GIT_STATUS ]]; then
  echo "Error: unversioned or uncommitted files"
  echo "Git status:"
  echo $GIT_STATUS
  exit 1
fi

if [[ $# -lt 3 ]]; then
  echo "Usage: `basename $0` <previous_release_version_number> <new_release_version_number> <next_snapshot_version_number> [<gpg_passphrase>]"
  echo "Example: `basename $0` 2.0.0 2.1.0 2.1.1 mypassphrase"
  exit 1
fi

PREVIOUS_VERSION=$1
if ! valid_version $PREVIOUS_VERSION; then
  echo "Error: invalid previous release version number $PREVIOUS_VERSION"
  exit 1
fi
shift
RELEASE_VERSION=$1
if ! valid_version $RELEASE_VERSION; then
  echo "Error: invalid new release version number $RELEASE_VERSION"
  exit 1
fi
shift
SNAPSHOT_VERSION=$1
if ! valid_version $SNAPSHOT_VERSION; then
  echo "Error: invalid snapshot version number $SNAPSHOT_VERSION"
  exit 1
fi
shift
SNAPSHOT_VERSION=$SNAPSHOT_VERSION-SNAPSHOT

if [[ -n $1 ]]; then
  GPG_PASSPHRASE="-Dgpg.passphrase=$1"
fi
shift

prompt_continue "set version $RELEASE_VERSION to local git repository"

mvn versions:set -DnewVersion=$RELEASE_VERSION
sed -i -e "s/$PREVIOUS_VERSION/$RELEASE_VERSION/g" README.md
git commit -am "release $RELEASE_VERSION"

prompt_continue "push version $RELEASE_VERSION to remote git repository"

git push

prompt_continue "release version $RELEASE_VERSION to Maven Central"

mvn -Prelease clean deploy $GPG_PASSPHRASE

prompt_continue "tag and push tags for version $RELEASE_VERSION to remote git repository"

git tag $RELEASE_VERSION
git push --tags

prompt_continue "set version $SNAPSHOT_VERSION to local git repository"

mvn versions:set -DnewVersion=$SNAPSHOT_VERSION
git commit -am "prepare for release $SNAPSHOT_VERSION"

prompt_continue "push version $SNAPSHOT_VERSION to remote git repository"

git push

prompt_continue "update JavaDoc and REST API documentation ($RELEASE_VERSION) in gh-pages to local git repository"

git checkout $RELEASE_VERSION
mvn clean install -DskipTests=true
mvn site javadoc:aggregate
git checkout gh-pages
git pull --rebase

mv target/site/apidocs apidocs/v$RELEASE_VERSION
sed -i "2s/.*/redirect_to: \/apidocs\/v$RELEASE_VERSION\//" apidocs/current/index.html
git add apidocs/current/index.html apidocs/v$RELEASE_VERSION
git commit -m "updated javadocs for version $RELEASE_VERSION"

mv nflow-tests/target/rest-api-docs rest-apidocs/v$RELEASE_VERSION
sed -i "2s/.*/redirect_to: \/rest-apidocs\/v$RELEASE_VERSION\//" rest-apidocs/current/index.html
git add rest-apidocs/current/index.html rest-apidocs/v$RELEASE_VERSION
git commit -m "updated REST API documentation for version $RELEASE_VERSION"

prompt_continue "push JavaDoc and REST API documentation ($RELEASE_VERSION) in gh-pages to remote git repository"

git push
git checkout master

prompt_continue "prepare CHANGELOG.md for $SNAPSHOT_VERSION"

CURRENT_DATE=$(date +%F)
sed -i "1s/.*/## $RELEASE_VERSION ($CURRENT_DATE)/" CHANGELOG.md
sed -i "1s/^/## $SNAPSHOT_VERSION (future release)\n\n**Highlights**\n\n**Details**\n\n/" CHANGELOG.md

git commit -am "prepare changelog for $SNAPSHOT_VERSION"
git push
