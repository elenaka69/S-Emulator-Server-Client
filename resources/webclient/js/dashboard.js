
const params = new URLSearchParams(window.location.search);
const username = params.get("username");
const availCredit = document.getElementById("availCredit");
const creditInput = document.getElementById("creditInput");

let selectedUser = null;

let selectedProgram = null;
let selectedProgramCost = null;
const SELECT_NONE = 0;
const SELECT_PROGRAM = 1;
const SELECT_FUNCTION = 2;
let typeProgramSelected = SELECT_NONE;

if (username) {
    const userDisplay = document.getElementById("loggedInUser");
    if (userDisplay) {
        userDisplay.textContent = `Welcome, ${username}!`;
        loadUserCredits(username);
        selectedUser = username;
        refreshAll();
        setInterval(refreshAll, 1000); // one second interval
    }
} else {
    console.warn("No username provided in URL");
    showStatus();("No username provided in URL", "error");
}

function refreshAll() {
  loadConnectedUsers();
    loadProgramsTable();
    loadFunctionsTable();
    loadUserStatistics();
}

async function loadUserCredits(username) {
    const requestBody = {
        action: "getCredits",
        data: { username: username }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody)
        });

        const result = await response.json();

        if (result.ok) {
            const credits = result.data?.credits;
            if (credits !== undefined && credits !== null) {
                availCredit.value = credits;
            } else {
                showStatus("No credits field in response", "warning");
            }
        } else {
            showStatus(result.message || "Unknown error", "warning");
        }
    } catch (err) {
        showStatus("Network or server error: " + err.message, "error");
    }
}

async function loadConnectedUsers() {
    const request = {
        action: "getUsers",
        data: {}
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        const tableBody = document.querySelector("#connectedUsersTable tbody");
        tableBody.innerHTML = ""; // clear existing rows

        if (result.ok && result.data && Array.isArray(result.data.users)) {
            const users = result.data.users;

            if (users.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="7">No connected users.</td></tr>`;
                return;
            }
            populateConnectedUsersTable(users);

        } else {
            const msg = result.message || "Failed to load users.";
            tableBody.innerHTML = `<tr><td colspan="7">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading users:", err);
        const tableBody = document.querySelector("#connectedUsersTable tbody");
        tableBody.innerHTML = `<tr><td colspan="7">Server connection error.</td></tr>`;
    }
}

function populateConnectedUsersTable(users) {
    const tableBody = document.querySelector("#connectedUsersTable tbody");
    tableBody.innerHTML = ""; // clear old rows

    users.forEach((u, index) => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${u.userName}</td>
            <td>${u.uploadedPrograms}</td>
            <td>${u.uploadedFunctions}</td>
            <td>${u.creditBalance}</td>
            <td>${u.spentCredits}</td>
            <td>${u.executions}</td>
        `;

         if (selectedUser && u.userName === selectedUser) {
            row.classList.add("selected");
        }

        // attach click listener here
        row.addEventListener("click", () => {
            // remove previous selection
            tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
            row.classList.add("selected");
            selectedUser = u.userName;
            loadUserStatistics();
        });

        tableBody.appendChild(row);
    });
}

async function loadProgramsTable() {
    const request = {
        action: "getPrograms",
        data: {}
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        const tableBody = document.querySelector("#programsTable tbody");
        tableBody.innerHTML = ""; // clear previous rows

        if (result.ok && result.data && Array.isArray(result.data.programs)) {
            const programs = result.data.programs;

            if (programs.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="7">No programs available.</td></tr>`;
                return;
            }
            populateProgramsTable(programs);
            loadConnectedUsers();
        } else {
            const msg = result.message || "Failed to load programs.";
            tableBody.innerHTML = `<tr><td colspan="7">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading programs:", err);
        const tableBody = document.querySelector("#programsTable tbody");
        tableBody.innerHTML = `<tr><td colspan="7">Server connection error.</td></tr>`;
    }
}

function populateProgramsTable(programs) {
        const tableBody = document.querySelector("#programsTable tbody");
        tableBody.innerHTML = ""; // clear previous rows

        programs.forEach((p, index) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${p.number ?? index + 1}</td>
                <td>${p.name ?? ""}</td>
                <td>${p.userName ?? ""}</td>
                <td>${p.numInstructions ?? 0}</td>
                <td>${p.maxCost ?? 0}</td>
                <td>${p.numExec ?? 0}</td>
                <td>${p.averCost ?? 0}</td>
            `;

            //  reselect if this program is the previously selected one
            if (typeProgramSelected === SELECT_PROGRAM && selectedProgram && p.name === selectedProgram) {
                row.classList.add("selected");
            }

            // attach click listener here
            row.addEventListener("click", () => {
                // remove previous selection
                tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
                row.classList.add("selected");
                removeFunctionTableSelection();
                typeProgramSelected = SELECT_PROGRAM;
                selectedProgram  = p.name;
                selectedProgramCost = p.maxCost + p.averCost;
            });

            tableBody.appendChild(row);
        });
}

