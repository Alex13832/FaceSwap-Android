#!/bin/bash

# Compile lib
./../../Android/Sdk/ndk-bundle/ndk-build

# Move files to libs
# ---------------------------------------------------------------------------

cp libs/arm64-v8a/libnativefaceswap.so dlib/src/main/jniLibs/arm64-v8a/

cp libs/armeabi-v7a/libnativefaceswap.so dlib/src/main/jniLibs/armeabi-v7a/

cp libs/x86/libnativefaceswap.so dlib/src/main/jniLibs/x86/

cp libs/x86_64/libnativefaceswap.so dlib/src/main/jniLibs/x86_64/
