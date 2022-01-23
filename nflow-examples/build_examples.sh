#!/bin/bash

cd spring-boot/bare-minimum/maven

if mvn install; then
  echo "Spring Boot bare minimum Maven builds ok"
else
  echo "Spring Boot bare minimum Maven build failed"
  exit 1
fi

cd ../gradle

if ./gradlew build; then
  echo "Spring Boot bare minimum Gradle builds ok"
else
  echo "Spring Boot bare minimum Gradle build failed"
  exit 1
fi

cd ../../full-stack/maven

if mvn install; then
  echo "Spring Boot full stack Maven builds ok"
else
  echo "Spring Boot full stack Maven build failed"
  exit 1
fi

cd ../gradle

if ./gradlew build; then
  echo "Spring Boot full stack Gradle builds ok"
else
  echo "Spring Boot full stack Gradle build failed"
  exit 1
fi

cd ../../full-stack-kotlin

if ./gradlew build; then
  echo "Spring Boot Kotlin Gradle builds ok"
else
  echo "Spring Boot Kotlin Gradle build failed"
  exit 1
fi
