#include "rsa.h"
#include<stdio.h>
#include<stdlib.h>
#include<string.h>

RSA *p_rsaPub;

/**
 * 初始化RSA密钥
 */
int rsaInit(){
	if(!initPubKey()){
		return 0;
	}
	return -1;
}
/**
 * 释放RSA密钥
 */
void rsaUninit(){

	if(p_rsaPub != NULL){
		RSA_free(p_rsaPub);
	}
}

/**
 * 释放RSA公钥
 */
int initPubKey(){
	char *p_en;
    int flen,rsa_len;
	BIO *mem = BIO_new(BIO_s_mem());
	BIO_puts(mem, "-----BEGIN PUBLIC KEY-----\n");
	BIO_puts(mem, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDMf4x/PjDHNz4zhsyUQXGrpLvb\n");
	BIO_puts(mem, "NYbRbZMy9i6mWczNOg52tsFMiqElSmx+1E/q6pmjNG5PZwuzlFLfGyZl05X01tkq\n");
	BIO_puts(mem, "KeHnoQ0Ve5K4onuIwsY8D1XcfukHCHbLQHb7j3KJrjq0lkF1LuJNrgG98kuaCAZC\n");
	BIO_puts(mem, "d0/ikx5rfoAYPgMDZQIDAQAB\n");
	BIO_puts(mem, "-----END PUBLIC KEY-----\n");
    if((p_rsaPub = PEM_read_bio_RSA_PUBKEY(mem,NULL,NULL,NULL))==NULL){
        ERR_print_errors_fp(stdout);
		return -1;
    }
	BIO_free(mem);
	return 0;
}

/**
 * RSA公钥加密
 */
char *rsaEncrypt_pubKey(char *str){
    char *p_en;
    RSA *p_rsa;
    int flen,rsa_len;
    flen=strlen(str);
    rsa_len=RSA_size(p_rsaPub);
    p_en=(char *)malloc(rsa_len+1);
    memset(p_en,0,rsa_len+1);
    if(RSA_public_encrypt(rsa_len,(unsigned char *)str,(unsigned char*)p_en,p_rsaPub,RSA_NO_PADDING)<0){
        return NULL;
    }
    return p_en;
}

int getRsaEncryptSize(){
	return RSA_size(p_rsaPub) + 1;
}
/**
 * RSA公钥解密
 */
char *rsaDecrypt_pubKey(char *str){
    char *p_de;
    RSA *p_rsa;
    int rsa_len;
    rsa_len=RSA_size(p_rsaPub);
    p_de=(char *)malloc(rsa_len+1);
    memset(p_de,0,rsa_len+1);
    if(RSA_public_decrypt(rsa_len,(unsigned char *)str,(unsigned char*)p_de,p_rsaPub,RSA_NO_PADDING)<0){
        return NULL;
    }
    return p_de;
}

int getRsaDecryptSize(){
	return RSA_size(p_rsaPub) + 1;
}