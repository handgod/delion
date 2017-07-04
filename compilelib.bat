@echo off
call ndk-build -C vnclib
xcopy  vnclib\libs app\src\main\jniLibs /Y /s
pause
