@echo off
echo ========================================
echo   ARENA 3 - Build Script (Windows)
echo ========================================
echo.

set SRC=src
set OUT=out

echo [1/3] Cleaning output directory...
if exist %OUT% rmdir /s /q %OUT%
mkdir %OUT%

echo [2/3] Compiling Java sources...
dir /s /b %SRC%\*.java > sources.txt
javac -d %OUT% @sources.txt
del sources.txt

if %ERRORLEVEL% neq 0 (
    echo.
    echo BUILD FAILED! Check errors above.
    pause
    exit /b 1
)

echo [3/3] Creating JAR...
echo Main-Class: arena3.Main > manifest.txt
jar cfm Arena3.jar manifest.txt -C %OUT% .
del manifest.txt

echo.
echo ========================================
echo   BUILD SUCCESSFUL!
echo   Run: java -jar Arena3.jar
echo ========================================
echo.

echo Starting Arena 3...
java -jar Arena3.jar
