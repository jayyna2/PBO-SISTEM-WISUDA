#!/bin/bash
set -e

echo "=== Compiling Java sources ==="
mkdir -p bin
find src -name "*.java" | xargs javac -cp "lib/*" -d bin

echo "=== Starting Wisuda Server ==="
java -cp "bin:lib/*" model.WisudaApp
