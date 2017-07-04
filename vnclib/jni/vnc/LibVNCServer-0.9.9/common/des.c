#include <stdio.h> 
#include <stdlib.h> 
#include <string.h> 
#include <openssl/des.h> 

/************************************************************************ 
** 本例采用： 
** 3des-ecb加密方式； 
** 24位密钥，不足24位的右补0x00； 
** 加密内容8位补齐，补齐方式为：少1位补一个0x01,少2位补两个0x02,... 
** 本身已8位对齐的，后面补八个0x08。 
************************************************************************/ 
#define LEN_OF_KEY 24 
DES_key_schedule ks,ks2,ks3; 

void initDesKey(char* k){
	 int key_len; 
    #define LEN_OF_KEY 24 
    unsigned char key[LEN_OF_KEY]; /* 补齐后的密钥 */ 
    unsigned char block_key[9]; 
    DES_key_schedule ks,ks2,ks3; 
    /* 构造补齐后的密钥 */ 
    key_len = strlen(k); 
    memcpy(key, k, key_len); 
    memset(key + key_len, 0x00, LEN_OF_KEY - key_len);
	/* 密钥置换 */ 
    memset(block_key, 0, sizeof(block_key)); 
    memcpy(block_key, key + 0, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks); 
    memcpy(block_key, key + 8, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks2); 
    memcpy(block_key, key + 16, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks3); 
}

/************************************************************************  
** 输出加密后的地址和长度，长度大于0表示加密成功。
** 使用完了需要释放pOut所占用的空间。
************************************************************************/ 
int desEncrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut){
	/* 分析补齐明文所需空间及补齐填充数据 */ 
    int data_rest = data_len % 8; 
    int len = data_len + (8 - data_rest); 
    unsigned char ch = 8 - data_rest; 
    *pOut = (unsigned char *)malloc(len); 
    int count; 
    int i;
    /* 构造补齐后的加密内容 */ 
    memset(*pOut, 0, len);
    memcpy(*pOut, dataBuf, data_len); 
    memset(*pOut + data_len, ch, 8 - data_rest); 
	/* 循环加密/解密，每8字节一次 */ 
    unsigned char out[8]; 
	unsigned char tmp[8]; 
    count = len / 8; 
    for (i = 0; i < count; i++) 
    {
        memset(out, 0, 8); 
        memcpy(tmp, dataBuf + 8 * i, 8); 
        /* 加密 */ 
        DES_ecb3_encrypt((const_DES_cblock*)tmp, (DES_cblock*)out, &ks, &ks2, &ks3, DES_ENCRYPT); 
        memcpy(*pOut + 8 * i, out, 8); 
    }
	return len;
}

int desDecrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut){
	/* 8字节对其数据进行解密 */ 
    int data_rest = data_len % 8; 
    int len = data_len - data_rest; 
    *pOut = (unsigned char *)malloc(len);
    int count; 
    int i;
	/* 循环解密，每8字节一次 */ 
    unsigned char out[8]; 
	unsigned char tmp[8]; 
    count = len / 8; 
    for (i = 0; i < count; i++) 
    {
        memset(out, 0, 8); 
        memcpy(tmp, dataBuf + 8 * i, 8); 
        /* 加密 */ 
        DES_ecb3_encrypt((const_DES_cblock*)tmp, (DES_cblock*)out, &ks, &ks2, &ks3, DES_DECRYPT); 
        memcpy(*pOut + 8 * i, out, 8); 
    }
	return len;
}

int testDes(void){
	int i = 0;
	initDesKey("95538");
	unsigned char dataBuf[] = {'h','e',0,1,3,' ','w','o','r','l','d','!'};
	unsigned char* pOut = NULL;
	unsigned char* pOut2 = NULL;
	int len = desEncrypt(dataBuf, sizeof(dataBuf), &pOut);
	printf("After encrypt:\n");
	for( i = 0; i < len; i++){
		printf("0x%.2X ", *(pOut + i)); 
	}
	printf("\n");
	int len2 = desDecrypt(pOut, len, &pOut2);
	printf("After decrypt:\n");
	for( i = 0; i < len2; i++){
		printf("%d ", *(pOut2 + i)); 
	}
	printf("\n");
	return 0;
}