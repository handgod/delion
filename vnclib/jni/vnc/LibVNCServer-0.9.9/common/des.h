#ifndef _DES_H_
#define _DES_H_
#endif
void initDesKey(char* k);
int desEncrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut);
int desDecrypt(unsigned char* dataBuf, int data_len, unsigned char** pOut);