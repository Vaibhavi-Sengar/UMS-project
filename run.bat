@echo off
echo Compiling the program...
javac src/SimpleUniversitySystem.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Running the program...
java -cp src SimpleUniversitySystem
pause 