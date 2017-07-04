
#include <jni.h>
#include <string.h>
#include "string_util.h"
char *jstringTostring(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass classString = (*env)->FindClass(env, "java/lang/String");
    jstring strenCode = (*env)->NewStringUTF(env, "utf-8");
    jmethodID mid = (*env)->GetMethodID(env, classString, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) (*env)->CallObjectMethod(env, jstr, mid, strenCode);
    jsize alen = (*env)->GetArrayLength(env, barr);
    jbyte *ba = (*env)->GetByteArrayElements(env, barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    (*env)->ReleaseByteArrayElements(env, barr, ba, 0);
    return rtn;
}

jstring strToJstring(JNIEnv *env, const char *pStr) {
    int strLen = strlen(pStr);
    jclass jstrObj = (*env)->FindClass(env, "java/lang/String");
    jmethodID methodId = (*env)->GetMethodID(env, jstrObj, "<init>", "([BLjava/lang/String;)V");
    jbyteArray byteArray = (*env)->NewByteArray(env, strLen);
    jstring encode = (*env)->NewStringUTF(env, "utf-8");

    (*env)->SetByteArrayRegion(env, byteArray, 0, strLen, (jbyte *) pStr);

    return (jstring) (*env)->NewObject(env, jstrObj, methodId, byteArray, encode);
}