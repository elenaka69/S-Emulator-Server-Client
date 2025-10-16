@echo off
set JAVAFX_LIB=lib\javafx
set JAVAFX_BIN=lib\javafx\bin

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml ^
     -Dprism.verbose=true -Dprism.order=sw ^
     -Djava.library.path="%JAVAFX_BIN%" ^
     -cp "dist\client.jar;dist\server.jar;lib\*" client.MainApp

pause
