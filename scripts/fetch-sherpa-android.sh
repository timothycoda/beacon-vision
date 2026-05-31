#!/usr/bin/env bash
# Downloads sherpa-onnx v1.12.9 Android JNI libs (arm64-v8a only) into :sherpa module.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/sherpa/src/main/jniLibs/arm64-v8a"
VERSION="v1.12.9"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/${VERSION}/sherpa-onnx-${VERSION}-android.tar.bz2"

mkdir -p "$DEST"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

echo "Downloading $URL ..."
curl -fsSL "$URL" -o "$tmpdir/sherpa.tar.bz2"
tar -xjf "$tmpdir/sherpa.tar.bz2" -C "$tmpdir" ./jniLibs/arm64-v8a
cp "$tmpdir/jniLibs/arm64-v8a"/*.so "$DEST/"
echo "Installed JNI libs to $DEST"
ls -lh "$DEST"
