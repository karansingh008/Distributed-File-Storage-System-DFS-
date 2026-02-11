@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME is set to %JAVA_HOME%
call "C:\Users\Asus\Distributed File Storage System\apache-maven-3.9.6\bin\mvn.cmd" clean package -DskipTests
