@echo off
cd /d "%~dp0"
echo [Build] Compiling MiniCompiler + Server...
javac -encoding UTF-8 MiniCompiler.java CompilerServer.java
if errorlevel 1 (
    echo [Error] Compilation failed.
    pause
    exit /b 1
)
echo [Build] Creating compiler-server.jar...
jar cfm compiler-server.jar MANIFEST.MF *.class
echo [Start] Server starting on http://localhost:8080
java -jar compiler-server.jar
