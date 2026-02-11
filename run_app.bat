@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
set "MAVEN_HOME=%~dp0apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo JAVA_HOME is set to %JAVA_HOME%

cd /d "%~dp0"
echo Starting DfsApplication...
"%JAVA_HOME%\bin\java.exe" -jar "target\dfs-0.0.1-SNAPSHOT.jar"

