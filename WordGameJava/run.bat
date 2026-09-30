@echo off
REM Compile and run the Guess the Word game (Windows).
REM Requires a JDK (javac + java), version 17 or newer. No other dependencies.
cd /d "%~dp0"

echo Compiling...
if not exist out mkdir out

REM Build a list of all .java files, then compile
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo Starting server...
java -cp out wordgame.App
