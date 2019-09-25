@ECHO OFF
:LoopStart
REM start nodecore-pop miner
CALL nodecore-pop.bat

REM Break from the loop if exit code is not 2
IF NOT "%ERRORLEVEL%"=="2" (
   ECHO VPM exited with return code "%ERRORLEVEL%"
   ECHO Exiting VPM...
   GOTO LoopEnd
)
REM Re-start VPM if exit code is 2
   ECHO VPM exited with return code "%ERRORLEVEL%"
   ECHO Restarting VPM...
GOTO LoopStart
:LoopEnd
pause