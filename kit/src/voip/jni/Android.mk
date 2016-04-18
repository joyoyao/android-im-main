LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)     
LOCAL_MODULE    := libVoip
ifeq ($(TARGET_ARCH),x86)
LOCAL_SRC_FILES := libVoip_x86.a
else
LOCAL_SRC_FILES := libVoip.a
endif
include $(PREBUILT_STATIC_LIBRARY) 

include $(CLEAR_VARS)
LOCAL_STATIC_LIBRARIES := libVoip 
LOCAL_MODULE    := RongIMVoip
LOCAL_SRC_FILES := RongIMVoip.cpp
LOCAL_LDLIBS := -llog  -lz
include $(BUILD_SHARED_LIBRARY)
