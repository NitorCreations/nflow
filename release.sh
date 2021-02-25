#!/bin/bash

RELEASE_DIR="$PWD"

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
  echo "For automated Github API release: place Github API token with repos-authorization to .github_api_token file"
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
git commit -am "release $RELEASE_VERSION [ci skip]"

prompt_continue "push version $RELEASE_VERSION to remote git repository"

git push

prompt_continue "release version $RELEASE_VERSION to Maven Central"

mvn -Prelease clean deploy $GPG_PASSPHRASE

prompt_continue "tag and push tags for version $RELEASE_VERSION to remote git repository"

git tag $RELEASE_VERSION
git push --tags

prompt_continue "set version $SNAPSHOT_VERSION to local git repository"

mvn versions:set -DnewVersion=$SNAPSHOT_VERSION
git commit -am "prepare for release $SNAPSHOT_VERSION [ci skip]"

prompt_continue "push version $SNAPSHOT_VERSION to remote git repository"

git push

prompt_continue "update JavaDoc and REST API documentation ($RELEASE_VERSION) in gh-pages to local git repository"

git checkout $RELEASE_VERSION
mvn clean install -DskipTests=true
mv .mvn/maven.config .mvn/maven.config.disabled
mvn site javadoc:aggregate
mv .mvn/maven.config.disabled .mvn/maven.config
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

if [[ -e .github_api_token ]]; then
  prompt_continue "Creating Github API release for $RELEASE_VERSION"
  RELEASE_DESC_START=$(grep -n "## $RELEASE_VERSION" CHANGELOG.md | cut -f1 -d:)
  RELEASE_DESC_END=$(grep -n "## $PREVIOUS_VERSION" CHANGELOG.md | cut -f1 -d:)
  RELEASE_DESC=$(cat CHANGELOG.md | awk -v desc_start="$RELEASE_DESC_START" -v desc_end="$RELEASE_DESC_END" 'NR > desc_start+1 && NR < desc_end-1 {printf "%s\\n", $0}')
  GITHUB_API_TOKEN=$(cat .github_api_token)
  curl -v -H "Authorization: token $GITHUB_API_TOKEN" -H "Content-Type: application/json" https://api.github.com/repos/NitorCreations/nflow/releases \
  --data @<(cat <<EOF
  {
    "tag_name": "$RELEASE_VERSION",
    "target_commitish": "master",
    "name": "v$RELEASE_VERSION",
    "body": "$RELEASE_DESC",
    "draft": false,
    "prerelease": false
  }
EOF
)
else
  echo "Github API token not found (.github_api_token -file), make Github release manually"
fi

NFLOW_WIKI_CHECKOUT_DIR="/tmp/nflow-release-tmp-$RANDOM"
prompt_continue "cloning nFlow Wiki under $NFLOW_WIKI_CHECKOUT_DIR for version number updates"

if mkdir -p "$NFLOW_WIKI_CHECKOUT_DIR" ; then
  cd "$NFLOW_WIKI_CHECKOUT_DIR"
  git clone ssh://git@github.com:NitorCreations/nflow.wiki.git
  cd nflow.wiki
  sed -i -e "s/$PREVIOUS_VERSION/$RELEASE_VERSION/g" Spring-Boot-guide.md
  git add Spring-Boot-guide.md
  git commit -m "updated version number to $RELEASE_VERSION in Spring-Boot-guide.md"
  git push
  cd $RELEASE_DIR
else
  echo "failed to update version number in nFlow Wiki pages - do it manually"
fi

prompt_continue "updating nflow-examples dependencies to new release and verifying that their build works"

EXAMPLE_DEPENDENCY_FILES=("bare-minimum/maven/pom.xml" "bare-minimum/gradle/build.gradle" "full-stack/maven/pom.xml" "full-stack/gradle/build.gradle" "full-stack-kotlin/gradle.properties")

cd nflow-examples

for dependency_file in "${EXAMPLE_DEPENDENCY_FILES[@]}"; do
  sed -i -e "s/$PREVIOUS_VERSION/$RELEASE_VERSION/g" "spring-boot/$dependency_file"
done

if ./build_examples.sh; then
  echo "changed nflow-examples files:"
  git --no-pager diff

  for dependency_file in "${EXAMPLE_DEPENDENCY_FILES[@]}"; do
    git add "spring-boot/$dependency_file"
  done
  git commit -m "updated nflow-examples for version $RELEASE_VERSION"

  prompt_continue "push version nflow-example to remote git repository"
  git push
else
  echo "failed to build nflow-examples - check manually what is wrong"
fi

cd "$RELEASE_DIR"

echo "*** RELEASE FINISHED ***"
