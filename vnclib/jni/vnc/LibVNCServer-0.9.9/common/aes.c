#include "aes.h"
#include <stdio.h>  
#include <string.h>  
#include <stdlib.h>  
#include <openssl/aes.h>  
#include <time.h>
#include<math.h>

char * userKey64char = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

const char ivec[16] = {'2','3','d','i','k','m','h','6','3','4','s','d','f','v','g','h'};

void getUserKeyRandom(char* userKey, int len){
	int i = 0;
	srand((unsigned) time(NULL));
	for (i = 0; i < len; i++)
	{
		int index = rand() % strlen(userKey64char);
		userKey[i] = userKey64char[index];
	}
}

void initIvec(char* outIvec){
	int i =0;
	for(i = 0; i < sizeof(ivec); i++){
		outIvec[i] = ivec[i];
	}
}
void getRandomInt(char* intBuf, int len){
	srand((unsigned) time(NULL));
	int mode = pow(10, len);
	int ret = rand() % mode;
	sprintf(intBuf,"%d", ret);
}