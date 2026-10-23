@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

set "BACKEND_DIR=%ROOT%\backend"
set "FRONTEND_DIR=%ROOT%\frontend\casepilot"

echo ======================================
echo   CasePilot Launcher
echo ======================================
echo Project root: %ROOT%
echo.

if not defined JWT_SECRET (
    echo [ERROR] JWT_SECRET is missing. Import .env with scripts\Import-LocalEnvironment.ps1 first.
    goto :fail
)
if not defined DB_USERNAME (
    echo [ERROR] DB_USERNAME is missing. Import the local environment first.
    goto :fail
)
if not defined DB_PASSWORD (
    echo [ERROR] DB_PASSWORD is missing. Import the local environment first.
    goto :fail
)

call :require_command java "Java is required for the backend services."
if errorlevel 1 goto :fail
call :require_command mvn "Maven is required because this project does not include mvnw.cmd."
if errorlevel 1 goto :fail
call :require_command node "Node.js is required for the frontend."
if errorlevel 1 goto :fail
call :require_command npm "npm is required for the frontend."
if errorlevel 1 goto :fail

if not exist "%BACKEND_DIR%\docker-compose.yml" (
    echo [ERROR] Missing backend\docker-compose.yml
    goto :fail
)

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] Missing frontend package.json at:
    echo         %FRONTEND_DIR%\package.json
    goto :fail
)

call :start_mysql
if errorlevel 1 goto :fail

call :ensure_frontend_dependencies
if errorlevel 1 goto :fail

call :start_if_free 8081 "auth-service" "%BACKEND_DIR%\auth-service" "call mvn -Dmaven.test.skip=true spring-boot:run"
if errorlevel 1 goto :fail
call :start_if_free 8084 "dispute-service" "%BACKEND_DIR%\dispute-service" "call mvn -Dmaven.test.skip=true spring-boot:run"
if errorlevel 1 goto :fail
call :start_if_free 8080 "api-gateway" "%BACKEND_DIR%\api-gateway" "call mvn -Dmaven.test.skip=true spring-boot:run"
if errorlevel 1 goto :fail
call :start_if_free 4200 "frontend" "%FRONTEND_DIR%" "call npm start"
if errorlevel 1 goto :fail

echo.
echo Launch commands sent.
echo Frontend: http://localhost:4200
echo Gateway:  http://localhost:8080
echo.
echo Close the opened terminal windows to stop the app.
goto :success

:require_command
where %~1 >nul 2>nul
if errorlevel 1 (
    echo [ERROR] %~1 was not found in PATH.
    echo         %~2
    exit /b 1
)
exit /b 0

:start_mysql
call :is_port_listening 3306
if not errorlevel 1 (
    echo [INFO] Port 3306 is already in use. Assuming MySQL is already running.
    exit /b 0
)

where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] MySQL is not running on port 3306, and Docker is not available to start it automatically.
    echo         Start MySQL manually or install Docker Desktop.
    exit /b 1
)

docker compose version >nul 2>nul
if errorlevel 1 (
    where docker-compose >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Docker is installed, but neither 'docker compose' nor 'docker-compose' is available.
        exit /b 1
    )

    echo [INFO] Starting MySQL with docker-compose...
    docker-compose -f "%BACKEND_DIR%\docker-compose.yml" up -d mysql
    if errorlevel 1 (
        echo [ERROR] Failed to start MySQL with docker-compose.
        exit /b 1
    )
) else (
    echo [INFO] Starting MySQL with docker compose...
    docker compose -f "%BACKEND_DIR%\docker-compose.yml" up -d --wait mysql
    if errorlevel 1 (
        echo [ERROR] Failed to start MySQL with docker compose.
        exit /b 1
    )
)

exit /b 0

:ensure_frontend_dependencies
if exist "%FRONTEND_DIR%\node_modules" (
    echo [INFO] Frontend dependencies already installed.
    exit /b 0
)

echo [INFO] Installing frontend dependencies...
pushd "%FRONTEND_DIR%" >nul
call npm install
set "INSTALL_EXIT=!ERRORLEVEL!"
popd >nul

if not "%INSTALL_EXIT%"=="0" (
    echo [ERROR] npm install failed.
    exit /b 1
)

exit /b 0

:start_if_free
set "PORT=%~1"
set "TITLE=%~2"
set "WORKDIR=%~3"
set "RUN_CMD=%~4"

call :is_port_listening %PORT%
if not errorlevel 1 (
    echo [INFO] Port %PORT% is already in use. Skipping %TITLE%.
    exit /b 0
)

if not exist "%WORKDIR%" (
    echo [ERROR] Missing folder for %TITLE%:
    echo         %WORKDIR%
    exit /b 1
)

start "%TITLE%" cmd /k "cd /d ""%WORKDIR%"" && %RUN_CMD%"
exit /b 0

:is_port_listening
set "PORT_FOUND="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%~1 .*LISTENING"') do (
    set "PORT_FOUND=1"
)

if defined PORT_FOUND (
    exit /b 0
)

exit /b 1

:fail
echo.
echo Launcher stopped because a required dependency or service is missing.
pause
exit /b 1

:success
exit /b 0
