@echo off
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
java -cp "C:\Users\xinan\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/exam408" -user sa -password "" -sql "SCRIPT TO 'C:/Users/xinan/AppData/Local/Temp/opencode/h2_dump.sql' CHARSET 'UTF-8'"
echo EXITCODE=%ERRORLEVEL%