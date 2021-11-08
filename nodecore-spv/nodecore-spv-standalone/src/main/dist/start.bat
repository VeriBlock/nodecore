echo setup nodecore

java -version >nul 2>&1 && (
    goto java_found
) || (
    goto java_not_found
)
   
:java_not_found
	@echo off
	echo JAVA NOT FOUND
	echo It is recommended to use Java 14 to run NodeCore.
	echo The direct install link is here:
	echo https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14.0.2%2B12/OpenJDK14U-jre_x64_windows_hotspot_14.0.2_12.msi
	echo Please first install Java and then re-run `start.bat` 
	
	pause
	exit /B 1
	
:java_found
	echo FOUND JAVA
	java -version
	echo Continue...	

	rem Kick off Nodcore in separate window
	cd bin/
	start veriblock-spv.bat


