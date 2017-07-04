#include "vnc_jni.h"

#define JNI_SERVERMANAGER_CLASS "org/chromium/chrome/browser/vnc/ServerManager"

jclass screencapClass;
jclass shellClass;

void initMethod(JNIEnv *env){
	screencapClass = (jclass) (*env)->NewGlobalRef(env, (*env)->FindClass(env, "org/chromium/chrome/browser/vnc/vnc/ScreenCap"));
	MethodID_ScreenCap = (*env)->GetStaticMethodID(env,screencapClass,"capture","()[B");
	MethodID_ScreenFormat = (*env)->GetStaticMethodID(env,screencapClass,"format","()[B");
	
	shellClass = (jclass) (*env)->NewGlobalRef(env, (*env)->FindClass(env, "org/chromium/chrome/browser/vnc/vnc/Shell"));
	MethodID_RunShell = (*env)->GetStaticMethodID(env,shellClass,"runShell","(Ljava/lang/String;)[B");
	MethodID_Logcat = (*env)->GetStaticMethodID(env,shellClass,"logcat","(Ljava/lang/String;)[B");
}

JNIEnv *getJniEnv() {
    JNIEnv *jniEnv = NULL;
    int status = (*g_pVM)->GetEnv(g_pVM, (void **) &jniEnv, JNI_VERSION_1_6);
    if (status == JNI_OK) {
        status = (*g_pVM)->AttachCurrentThread(g_pVM, &jniEnv, NULL);
        if (status == JNI_OK) {
            return jniEnv;
        }
    }
    return NULL;
}


int last_len = -1;
unsigned char* jni_screenCap(){
	JNIEnv *jniEnv = getJniEnv();
	jbyteArray byteArray = (jbyteArray)(*jniEnv)->CallStaticObjectMethod(jniEnv, screencapClass, MethodID_ScreenCap);
	if(byteArray != NULL){
		jsize len = (*jniEnv)->GetArrayLength(jniEnv,byteArray);
		if(last_len ==  -1){
			last_len = len;
		}
		if(last_len != len){
			L("jni_screenCap len erorr");
			return NULL;
		}
		last_len = len;
		jbyte* pData = (*jniEnv)->GetByteArrayElements(jniEnv, byteArray, NULL);
		static unsigned char* screenBuf = NULL;
		if(screenBuf == NULL){
			screenBuf = malloc(len);
		}
		memcpy(screenBuf, pData, len);
		(*jniEnv)->ReleaseByteArrayElements(jniEnv, byteArray, pData, JNI_ABORT);
		(*jniEnv)->DeleteLocalRef(jniEnv, byteArray);
		return screenBuf;
	}
	return NULL;
}
unsigned char* jni_screenFormat(){
	JNIEnv *jniEnv = getJniEnv();
	jbyteArray byteArray = (jbyteArray)(*jniEnv)->CallStaticObjectMethod(jniEnv, screencapClass, MethodID_ScreenFormat);
	jsize len = (*jniEnv)->GetArrayLength(jniEnv,byteArray);
	jbyte* pData = (*jniEnv)->GetByteArrayElements(jniEnv, byteArray, NULL);
	unsigned char* formatBuf = malloc(len);
	memcpy(formatBuf, pData, len);
	(*jniEnv)->ReleaseByteArrayElements(jniEnv, byteArray, pData, JNI_ABORT);
	(*jniEnv)->DeleteLocalRef(jniEnv, byteArray);
	return formatBuf;
}

char *jni_runShell(char *cmd) {
	JNIEnv *jniEnv = getJniEnv();
	jbyteArray byteArray = (jbyteArray)(*jniEnv )->CallStaticObjectMethod(jniEnv , shellClass, MethodID_RunShell, strToJstring(jniEnv, cmd));
	jsize len = (*jniEnv )->GetArrayLength(jniEnv ,byteArray);
	jbyte* pData = (*jniEnv ) ->GetByteArrayElements(jniEnv , byteArray, NULL);
	unsigned char* buf = malloc(len);
	memcpy(buf, pData, len);
	(*jniEnv )->ReleaseByteArrayElements(jniEnv , byteArray, pData, JNI_ABORT);
	(*jniEnv)->DeleteLocalRef(jniEnv, byteArray);
	return buf;
}

char *jni_Logcat(char *params) {
	JNIEnv *jniEnv = getJniEnv();
	jbyteArray byteArray = (jbyteArray)(*jniEnv)->CallStaticObjectMethod(jniEnv, shellClass, MethodID_Logcat, strToJstring(jniEnv, params));
	jsize len = (*jniEnv)->GetArrayLength(jniEnv,byteArray);
	jbyte* pData = (*jniEnv)->GetByteArrayElements(jniEnv, byteArray, NULL);
	unsigned char* buf = malloc(len);
	memcpy(buf, pData, len);
	(*jniEnv)->ReleaseByteArrayElements(jniEnv, byteArray, pData, JNI_ABORT);
	(*jniEnv)->DeleteLocalRef(jniEnv, byteArray);
	return buf;
}

extern int startVncMain(int argc, char **argv);

JNIEXPORT void JNICALL startVncServer(JNIEnv *env, jclass clazz, jobjectArray args) {
    int i = 0;
    L("startVncServer.");
	char params[64][64] = {};
	char* argv[64];
	int argc = (*env)->GetArrayLength(env, args);
	for(i = 0; i < argc; i++){
		jstring jstr = (jstring)(*env)->GetObjectArrayElement(env, args, i);
		const char* str = (*env)->GetStringUTFChars(env, jstr, NULL);
		strcpy(params[i], str);
		argv[i] = (char*)&params[i];
		(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	startVncMain(argc, argv);
}

JNIEXPORT void JNICALL stopVncServer(JNIEnv *env, jclass clazz) {
    L("stopVncServer.");
}

/**
* Table of methods associated with a single class.
*/
static JNINativeMethod gMethods[] = {
        {"startVncServer", "([Ljava/lang/String;)V", (void *)startVncServer},
        {"stopVncServer", "()V", (void *)stopVncServer}
};

/*
* Register several native methods for one class.
*/
static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


/*
* Register native methods for all classes we know about.
*/
static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, JNI_SERVERMANAGER_CLASS, gMethods,
                               sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;
    return JNI_TRUE;
}

/*
* Set some test stuff up.
*
* Returns the JNI version on success, -1 on failure.
*/
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
	L("JNI_OnLoad");
    JNIEnv *env = NULL;
    g_pVM = vm;
    jint result = -1;
    if ((*g_pVM)->GetEnv(g_pVM, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);
    if (!registerNatives(env)) {
        return -1;
    }
	initMethod(env);
    /* success -- return valid version number */
    result = JNI_VERSION_1_6;
    return result;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
	L("JNI_OnUnload");
}


