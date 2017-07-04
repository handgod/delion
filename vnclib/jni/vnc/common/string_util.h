#ifndef CALLJAVATEST_COMMON_H
#define CALLJAVATEST_COMMON_H
#include "jni.h"
char* jstringTostring(JNIEnv* env, jstring jstr);
jstring strToJstring(JNIEnv* env, const char* pStr);
#endif //CALLJAVATEST_COMMON_H
