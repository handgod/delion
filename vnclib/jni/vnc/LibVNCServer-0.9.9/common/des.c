#include <stdio.h> 
#include <stdlib.h> 
#include <string.h> 
#include <openssl/des.h> 

/************************************************************************ 
** �������ã� 
** 3des-ecb���ܷ�ʽ�� 
** 24λ��Կ������24λ���Ҳ�0x00�� 
** ��������8λ���룬���뷽ʽΪ����1λ��һ��0x01,��2λ������0x02,... 
** ������8λ����ģ����油�˸�0x08�� 
************************************************************************/ 
#define LEN_OF_KEY 24 
DES_key_schedule ks,ks2,ks3; 

void initDesKey(char* k){
	 int key_len; 
    #define LEN_OF_KEY 24 
    unsigned char key[LEN_OF_KEY]; /* ��������Կ */ 
    unsigned char block_key[9]; 
    DES_key_schedule ks,ks2,ks3; 
    /* ���첹������Կ */ 
    key_len = strlen(k); 
    memcpy(key, k, key_len); 
    memset(key + key_len, 0x00, LEN_OF_KEY - key_len);
	/* ��Կ�û� */ 
    memset(block_key, 0, sizeof(block_key)); 
    memcpy(block_key, key + 0, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks); 
    memcpy(block_key, key + 8, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks2); 
    memcpy(block_key, key + 16, 8); 
    DES_set_key_unchecked((const_DES_cblock*)block_key, &ks3); 
}

/************************************************************************  
** ������ܺ�ĵ�ַ�ͳ��ȣ����ȴ���0��ʾ���ܳɹ���
** ʹ��������Ҫ�ͷ�pOut��ռ�õĿռ䡣
************************************************************************/ 
int desEncrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut){
	/* ����������������ռ估����������� */ 
    int data_rest = data_len % 8; 
    int len = data_len + (8 - data_rest); 
    unsigned char ch = 8 - data_rest; 
    *pOut = (unsigned char *)malloc(len); 
    int count; 
    int i;
    /* ���첹���ļ������� */ 
    memset(*pOut, 0, len);
    memcpy(*pOut, dataBuf, data_len); 
    memset(*pOut + data_len, ch, 8 - data_rest); 
	/* ѭ������/���ܣ�ÿ8�ֽ�һ�� */ 
    unsigned char out[8]; 
	unsigned char tmp[8]; 
    count = len / 8; 
    for (i = 0; i < count; i++) 
    {
        memset(out, 0, 8); 
        memcpy(tmp, dataBuf + 8 * i, 8); 
        /* ���� */ 
        DES_ecb3_encrypt((const_DES_cblock*)tmp, (DES_cblock*)out, &ks, &ks2, &ks3, DES_ENCRYPT); 
        memcpy(*pOut + 8 * i, out, 8); 
    }
	return len;
}

int desDecrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut){
	/* 8�ֽڶ������ݽ��н��� */ 
    int data_rest = data_len % 8; 
    int len = data_len - data_rest; 
    *pOut = (unsigned char *)malloc(len);
    int count; 
    int i;
	/* ѭ�����ܣ�ÿ8�ֽ�һ�� */ 
    unsigned char out[8]; 
	unsigned char tmp[8]; 
    count = len / 8; 
    for (i = 0; i < count; i++) 
    {
        memset(out, 0, 8); 
        memcpy(tmp, dataBuf + 8 * i, 8); 
        /* ���� */ 
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