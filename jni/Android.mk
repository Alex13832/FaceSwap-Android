LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
OPENCV_LIB_TYPE:=STATIC
include /home/alexander/Android/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := nativefaceswap
LOCAL_SRC_FILES := nativefaceswap.cpp
LOCAL_LDLIBS +=  -llog -ldl -landroid -latomic
LOCAL_CPPFLAGS := -O0 -g3 -std=c++11 -Wall -Wextra -fexceptions


include $(BUILD_SHARED_LIBRARY)
