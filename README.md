# S-Emulator

This project is a simple client–server JavaFX application built **without Maven or Gradle**.  
It demonstrates how to run a multiclient setup using a single Java HTTP server and a JavaFX graphical client.

---

## 📁 Project Structure

project/
├─ src/
│ ├─ server/ # Server-side Java code
│ └─ client/ # Client-side JavaFX code
│
├─ resources/
│ └─ client/
│ ├─ css/ # Client styles
│ ├─ fxml/ # FXML layouts
│ └─ icons/ # UI icons
│
├─ lib/
│ └─ javafx/ # JavaFX SDK (copied manually)
│
├─ dist/
│ ├─ server.jar # Built server JAR
│ └─ client.jar # Built client JAR
│
├─ out/ # Compiled .class files
├─ build_all.bat # Builds both server and client
├─ server.bat # Runs the server
├─ client.bat # Runs the JavaFX client
└─ README.md


---

## ⚙️ Requirements

- **JDK 17 or newer**  
- **JavaFX SDK** (already included in `lib/javafx`)

---

## 🏗️ Building

Run the following script:

```bash
build_all.bat

It will:

--> Compile server and client code
--> Copy resources (CSS, FXML, icons)
--> Create dist/server.jar and dist/client.jar

🚀 Running
Run the Server : run_server.bat
Run the Client : run_client.bat
The client connects to the running server and starts the JavaFX interface.



