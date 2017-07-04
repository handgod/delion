#ifndef SCREENCAP_METHOD
#define SCREENCAP_METHOD

#include "common.h"


int initSC(void);
void closeSC(void);
int getScreenFormat();
void *recvSCData(void *param);
unsigned int *readBufferSC(void);
#endif

