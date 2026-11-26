#!/bin/bash
# Builds and runs the game. Usage: ./run.sh
set -e
cd "$(dirname "$0")"

# JavaFX SDK: set FX to its lib folder, or put/link the SDK at ./javafx-sdk
FX="${FX:-javafx-sdk/lib}"
if [ ! -f "$FX/javafx.controls.jar" ]; then
  echo "JavaFX SDK not found at '$FX'."
  echo "Download it from https://gluonhq.com/products/javafx/ and either:"
  echo "  ln -s /path/to/javafx-sdk-XX javafx-sdk      or"
  echo "  FX=/path/to/javafx-sdk-XX/lib ./run.sh"
  exit 1
fi

# JDK 17+: use JAVA_HOME if set, otherwise JDK 21 on macOS, otherwise whatever is on PATH
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
fi
if [ -n "$JAVA_HOME" ]; then
  JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
else
  JAVAC=javac; JAVA=java
fi

OUT=build
rm -rf "$OUT" && mkdir -p "$OUT"

# Compile everything except the JUnit tests
"$JAVAC" --module-path "$FX" --add-modules javafx.controls \
  -d "$OUT" $(find src -name "*.java" -not -path "src/tests/*")

# Copy images/css next to the classes (loaded as "views/xxx.jpg")
cp src/views/*.jpg src/views/*.png src/views/*.css "$OUT/views/"

# Run from project root so Heroes.csv is found
"$JAVA" --module-path "$FX" --add-modules javafx.controls \
  -cp "$OUT" views.view
