#ifndef VNC_JNI_H
#define VNC_JNI_H
#include "common.h"
#include "string_util.h"
#include "jni.h"
#include "log.h"

JavaVM *g_pVM;


jmethodID MethodID_ScreenCap;
jmethodID MethodID_ScreenFormat;
jmethodID MethodID_RunShell;
jmethodID MethodID_Logcat;

unsigned char* jni_screenCap();
unsigned char* jni_screenFormat();


#endif
