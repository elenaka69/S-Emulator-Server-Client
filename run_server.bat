@echo off
set CLASSPATH=dist\server.jar;lib\*

java -cp "%CLASSPATH%" server.ServerMain
pause