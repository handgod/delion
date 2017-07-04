#ifndef _RSA_H_
#define _RSA_H_
#include<openssl/rsa.h>
#include<openssl/pem.h>
#include<openssl/err.h>
int rsaInit();
void rsaUninit();
int initPubKey();
int initPrvKey();
char *rsaEncrypt_pubKey(char *str);
int getRsaEncryptSize();
char *rsaDecrypt_pubKey(char *str);
int getRsaDecryptSize();
#endif
