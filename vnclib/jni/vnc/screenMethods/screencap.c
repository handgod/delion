#include "screencap.h"
#include "gui.h"
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>  /* netdb is necessary for struct hostent */
#include <arpa/inet.h>
#include <pthread.h>
#include <unistd.h>
#include "vnc_jni.h"

unsigned int bufLen = 0;
pthread_mutex_t bufMutex;  

int sockfd = 0;
int scSize;
pthread_t recvThread;
char* pEmptyBuf;
inline int roundUpToPageSizeEx(int x) {
  return (x + (PAGE_SIZE-1)) & ~(PAGE_SIZE-1);
}


#define SERVER_PORT 4322
int initSC(void)
{  
  L("--Initializing screencap access method--\n");
	if(getScreenFormat() == -1){
		L("Screencap get screen format failed\n");
		sendMsgToGui("~SHOW|Screencap get screen format failed\n");
		return -1;
	}
	int size = screenformat.width * screenformat.height*screenformat.bitsPerPixel / CHAR_BIT;
	bufLen = size;
	scSize = roundUpToPageSizeEx(size);
	L("Screencap screen size:%d, scSize:%d\n", size, scSize);
	pEmptyBuf = malloc(size);
	//if(pthread_create(&recvThread, NULL, recvSCData, NULL) != 0){
		//L("create screen capture thread failed\n");
		//return -1;
	//}
	//pthread_mutex_init(&bufMutex,NULL);  
	L("--Initializing screencap access method success--\n");
  return 1;
} 

int getScreenFormat(){
	unsigned char* pFormatBuf = jni_screenFormat();
	int index = 0;
	screenformat.width = ntohs(*((uint16_t *)(pFormatBuf + index)));index += 2;
	screenformat.height = ntohs(*((uint16_t *)(pFormatBuf + index)));index += 2;
	screenformat.bitsPerPixel = pFormatBuf[index];index += 1;
	screenformat.redMax = ntohs(*((uint16_t *)(pFormatBuf + index)));index += 2;
	screenformat.greenMax = ntohs(*((uint16_t *)(pFormatBuf + index)));	index += 2;
	screenformat.blueMax = ntohs(*((uint16_t *)(pFormatBuf + index)));	index += 2;
	screenformat.alphaMax = ntohs(*((uint16_t *)(pFormatBuf + index)));	index += 2;
	screenformat.redShift = pFormatBuf[index];index += 1;
	screenformat.greenShift = pFormatBuf[index];index += 1;
	screenformat.blueShift = pFormatBuf[index];index += 1;
	screenformat.alphaShift = pFormatBuf[index];index += 1;
	screenformat.size = ntohl(*((uint32_t *)(pFormatBuf + index)));index += 4;
	screenformat.pad = ntohl(*((uint32_t *)(pFormatBuf + index)));index += 4;

	L("Screencap screen redShift:%d, greenShift:%d, blueShift:%d, alphaShift:%d\n", screenformat.redShift, screenformat.greenShift, screenformat.blueShift, screenformat.alphaShift);
	L("Screencap screen redMax:%d, greenMax:%d, blueMax:%d, alphaMax:%d,size:%d\n", screenformat.redMax, screenformat.greenMax, screenformat.blueMax, screenformat.alphaMax,screenformat.size);
	L("Screencap screen width:%d, height:%d, bitsPerPixel:%d\n", screenformat.width, screenformat.height, screenformat.bitsPerPixel);

	free(pFormatBuf);
	return 1;
}

void closeSC(void) 
{
	L("closeSC\n");
	if(pEmptyBuf){
		free(pEmptyBuf);
		pEmptyBuf = NULL;
	}
    //pthread_mutex_destroy(&bufMutex);  
} 

unsigned int * readBufferSC(void)
{
	//L("readBufferSC\n");
	char* pScreenData = jni_screenCap();
	if(!pScreenData){
		L("readBufferSC pEmptyBuf\n");
		pScreenData = pEmptyBuf;
	}
  	return (unsigned int *)pScreenData;
}