function removeProgramTableSelection() {
    const tableBody = document.querySelector("#programsTable tbody");
    tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
}

function removeFunctionTableSelection() {
     const tableBody = document.querySelector("#functionsTable tbody");
     tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
 }

async function loadFunctionsTable() {
    const request = {
        action: "getFunctions",
        data: {}
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();
        const tableBody = document.querySelector("#functionsTable tbody");
        tableBody.innerHTML = ""; // clear previous rows

        if (result.ok && result.data && Array.isArray(result.data.functions)) {
            const functions = result.data.functions;

            if (functions.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6">No functions available.</td></tr>`;
                return;
            }

            populateFunctionsTable(functions);
            loadConnectedUsers();
        } else {
            const msg = result.message || "Failed to load functions.";
            tableBody.innerHTML = `<tr><td colspan="6">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading functions:", err);
        const tableBody = document.querySelector("#functionsTable tbody");
        tableBody.innerHTML = `<tr><td colspan="6">Server connection error.</td></tr>`;
    }
}

function populateFunctionsTable(functions) {
    const tableBody = document.querySelector("#functionsTable tbody");
    tableBody.innerHTML = ""; // clear previous rows

    functions.forEach((f, index) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${f.number ?? index + 1}</td>
            <td>${f.name ?? ""}</td>
            <td>${f.programName ?? ""}</td>
            <td>${f.userName ?? ""}</td>
            <td>${f.numInstructions ?? 0}</td>
            <td>${f.maxCost ?? 0}</td>
        `;

         //  reselect if this program is the previously selected one
            if (typeProgramSelected === SELECT_FUNCTION && selectedProgram && f.name === selectedProgram) {
                row.classList.add("selected");
            }

         // attach click listener here
            row.addEventListener("click", () => {
                // remove previous selection
                tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
                row.classList.add("selected");
                removeProgramTableSelection();
                typeProgramSelected = SELECT_FUNCTION;
                selectedProgram  = f.name;
                selectedProgramCost = f.maxCost;
            });
        tableBody.appendChild(row);
    });
}

