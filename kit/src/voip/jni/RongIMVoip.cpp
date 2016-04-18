#include <jni.h>
#include <string.h>
#include <stdio.h>

#include "Voip.h"
#undef printf
#include <android/log.h>

#define printf(...) __android_log_print(ANDROID_LOG_DEBUG,"--HYJ--",__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

static JavaVM* g_jvm = 0;
jint JNI_OnLoad(JavaVM* vm, void *reserved) {
	printf("JNI_OnLoad().");
	JNIEnv *env;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return -1;
	}
	return JNI_VERSION_1_4;
}
JNIEXPORT void Java_io_rong_voiplib_NativeObject_setJNIEnv(JNIEnv* env, jobject obj, jobject objOnPublish) {
	printf("--jni-- setJNIEnv().");
	env->GetJavaVM(&g_jvm);
}

class CJavaEnv {
public:
	operator JNIEnv*() {
		return env;
	}
	CJavaEnv() : env(0), m_bAlreadyAttach(false) {
		printf("CJavaEnv()");
		if (g_jvm == 0)
			printf("====== Not Call setJNIEnv =======\n");
		if (g_jvm) {
			if (g_jvm->GetEnv((void **) &env, JNI_VERSION_1_4) == JNI_OK)
				printf("---jni--- current thread already attach to javaVM \n"), m_bAlreadyAttach = true;
			else if (g_jvm->AttachCurrentThread(&env, NULL) != JNI_OK)
				printf("AttachCurrentThread() failed");
		}
	}
	~CJavaEnv() {
		printf("~CJavaEnv()");
		if (m_bAlreadyAttach == false) {
			if (g_jvm == 0)
				printf("====== Not Call setJNIEnv =======\n");

			if (g_jvm) {
				int check = g_jvm->GetEnv((void **) &env, JNI_VERSION_1_4);
				printf("---jni--- check %d \n", check);
				if (check != JNI_EDETACHED) {
					if (g_jvm->DetachCurrentThread() != JNI_OK)
						printf("DetachCurrentThread() failed");
				}

			}
		}
	}
private:
	JNIEnv *env;
	bool m_bAlreadyAttach;
};
class CAutoJString
{
public:
	CAutoJString(JNIEnv *env,jstring& jstr)
	{
		jboolean b_ret;
		m_cstr = env->GetStringUTFChars(jstr, &b_ret);
		m_jstr = &jstr;
		m_env = env;
	}
	operator const char*()
	{
		return m_cstr;
	}
	~CAutoJString()
	{
		m_env->ReleaseStringUTFChars(*m_jstr,m_cstr);
	}
private:
	const char* m_cstr;
	jstring* m_jstr;
	JNIEnv *m_env;

};

