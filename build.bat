@echo off
setlocal

REM Simple helper to build backend/frontend Docker images on Windows
REM Usage: double-click or run `build.bat` from a terminal in the repo root.

set IMAGE_PREFIX=newtechtest

echo.
echo [1/2] Building backend image...
docker build -t %IMAGE_PREFIX%-backend backend
if errorlevel 1 goto :error

echo.
echo [2/2] Building frontend image...
docker build -t %IMAGE_PREFIX%-frontend frontend
if errorlevel 1 goto :error

echo.
echo Docker images built successfully:
echo   %IMAGE_PREFIX%-backend
echo   %IMAGE_PREFIX%-frontend
echo.
exit /b 0

:error
echo.
echo Docker build failed. See errors above.
exit /b 1

