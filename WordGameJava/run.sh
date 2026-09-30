#!/usr/bin/env bash
# Compile and run the Guess the Word game (macOS / Linux).
# Requires a JDK (javac + java), version 17 or newer. No other dependencies.
set -e
cd "$(dirname "$0")"

echo "Compiling..."
mkdir -p out
javac -d out $(find src -name '*.java')

echo "Starting server..."
java -cp out wordgame.App
