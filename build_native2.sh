#!/bin/bash

# Compile lib
./../../Android/Sdk/ndk-bundle/ndk-build

# Move files to libs
# ---------------------------------------------------------------------------
rm app/src/main/jniLibs/arm64-v8a/libnativefaceswap.so

cp libs/arm64-v8a/libnativefaceswap.so app/src/main/jniLibs/arm64-v8a/

rm app/src/main/jniLibs/armeabi-v7a/libnativefaceswap.so

cp libs/armeabi-v7a/libnativefaceswap.so app/src/main/jniLibs/armeabi-v7a/

rm app/src/main/jniLibs/x86/libnativefaceswap.so

cp libs/x86/libnativefaceswap.so app/src/main/jniLibs/x86/

rm app/src/main/jniLibs/x86_64/libnativefaceswap.so

cp libs/x86_64/libnativefaceswap.so app/src/main/jniLibs/x86_64/

rm app/src/main/jniLibs/armeabi/libnativefaceswap.so

cp libs/armeabi/libnativefaceswap.so app/src/main/jniLibs/armeabi/

rm app/src/main/jniLibs/mips/libnativefaceswap.so

cp libs/mips/libnativefaceswap.so app/src/main/jniLibs/mips/

rm app/src/main/jniLibs/mips64/libnativefaceswap.so

cp libs/mips/libnativefaceswap.so app/src/main/jniLibs/mips64/


