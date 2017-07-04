package org.chromium.chrome.browser.vnc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class InputEventListener extends Thread {
	DatagramSocket inputlister = null;
	boolean finished = false;
	Instrumentation mInst = new Instrumentation();
	
	public void finishThread() {
		finished = true;
	}
	
	public void processPointerInputEvent(float x_Position,float y_Position,int buttonMask,int inputevent_value) {	
		switch(inputevent_value)
		{
		 case 0: //ACTION_DOWN
			 if(buttonMask == 1) //left button
			 {
				 mInst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
			                SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
			                x_Position, y_Position, 0));		
			 }
			 else if(buttonMask == 4) //right button
			 {
				 //mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
			 }
			 else if(buttonMask == 2) //mid button
			 {
				 
			 }				
				
			 break;
			 
		 case 1: //ACTION_UP
			 if(buttonMask == 1) //left button
			 {						
				 mInst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
			                SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
			                x_Position, y_Position, 0));
			 }
			 else if(buttonMask == 4) //right button
			 {
				 mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
			 }
			 else if(buttonMask == 2) //mid button
			 {
				 
			 }
			 
			 break;
		 case 2: //ACTION_MOVE
			 if(buttonMask == 1) //left button
			 {
				 mInst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
			                SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
			                x_Position, y_Position, 0));		
			 }
			 else if(buttonMask == 4) //right button
			 {
				
			 }
			 else if(buttonMask == 8) //right button
			 {
			//	 mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ZOOM_IN);
			 }
			 else if(buttonMask == 16) //right button
			 {
			//	 mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ZOOM_OUT);
			 }
			 else if(buttonMask == 2) //mid button
			 {				
			
			 }				
						
			 break;	 			
		 
		 default:		 
				break;			
		}						
	}
	
	/*KEYCODE_UNKNOWN=0;
	KEYCODE_SOFT_LEFT=1;
	KEYCODE_SOFT_RIGHT=2;
	KEYCODE_HOME=3;
	KEYCODE_BACK=4;
	KEYCODE_CALL=5;
	KEYCODE_ENDCALL=6;
	KEYCODE_0=7;
	KEYCODE_1=8;
	KEYCODE_2=9;
	KEYCODE_3=10;
	KEYCODE_4=11;
	KEYCODE_5=12;
	KEYCODE_6=13;
	KEYCODE_7=14;
	KEYCODE_8=15;
	KEYCODE_9=16;
	KEYCODE_STAR=17;
	KEYCODE_POUND=18;
	KEYCODE_DPAD_UP=19;
	KEYCODE_DPAD_DOWN=20;
	KEYCODE_DPAD_LEFT=21;
	KEYCODE_DPAD_RIGHT=22;
	KEYCODE_DPAD_CENTER=23;
	KEYCODE_VOLUME_UP=24;
	KEYCODE_VOLUME_DOWN=25;
	KEYCODE_POWER=26;
	KEYCODE_CAMERA=27;
	KEYCODE_CLEAR=28;
	KEYCODE_A=29;
	KEYCODE_B=30;
	KEYCODE_C=31;
	KEYCODE_D=32;
	KEYCODE_E=33;
	KEYCODE_F=34;
	KEYCODE_G=35;
	KEYCODE_H=36;
	KEYCODE_I=37;
	KEYCODE_J=38;
	KEYCODE_K=39;
	KEYCODE_L=40;
	KEYCODE_M=41;
	KEYCODE_N=42;
	KEYCODE_O=43;
	KEYCODE_P=44;
	KEYCODE_Q=45;
	KEYCODE_R=46;
	KEYCODE_S=47;
	KEYCODE_T=48;
	KEYCODE_U=49;
	KEYCODE_V=50;
	KEYCODE_W=51;
	KEYCODE_X=52;
	KEYCODE_Y=53;
	KEYCODE_Z=54;
	KEYCODE_COMMA=55;
	KEYCODE_PERIOD=56;
	KEYCODE_ALT_LEFT=57;
	KEYCODE_ALT_RIGHT=58;
	KEYCODE_SHIFT_LEFT=59;
	KEYCODE_SHIFT_RIGHT=60;
	KEYCODE_TAB=61;
	KEYCODE_SPACE=62;
	KEYCODE_SYM=63;
	KEYCODE_EXPLORER=64;
	KEYCODE_ENVELOPE=65;
	KEYCODE_ENTER=66;
	KEYCODE_DEL=67;
	KEYCODE_GRAVE=68;
	KEYCODE_MINUS=69;
	KEYCODE_EQUALS=70;
	KEYCODE_LEFT_BRACKET=71;
	KEYCODE_RIGHT_BRACKET=72;
	KEYCODE_BACKSLASH=73;
	KEYCODE_SEMICOLON=74;
	KEYCODE_APOSTROPHE=75;
	KEYCODE_SLASH=76;
	KEYCODE_AT=77;
	KEYCODE_NUM=78;
	KEYCODE_HEADSETHOOK=79;
	KEYCODE_FOCUS=80;//*Camera*focus
	KEYCODE_PLUS=81;
	KEYCODE_MENU=82;
	KEYCODE_NOTIFICATION=83;
	KEYCODE_SEARCH=84;
	KEYCODE_MEDIA_PLAY_PAUSE=85;
	KEYCODE_MEDIA_STOP=86;
	KEYCODE_MEDIA_NEXT=87;
	KEYCODE_MEDIA_PREVIOUS=88;
	KEYCODE_MEDIA_REWIND=89;
	KEYCODE_MEDIA_FAST_FORWARD=90;
	KEYCODE_MUTE=91;*/	
	

	public void processKeyInputEvent_NoMap(int keydown,int keycode)	
	{
		
		if((keydown != 0) &&(keycode != 0))
		{
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_0);
		}							
	}
			
	public void specialKeyMap(int keycode,int [] specialKeycode,int [] shiftPressed)
	{
		int keycode_index= 0;
		for(int i=0;i<specialKeycode.length;i++)
		{
			if(keycode == specialKeycode[i] )
			{
				keycode_index = i;
			}
		}
		//,!,",#,$,%,&,',(,),*,+,,,-,.,/,:,;,<,=,>,?,@,[,\,],^,_,`,{,|,},~			
	//	int specialKeycode[] = {57,2,40,4,5,6,8,40,10,11,9,13,51,12,52,52,39,39,227,13,228,53,215,26,43,27,7,12,399,26,43,27,215,14};
		
		switch(keycode_index)
		{
			case 0: // 
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SPACE);
			break;
			case 1: //!	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NOTIFICATION);
				break;
			case 2: //”					
				break;
			case 3: //#	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_POUND);
				break;
			case 4: //$					
				break;
			case 5: //%					
				break;
			case 6: //&					
				break;
			case 7: //'	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_APOSTROPHE);
				break;
			case 8: //(	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);
				break;
			case 9: //)		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);
				break;
			case 10: //*		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_STAR);
				break;
			case 11: //+
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_PLUS);
			//	mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_ADD);
				break;
			case 12: //,	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_COMMA);
				break;
			case 13: //- 
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_MINUS);
				//	mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
				break;
			case 14: //.			
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_PERIOD);
				break;
			case 15: // /KEYCODE_SLASH		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SLASH);
				break;
			case 16: //:					
				break;
			case 17: //;	
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SEMICOLON);
				break;
			case 18: //<					
				break;
			case 19: //=		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_EQUALS);
				break;
			case 20: //>					
				break;
			case 21: //? 					
				break;
			case 22: //@		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_AT);
				break;
			case 23: //[
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_LEFT_BRACKET);
				break;
			case 24: //\		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACKSLASH);
				break;
			case 25: //]		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_RIGHT_BRACKET);
				break;
			case 26: //^					
				break;
			case 27: //_					
				break;
			case 28: //`		
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_GRAVE);
				break;
			case 29: //{					
				break;
			case 30: //|					
				break;
			case 31: //}					
				break;
			case 32: //~					
				break;
								
		}
		
	}
	public void processKeyInputEvent(int left_shift,int left_alt,int keycode,int keysym)	
	{
		// q,w,e,r,t,y,u,i,o,p,a,s,d,f,g,h,j,k,l,z,x,c,v,b,n,m
		int qwerty[] = {30,48,46,32,18,33,34,35,23,36,37,38,50,49,24,25,16,19,31,20,22,47,17,45,21,44};			
		//,!,",#,$,%,&,',(,),*,+,,,-,.,/,:,;,<,=,>,?,@,[,\,],^,_,`,{,|,},~			
		int specialKeycode[] = {57,2,40,4,5,6,8,40,10,11,9,13,51,12,52,52,39,39,227,13,228,53,215,26,43,27,7,12,399,26,43,27,215,14};
		int shiftPressed[] = {0,1,1,1,1,1,1,0,1,1,1,1,0,0,0,1,1,0,1,1,1,1,0,0,0,0,1,1,0,1,1,1,1,0};
		
	
		if(left_shift == 42)//left shift pressed.
		{
			specialKeyMap(keycode,specialKeycode,shiftPressed);
			
			for(int i = 0; i < qwerty.length;i++)
			{
				if(keycode == qwerty[i])
				{			
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_A+i);
				}	
			}
		}
		else
		{
			if((keycode > 1) && (keycode < 11)&&(left_shift == 0)) //1~9
			{
				mInst.sendKeyDownUpSync(keycode+6);				
			}
			else if((keycode == 11) && (left_shift == 0) )
			{
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_0);	//0		
			}	
			
			switch(keycode)
			{
				case 12://-
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
					break;
				case 13://-
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUMPAD_ADD);
					break;	
				case 26://[
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_LEFT_BRACKET);
					break;
				case 27://]
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_RIGHT_BRACKET);
					break;
				case 39://;
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SEMICOLON);
					break;
				case 40://'
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_APOSTROPHE);
					break;
				case 43://\
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACKSLASH);
					break;
				case 51://,
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_COMMA);
					break;
				case 52://.
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_PERIOD);
					break;
				case 215://.
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_AT);
					break;
				case 399://.
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_GRAVE);
					break;
					
				default:
					break;
			}
							
			for(int i = 0; i < qwerty.length;i++)
			{
				if(keycode == qwerty[i])
				{			
					mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_A+i);
				}	
			}
		}	
		
		if(keycode == 14 ) //backspace
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
		if(keycode == 15 ) //TAB
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB);
		if(keycode == 28 ) //enter
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		
		if(keycode == 158 ) //escap del
		{   if(keysym == 65307)//escap
			{
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_ESCAPE);
			}
			else //del
			{
				mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_FORWARD_DEL);
			}				
		}
		
		//arrow
		if(keycode == 103 )
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
		if(keycode == 108 )
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		if(keycode == 105 )
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
		if(keycode == 106 )
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
		
		/*if(keycode == 127 ) //ctrl
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);*/
		
		if(keysym == 0xffe5)//CapsLock
		{
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_CAPS_LOCK);
		}
		if(keysym == 0xff7f)//NumLock
		{
			mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_NUM_LOCK);
		}
					
	}
	
	@Override
	public void run() {
		try {
			inputlister = new DatagramSocket(13138);//13138 port for input event
			float x,y = 0;
			int buttonMask = 0;
			int inputevent_value =0;
			int left_alt,left_shift,keycode,keydown,keysym=0;
			
			while (!finished) {
				DatagramPacket inputevent = new DatagramPacket(new byte[1024],
						1024);
				inputlister.receive(inputevent);

				String resp = new String(inputevent.getData());
				resp = resp.substring(0, inputevent.getLength());

//				log("recieved input:" + resp);  
//				showTextOnScreen("recieved input:" + resp);
				if (resp.length() > 4 //for point input event
						&& resp.substring(0, 5).equals("~PTR|")) {
					resp = resp.substring(5, resp.length());
			//		log("recieved pointer input: " + resp);
					
					String inputevent_str[]= resp.split("\\|");
					x = Float.parseFloat(inputevent_str[0]);
                    y = Float.parseFloat(inputevent_str[1]);
                    buttonMask = Integer.parseInt(inputevent_str[2]);
                    inputevent_value = Integer.parseInt(inputevent_str[3]);

                    processPointerInputEvent(x,y,buttonMask,inputevent_value);
					
				}else if(resp.length() > 4 //for key input event
						&& resp.substring(0, 5).equals("~KEY|"))
				{
					resp = resp.substring(5, resp.length());
					
					String inputevent_str[]= resp.split("\\|");
					
					left_shift = Integer.parseInt(inputevent_str[0]);
					left_alt = Integer.parseInt(inputevent_str[1]);
					keycode = Integer.parseInt(inputevent_str[2]);
					keysym = Integer.parseInt(inputevent_str[3]);
					processKeyInputEvent(left_shift,left_alt,keycode,keysym);
				
				}
				else {
//					log("recieved input: " + resp);
//					showTextOnScreen("recieved input:" + resp);
				}
			}
		} catch (IOException e) {
//			log("RECEIVED input:ERROR em SOCKETLISTEN " + e.getMessage());
		}
	}
}
