# Build libmp3lame as a shared library.

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := mp3lame
LOCAL_SRC_FILES := \
	bitstream.cpp \
	encoder.cpp \
	fft.cpp \
	gain_analysis.cpp \
	id3tag.cpp \
	lame.cpp \
	mpglib_interface.cpp \
	newmdct.cpp \
	presets.cpp \
	psymodel.cpp \
	quantize.cpp \
	quantize_pvt.cpp \
	reservoir.cpp \
	set_get.cpp \
	tables.cpp \
	takehiro.cpp \
	util.cpp \
	vbrquantize.cpp \
	version.cpp \
	xmm_quantize_sub.cpp \
	libmp3lame.cpp
			
LOCAL_LDLIBS := -llog -lGLESv1_CM
LOCAL_CFLAGS := -DHAVE_CONFIG_H

include $(BUILD_SHARED_LIBRARY)