class CVoipInitCallback : public IVoipCallback
{
public:
	CVoipInitCallback(jobject obj) { m_obj = obj; }
	virtual ~CVoipInitCallback(){}
	virtual bool Callme(unsigned char* pbData, unsigned long nl)
	{
		CJavaEnv oEnv;
		JNIEnv *env = oEnv;

		char* pszCombin = strdup((char*)pbData);
		printf("Voip init callback: %s", pszCombin);

		jstring jstrSessionId, jstrIp, jstrTransferPort, jstrControlPort;
		char* pS = pszCombin;
		char* p = strchr(pS, '|');
		if (p)
		{
			*p = 0;
			jstrSessionId = env->NewStringUTF(pS);
			pS = p+1;
			p = strchr(pS, '|');
			if (p)
			{
				*p = 0;
				jstrIp = env->NewStringUTF(pS);
				pS = p+1;
				p = strchr(pS, '|');
				if (p)
				{
					*p = 0;
					jstrTransferPort = env->NewStringUTF(pS);
					pS = p+1;
					jstrControlPort = env->NewStringUTF(pS);
				}
			}
		}
		if (m_obj)
		{
			jclass cls = env->GetObjectClass((jobject) m_obj);
			if (cls != NULL) {
				jmethodID nMethodId = env->GetMethodID(cls, "OnSuccess", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
				if (nMethodId != NULL)
					env->CallVoidMethod((jobject) m_obj, nMethodId, jstrSessionId, jstrIp, jstrTransferPort, jstrControlPort);

				env->DeleteLocalRef(cls);
			}

			env->DeleteGlobalRef(m_obj);
			m_obj = 0;
		}
		if (jstrSessionId) env->DeleteLocalRef(jstrSessionId);
		if (jstrIp) env->DeleteLocalRef(jstrIp);
		if (jstrTransferPort) env->DeleteLocalRef(jstrTransferPort);
		if (jstrControlPort) env->DeleteLocalRef(jstrControlPort);
		return true;
	}
	virtual void Error(int eType, const char* pszDescription)
	{
		printf("voip %d - %s", eType, pszDescription);
		CJavaEnv oEnv;
		JNIEnv *env = oEnv;
		if (m_obj)
		{
			jclass cls = env->GetObjectClass((jobject) m_obj);
			if (cls != NULL) {
				jmethodID nMethodId = env->GetMethodID(cls, "OnError", "(ILjava/lang/String;)V");
				if (nMethodId != NULL) {
					jstring jstrDesc = env->NewStringUTF(pszDescription);
					env->CallVoidMethod((jobject) m_obj, nMethodId, (jint)eType, jstrDesc);
					env->DeleteLocalRef(jstrDesc);
				}

				env->DeleteLocalRef(cls);
			}
			env->DeleteGlobalRef(m_obj);
			m_obj = 0;
		}
	}
private:
	jobject m_obj;
};

JNIEXPORT void JNICALL Java_io_rong_voiplib_NativeObject_StartVoip
  (JNIEnv *env, jobject,  jstring jstrAppId, jstring jstrToken, jstring jstrFromId, jstring jstrToId, jint nLocalPort, jobject objListener)
{
	printf("-----Enter StartVoip-----");
	jobject obj = env->NewGlobalRef(objListener);
	CVoipInitCallback* pCb = new CVoipInitCallback(obj);
	StartVoip(CAutoJString(env, jstrAppId), CAutoJString(env, jstrToken), CAutoJString(env, jstrFromId), CAutoJString(env, jstrToId), (int)nLocalPort, pCb);
	printf("-----leave StartVoip-----");
}

class CVoipAcceptCallback : public IVoipCallback
{
public:
	CVoipAcceptCallback(jobject obj) { m_obj = obj; }
	virtual ~CVoipAcceptCallback(){}
	virtual bool Callme(unsigned char* pbData, unsigned long nl)
	{
		return true;
	}
	virtual void Error(int eType, const char* pszDescription)
	{
		printf("voip %d - %s", eType, pszDescription);
		CJavaEnv oEnv;
		JNIEnv *env = oEnv;
		if (m_obj)
		{
			jclass cls = env->GetObjectClass((jobject) m_obj);
			if (cls != NULL) {
				if (eType != 200) {
					jmethodID nMethodId = env->GetMethodID(cls, "OnError", "(ILjava/lang/String;)V");
					if (nMethodId != NULL) {
						jstring jstrDesc = env->NewStringUTF(pszDescription);
						env->CallVoidMethod((jobject) m_obj, nMethodId, (jint)eType, jstrDesc);
						env->DeleteLocalRef(jstrDesc);
					}
				} else {
					jmethodID nMethodId = env->GetMethodID(cls, "OnSuccess", "()V");
					if (nMethodId != NULL) {
						env->CallVoidMethod((jobject) m_obj, nMethodId);
					}
				}

				env->DeleteLocalRef(cls);
			}
			env->DeleteGlobalRef(m_obj);
			m_obj = 0;
		}
	}
private:
	jobject m_obj;
};

JNIEXPORT void JNICALL Java_io_rong_voiplib_NativeObject_AcceptVoip
  (JNIEnv *env, jobject,  jstring jstrAppId, jstring jstrSessionId, jstring jstrServerIp, jint nRemotTransferPort, jstring jstrUserId, jint nLocalPort, jint nRemoteControlPort, jobject objListener)
{
	printf("-----Enter AcceptVoip-----");
	jobject obj = env->NewGlobalRef(objListener);
	CVoipAcceptCallback* pCb = new CVoipAcceptCallback(obj);
	AcceptVoip(CAutoJString(env, jstrAppId), CAutoJString(env, jstrSessionId), CAutoJString(env, jstrUserId), CAutoJString(env, jstrServerIp), (int)nRemotTransferPort, (int)nLocalPort, (int)nRemoteControlPort, pCb);
	printf("-----leave AcceptVoip-----");
}

JNIEXPORT void JNICALL Java_io_rong_voiplib_NativeObject_EndVoip(JNIEnv *env, jobject, jstring jstrAppId, jstring jstrSessionId, jstring jstrUserId, jobject objListener)
{
	printf("-----Enter EndVoip-----");
	jobject obj = env->NewGlobalRef(objListener);
	CVoipAcceptCallback* pCb = new CVoipAcceptCallback(obj);
	EndVoip(CAutoJString(env, jstrAppId), CAutoJString(env, jstrSessionId), CAutoJString(env, jstrUserId), pCb);
	printf("-----leave EndVoip-----");
}



#ifdef __cplusplus
}
#endif
