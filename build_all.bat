@echo off
setlocal enabledelayedexpansion

REM ================= CONFIG — adjust these =================
REM Path to JavaFX SDK lib folder (do NOT include trailing slash)
set "PATH_TO_FX=lib\javafx"

REM Fully-qualified main classes
set "SERVER_MAIN_CLASS=server.ServerMain"
set "CLIENT_MAIN_CLASS=client.MainApp"
REM =========================================================

REM ---------------- CREATE OUTPUT FOLDERS -------------------
if not exist out mkdir out
if not exist out\server mkdir out\server
if not exist out\client mkdir out\client
if not exist dist mkdir dist

REM ---------------- CLEAN OLD SOURCE LISTS -----------------
if exist sources_server.txt del sources_server.txt
if exist sources_client.txt del sources_client.txt

REM ================ COLLECT SERVER + SHARED SOURCES =========
echo.
echo ================= Collecting server + shared sources =================
for /r "src\server" %%f in (*.java) do (
    set "p=%%~f"
    set "p=!p:\=/!"
    >>sources_server.txt echo "!p!"
)
for /r "src\shared" %%f in (*.java) do (
    set "p=%%~f"
    set "p=!p:\=/!"
    >>sources_server.txt echo "!p!"
)

for /f %%c in ('find /v /c "" ^< sources_server.txt') do set SERVER_COUNT=%%c
echo Found %SERVER_COUNT% server/shared source(s).

REM ================== COMPILE SERVER ==================
echo Compiling server...
if exist lib (
    javac -cp "lib/*" -d out\server @sources_server.txt
) else (
    javac -d out\server @sources_server.txt
)
if errorlevel 1 (
    echo [ERROR] Server compilation failed.
    pause
    exit /b 1
)

REM -------------- COPY SERVER RESOURCES (webclient) ----------
echo.
echo Copying server resources (webclient)...
if not exist out\server\webclient mkdir out\server\webclient
xcopy /E /I /Y "resources\webclient" out\server\webclient >nul
if errorlevel 1 (
    echo [WARNING] Some server resources may not have been copied.
)

REM ================== PACKAGE SERVER JAR ==================
if exist dist\server.jar del dist\server.jar
echo Packaging server.jar...
jar cfe dist\server.jar %SERVER_MAIN_CLASS% -C out\server .
if errorlevel 1 (
    echo [ERROR] Creating server.jar failed.
    pause
    exit /b 1
)

REM ================== COLLECT CLIENT + SHARED SOURCES ==================
echo.
echo ================= Collecting client + shared sources =================
if exist sources_client.txt del sources_client.txt
for /r "src\client" %%f in (*.java) do (
    set "p=%%~f"
    set "p=!p:\=/!"
    >>sources_client.txt echo "!p!"
)
for /r "src\shared" %%f in (*.java) do (
    set "p=%%~f"
    set "p=!p:\=/!"
    >>sources_client.txt echo "!p!"
)

for /f %%c in ('find /v /c "" ^< sources_client.txt') do set CLIENT_COUNT=%%c
echo Found %CLIENT_COUNT% client/shared source(s).

REM ================== COMPILE CLIENT (JavaFX) ==================
echo Compiling client (JavaFX)...
if "%PATH_TO_FX%"=="" (
    echo [ERROR] PATH_TO_FX is not set. Edit build_all.bat and set it.
    pause
    exit /b 1
)

if exist lib (
    javac -cp "lib/*" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -d out\client @sources_client.txt
) else (
    javac --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -d out\client @sources_client.txt
)
if errorlevel 1 (
    echo [ERROR] Client compilation failed.
    pause
    exit /b 1
)

REM ================== COPY CLIENT RESOURCES ==================
echo.
echo Copying client resources (css, fxml, icons)...
if not exist out\client\client mkdir out\client\client
xcopy /E /I /Y "resources\client\css" out\client\client\css >nul
xcopy /E /I /Y "resources\client\fxml" out\client\client\fxml >nul
xcopy /E /I /Y "resources\client\icons" out\client\client\icons >nul

REM ================== PACKAGE CLIENT JAR ==================
echo.
echo Packaging client.jar...
jar cfe dist\client.jar %CLIENT_MAIN_CLASS% -C out\client .
if errorlevel 1 (
    echo [ERROR] Creating client.jar failed.
    pause
    exit /b 1
)
REM ----------------- FINISHED ------------------------------
echo.
echo === BUILD SUCCEEDED ===
echo Server jar: dist\server.jar
echo Client jar: dist\client.jar
pause
endlocal
