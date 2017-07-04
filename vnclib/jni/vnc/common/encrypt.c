#include "encrypt.h"
#include "string.h"

char genXorBase(const char *pBase){
	int i = 0;
	int len = strlen(pBase);
	len = len > 4 ? 4 : len;
	char base = pBase[0];
	for( i = 1; i < len; i++){
		base = base ^ pBase[i];
	}
	return base;
}
void xorData(char* buf, int offset, int len, unsigned char base){
	int i = 0;
	for( i = 0; i < len; i++){
		buf[offset + i] = buf[offset + i] ^ base;
	}
}