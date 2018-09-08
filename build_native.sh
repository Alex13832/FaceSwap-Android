#!/bin/bash

# Compile lib
./../../Android/Sdk/ndk-bundle/ndk-build

# Move files to libs
# ---------------------------------------------------------------------------
rm dlib/src/main/jniLibs/arm64-v8a/libnativefaceswap.so

cp libs/arm64-v8a/libnativefaceswap.so dlib/src/main/jniLibs/arm64-v8a/

rm dlib/src/main/jniLibs/armeabi-v7a/libnativefaceswap.so

cp libs/armeabi-v7a/libnativefaceswap.so dlib/src/main/jniLibs/armeabi-v7a/

rm dlib/src/main/jniLibs/x86/libnativefaceswap.so

cp libs/x86/libnativefaceswap.so dlib/src/main/jniLibs/x86/

rm dlib/src/main/jniLibs/x86_64/libnativefaceswap.so

cp libs/x86_64/libnativefaceswap.so dlib/src/main/jniLibs/x86_64/