async function loadUserStatistics() {
    const request = {
        action: "userStatistics",
        data: { username: selectedUser }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();
        const tableBody = document.querySelector("#statisticTable tbody");
        tableBody.innerHTML = ""; // clear after loading

        if (result.ok && result.data && Array.isArray(result.data.execStatistics)) {
            const stats = result.data.execStatistics;

            if (stats.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="7">No statistics available.</td></tr>`;
                return;
            }

            stats.forEach((u, index) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${u.number ?? index + 1}</td>
                    <td>${u.type ?? ""}</td>
                    <td>${u.name ?? ""}</td>
                    <td>${u.arch ?? ""}</td>
                    <td>${u.degree ?? 0}</td>
                    <td>${u.result ?? 0}</td>
                    <td>${u.cycles ?? 0}</td>
                `;
                tableBody.appendChild(row);
            });
        } else {
            const msg = result.message || "Failed to load statistics.";
            tableBody.innerHTML = `<tr><td colspan="7">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading statistics:", err);
        tableBody.innerHTML = `<tr><td colspan="7">Server connection error.</td></tr>`;
    }
}

document.getElementById("chargeCreditBtn").addEventListener("click", async () => {
    const amountStr = document.getElementById("creditInput").value.trim();

    if (!amountStr) {
        alert("Please enter the amount of credits to charge.");
        return;
    }

    const amount = parseInt(amountStr, 10);
    if (isNaN(amount) || amount <= 0) {
        alert("Amount must be a positive number.");
        return;
    }

    const request = {
        action: "chargeCredits",
        data: {
            username: username,
            amount: amount
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok) {
            showStatus("Credits charged successfully.");
            if (result.data && result.data.newBalance != null) {
                document.getElementById("availCredit").value = result.data.newBalance;
                document.getElementById("creditInput").value = "";
                loadConnectedUsers();
            }
        } else {
            showStatus("Failed to charge credits.", "error");
        }
    } catch (err) {
        console.error("Error charging credits:", err);
        showStatus("Server error while charging credits.", "error");
    }
});

document.getElementById("unSelectUserBtn").addEventListener("click", async () => {
     document.querySelectorAll("#connectedUsersTable tbody tr").forEach(row => {
        row.classList.remove("selected");
    });
    selectedUser = username;
    loadUserStatistics();
});

document.getElementById("executeProgramBtn").addEventListener("click", async () => {

    if (typeProgramSelected === SELECT_NONE || !selectedProgram) {
        alert("Please select a program or function to execute.");
        return;
    }

    const availableCredits = parseInt(availCredit.value);

    if (selectedProgramCost > availableCredits) {
        showAlert(
            "Insufficient Credits",
            `You do not have enough credits to execute  ${selectedProgram} .\n` +
            `Program Cost: ${selectedProgramCost}\n` +
            `Your Credits: ${availableCredits}`,
            "warning"
        );
        return;
    }

 //   window.location.href = `execute.html?username=username&programName=selectedProgram&isProgram=${typeProgramSelected === SELECT_PROGRAM ? 1 : 0}&availableCredit=${availableCredits}`;

});

document.getElementById("loadFileBtn").addEventListener("click", async () => {
    const fileInput = document.getElementById("xmlFile");
    fileInput.click(); // open file chooser
});

document.getElementById("xmlFile").addEventListener("change", async (event) => {
    const file = event.target.files[0];
    const filePathField = document.getElementById("fileNameLabel");

    if (!file) {
        filePathField.value = "";
        showStatus("No file selected.", "warning");
        return;
    }

    if (!file.name.endsWith(".xml")) {
        filePathField.value = "";
        showStatus("Invalid file type. Please select an XML file.", "warning");
        return;
    }

    filePathField.value = file.name;

    try {
        showStatus("Loading file...", "info");

        // Read file as Base64
        const arrayBuffer = await file.arrayBuffer();
        const base64Data = btoa(
            new Uint8Array(arrayBuffer)
                .reduce((data, byte) => data + String.fromCharCode(byte), "")
        );

        // Prepare request payload
        const payload = {
            action: "uploadFile",
            data: {
                username: username,  // global variable from login
                filename: file.name,
                fileData: base64Data
            }
        };

        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const json = await response.json();

        if (json.ok) {
            showStatus("File uploaded successfully!", "info");
            // Refresh tables
            loadUserStatistics();
            loadProgramsTable();
            loadFunctionsTable();
        } else {
            showStatus("Upload failed: " + json.message, "error");
        }

    } catch (err) {
        showStatus("Failed to read or upload file: " + err.message, "error");
    }
});

function showAlert(title, message, type = "info") {
    let color;
    switch (type) {
        case "error": color = "red"; break;
        case "warning": color = "orange"; break;
        case "success": color = "green"; break;
        default: color = "blue";
    }

    alert(`${title}\n\n${message}`);

    showStatus(`${title}: ${message}`, type);
}


function showStatus(message, type = "info") {
    const statusBar = document.getElementById('statusBar');
    statusBar.textContent = message;

    switch(type.toLowerCase()) {
        case "error":
            statusBar.style.color = "red";
            break;
        case "warning":
            statusBar.style.color = "orange";
            break;
        case "info":
        default:
            statusBar.style.color = "green";
            break;
    }
}

