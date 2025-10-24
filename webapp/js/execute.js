const params = new URLSearchParams(window.location.search);
const username = params.get("username");
const programName = params.get("programName");
const availableCredit = params.get("availableCredit");
const isProgram = params.get("isProgram") === "true";
const availCreditField = document.getElementById("availCredit");
let paramFields = [];
let inputVariables = [];
let runListMap = [];
let maxDegree = 0;
let highlightText = null;

if (username) {
    const userDisplay = document.getElementById("loggedInUser");
    if (userDisplay) {
        userDisplay.textContent = `${username}`;
        availCreditField.value = `${availableCredit}`;
        setProgramToUser(isProgram);
        setStatistics();
        showStatus("Loaded program: " + programName);
      //  loadUserCredits(username);
     //   selectedUser = username;
     //   refreshAll();
    //    setInterval(refreshAll, 1000); // one second interval
    }
} else {
    console.warn("No username provided in URL");
    showStatus();("No username provided in URL", "error");
}


async function setProgramToUser(isProgram) {
    const requestBody = {
        action: "setProgramToUser",
        data: {
            username: username,
            programName: programName,
            isProgram: isProgram
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody)
        });

        const result = await response.json();

        if (result.ok) {
            showStatus(result.message || "Program updated successfully", "info");
            loadProgramInstructions();
            loadHighlightComboBox();
            loadInputVariables();
            setRangeDegree(0);
            loadFuncsSelection();
        } else {
            showStatus(result.message || "Unknown error", "warning");
        }
    } catch (err) {
        console.error("Error:", err);
        showStatus("Network or server error: " + err.message, "error");
    }
}

async function loadFuncsSelection() {
    const request = {
        action: "getProgramFunctions",
        data: {
            username: username,
            programName: programName
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok && result.data && Array.isArray(result.data.functions)) {
            const functions = result.data.functions;

            const funcsComboBox = document.getElementById("funcsComboBox");
            funcsComboBox.innerHTML = ""; // clear old options

            functions.forEach(func => {
                const option = document.createElement("option");
                option.value = func;
                option.textContent = func;
                funcsComboBox.appendChild(option);
            });

            if (functions.length > 0) {
                funcsComboBox.selectedIndex = 0;
            }
            // Attach listener
            funcsComboBox.addEventListener("change", event => {
                const selectedFunc = event.target.value;
                onFuncsSelection(selectedFunc);
            });

        } else {
            const msg = result.message || "Failed to load functions.";
            showStatus(msg, "warning");
        }
    } catch (err) {
        console.error("Error loading functions:", err);
        showStatus("Server connection error.", "warning");
    }
}

async function onFuncsSelection(selectedFunc) {
    if (!selectedFunc) return;

    const historyTableBody = document.querySelector("#historyInstructionTable tbody");
    if (historyTableBody) historyTableBody.innerHTML = "";

    const request = {
        action: "setWokFunctionUser",
        data: {
            username: username,
            funcName: selectedFunc
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
            showStatus(result.message, "info");

            // Replicates JavaFX Platform.runLater() sequence:
            await loadProgramInstructions();
            await loadHighlightComboBox();
            await loadInputVariables();
            await setRangeDegree(0);
            await setStatistics();

            // clear debug table
            const debugTableBody = document.querySelector("#debugTable tbody");
            if (debugTableBody) debugTableBody.innerHTML = "";

        } else {
            showStatus(result.message || "Operation failed", "warning");
        }
    } catch (err) {
        console.error("Error setting work function:", err);
        showStatus("Server connection error: " + err.message, "error");
    }
}

async function loadInputVariables() {
    const request = {
        action: "getProgramInputVariables",
        data: { username: username }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok && result.data && Array.isArray(result.data.inputVariables)) {
            const variables = result.data.inputVariables;
            createRunParameterFields(variables);
        } else {
            const msg = result.message || "Failed to load input variables.";
            showStatus(msg, "warning");
        }

    } catch (err) {
        console.error("Error loading input variables:", err);
        showStatus("Server connection error: " + err.message, "error");
    }
}

function createRunParameterFields(variables) {
    const runBox = document.getElementById("runBox");
    if (!runBox) {
        console.error("runBox not found in HTML");
        return;
    }

    // Remove old paramBox if it exists
    let oldParamBox = document.getElementById("paramBox");
    if (oldParamBox) {
        runBox.removeChild(oldParamBox);
    }
    paramFields = [];

    if (!variables || variables.length === 0) return;
    inputVariables = [...variables];

    // Create label
    const paramLabel = document.createElement("div");
    paramLabel.textContent = "Enter variables";
    paramLabel.style.textAlign = "center";
    paramLabel.style.fontWeight = "bold";
    paramLabel.style.marginBottom = "5px";

    // Create container for input fields (HBox equivalent)
    const fieldsBox = document.createElement("div");
    fieldsBox.style.display = "flex";
    fieldsBox.style.gap = "10px";
    fieldsBox.style.justifyContent = "center";
    fieldsBox.style.alignItems = "center";

    // Create text fields
    variables.forEach(varName => {
        const field = document.createElement("input");
        field.type = "number";
        field.placeholder = varName;
        field.style.width = "80px";
        field.style.padding = "4px 6px";
        field.style.border = "1px solid #ccc";
        field.style.borderRadius = "5px";
        field.style.fontSize = "13px";
        field.style.textAlign = "center";
        fieldsBox.appendChild(field);
        paramFields.push(field);
    });

    // Create paramBox (VBox equivalent)
    const paramBox = document.createElement("div");
    paramBox.id = "paramBox";
    paramBox.style.display = "flex";
    paramBox.style.flexDirection = "column";
    paramBox.style.alignItems = "center";
    paramBox.style.gap = "5px";
    paramBox.appendChild(paramLabel);
    paramBox.appendChild(fieldsBox);

    // Insert paramBox before runButtonsBox
    const runButtonsBox = document.getElementById("runButtonsBox");
    if (runButtonsBox && runButtonsBox.parentNode === runBox) {
        runBox.insertBefore(paramBox, runButtonsBox);
    } else {
        runBox.appendChild(paramBox); // fallback if no buttons box found
    }
}

async function setRangeDegree(degree) {
    const request = {
        action: "getDegreeProgram",
        data: {
            username: username
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok && result.data && result.data.degree !== undefined) {
            maxDegree = result.data.degree;

            // Update the UI
            document.getElementById("expandLabel").textContent = `Range degrees (0–${maxDegree})`;
            document.getElementById("expandField").value = degree;
        } else {
            const msg = result.message || "Failed to load degree range.";
            showStatus(msg, "warning");
        }
    } catch (err) {
        console.error("Error loading degree range:", err);
        showStatus("Server connection error.", "warning");
    }
}


async function loadProgramInstructions() {
    const request = {
        action: "getProgramInstructions",
        data: { username: username }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        const tableBody = document.querySelector("#instructionTable tbody");
        tableBody.innerHTML = ""; // clear existing rows

        if (result.ok && result.data && Array.isArray(result.data.instructions)) {
            const instructions = result.data.instructions;

            if (instructions.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6">No instructions found.</td></tr>`;
                return;
            }

            populateInstructionTable(instructions);

        } else {
            const msg = result.message || "Failed to load instructions.";
            tableBody.innerHTML = `<tr><td colspan="7">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading instructions:", err);
        const tableBody = document.querySelector("#instructionTable tbody");
        tableBody.innerHTML = `<tr><td colspan="7">Server connection error.</td></tr>`;
    }
}

function populateInstructionTable(instructions) {
    const tableBody = document.querySelector("#instructionTable tbody");
    tableBody.innerHTML = ""; // clear old rows

    instructions.forEach((instr) => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${instr.number ?? ""}</td>
            <td>${instr.type ?? ""}</td>
            <td>${instr.arch ?? ""}</td>
            <td>${instr.label ?? ""}</td>
            <td>${instr.instruction ?? ""}</td>
            <td>${instr.cycle ?? ""}</td>
        `;

        if (highlightText) {
            const regex = new RegExp(`\\b${highlightText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`);
            if (regex.test(instr.label) || regex.test(instr.instruction)) {
                row.style.backgroundColor = "#F08650"; // orange
                row.style.color = "black";
            }
        }

         row.addEventListener("click", () => {
            // remove previous selection
            tableBody.querySelectorAll("tr").forEach(r => r.classList.remove("selected"));
            row.classList.add("selected");
            loadInstructionHistory(instr.number);
        });

        tableBody.appendChild(row);
    });
}

async function loadInstructionHistory(index) {
    const request = {
        action: "getHistoryInstruction",
        data: {
            username: username,
            instructionNumber: index
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        const tableBody = document.querySelector("#historyInstructionTable tbody");
        tableBody.innerHTML = ""; // clear existing rows

        if (result.ok && result.data && Array.isArray(result.data.historyInstruction)) {
            const instructions = result.data.historyInstruction;

            if (instructions.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6">No instruction history found.</td></tr>`;
                return;
            }

            populateHistoryInstructionsTable(instructions);
        } else {
            const msg = result.message || "Failed to load instruction history.";
            tableBody.innerHTML = `<tr><td colspan="6">${msg}</td></tr>`;
        }
    } catch (err) {
        console.error("Error loading instruction history:", err);
        const tableBody = document.querySelector("#historyInstructionTable tbody");
        tableBody.innerHTML = `<tr><td colspan="6">Server connection error.</td></tr>`;
    }
}

function populateHistoryInstructionsTable(instructions) {
    const tableBody = document.querySelector("#historyInstructionTable tbody");
    tableBody.innerHTML = ""; // clear previous rows

    instructions.forEach(instr => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${instr.number}</td>
            <td>${instr.type}</td>
            <td>${instr.arch}</td>
            <td>${instr.label}</td>
            <td>${instr.instruction}</td>
            <td>${instr.cycle}</td>
        `;
        tableBody.appendChild(row);
    });
}

async function updateUserCredits() {
    const request = {
        action: "getCredits",
        data: { username: username }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok) {
            const val = result.data?.credits;
            if (val !== undefined && val !== null) {
                availCreditField.value = val;
                showStatus("Credits updated", "info");
            } else {
                showStatus("No credits field in response", "warning");
            }
        } else {
            showStatus(result.message || "Failed to update credits", "warning");
        }
    } catch (err) {
        console.error("Error updating credits:", err);
        showStatus("Network or server error: " + err.message, "error");
    }
}


document.getElementById("runButton").addEventListener("click", async () => {

    const success = await runRoutine();
    if (success) {
        populateRunResultTable(runListMap.length - 1);
        setStatistics();
    }
    updateUserCredits();
});

function populateRunResultTable(stepIndex) {
    const currentStep = runListMap[stepIndex];
    if (!currentStep || !currentStep.variables) return;

    const tableBody = document.querySelector("#runResultTable tbody");
    tableBody.innerHTML = ""; // clear old rows

    // Get the variables map (object of { name: value })
    const currentMap = currentStep.variables;

    // Add each variable row
    Object.entries(currentMap).forEach(([key, value]) => {
        const row = document.createElement("tr");

        const nameCell = document.createElement("td");
        nameCell.textContent = key;

        const valueCell = document.createElement("td");
        valueCell.textContent = value;

        row.appendChild(nameCell);
        row.appendChild(valueCell);
        tableBody.appendChild(row);
    });

    return currentStep.step; // return the step number, like in Java
}


async function runRoutine() {
    const ok = await checkAndConfirmParams();
    if (!ok) return false;

    const userVars = getUserVars();
    const curDegree = parseInt(document.getElementById("expandField").value.trim()) || 0;

    const request = {
        action: "runProgram",
        data: {
            username: username,
            inputVariables: userVars,
            isDebugMode: false,
            degree: curDegree
        }
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();
        let success = false;

        if (result.ok && result.data && Array.isArray(result.data.runListMap)) {
            runListMap = result.data.runListMap;
            if (runListMap.length > 0) success = true;
        }

        if (result.ok) {
            showStatus(result.message, "info");
        } else {
            showAlert("Run Failed", result.message || "Unknown error", "error");
        }

        return success;

    } catch (err) {
        console.error("Error during runProgram:", err);
        showAlert("Run Failed", "Server error: " + err.message, "error");
        return false;
    }
}

async function checkAndConfirmParams() {
    const emptyFields = paramFields.filter(f => !f.value.trim());
    if (emptyFields.length === 0) {
        return true; // all filled
    }

    const confirmMsg = "Some parameter fields are empty.\nEmpty fields will be treated as 0.\nDo you want to continue?";
    const proceed = confirm(confirmMsg);

    if (proceed) {
        emptyFields.forEach(f => { f.value = "0"; });
        return true;
    } else {
        return false;
    }
}

function getUserVars() {
    return paramFields.map(f => {
        const val = f.value.trim();
        return val === "" ? 0 : parseInt(val, 10);
    });
}

function populateProgramStatisticTable(runStatistics) {
    const tableBody = document.querySelector("#runHistoryTable tbody");
    tableBody.innerHTML = ""; // clear old rows
    let runHistoryCounter = 0;
    runStatistics.forEach(stat => {
        const row = document.createElement("tr");

        const counterCell = document.createElement("td");
        counterCell.textContent = ++runHistoryCounter;

        const degreeCell = document.createElement("td");
        degreeCell.textContent = stat.degree;

        const inputCell = document.createElement("td");
        inputCell.textContent = stat.inputVars;

        const resultCell = document.createElement("td");
        resultCell.textContent = stat.result;

        const cycleCell = document.createElement("td");
        cycleCell.textContent = stat.cycles;

        row.appendChild(counterCell);
        row.appendChild(degreeCell);
        row.appendChild(inputCell);
        row.appendChild(resultCell);
        row.appendChild(cycleCell);

        tableBody.appendChild(row);
    });

    // If no statistics available, show a placeholder
    if (!runStatistics || runStatistics.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5">No run history available.</td></tr>`;
    }
}

// Fetch run statistics from the server
async function setStatistics() {
    const request = {
        action: "getRunStatistic",
        data: { username: username } // assuming 'username' variable exists
    };

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok && result.data && Array.isArray(result.data.runStatistics)) {
            populateProgramStatisticTable(result.data.runStatistics);
        } else {
            showStatus(result.message || "Failed to load statistics.", "warning");
        }
    } catch (err) {
        console.error("Error fetching statistics:", err);
        showStatus("Server connection error.", "error");
    }
}

document.getElementById("backButton").addEventListener("click", async () => {

   window.location.href = `dashboard.html?username=${username}`;
});

document.getElementById("expandButton").addEventListener("click", async () => {
    let setDegree = parseInt(expandField.value.trim(), 10);

    if (setDegree >= maxDegree) {
        showAlert("Invalid degree", `Max degree is ${maxDegree}`, "error");
        return;
    }

    // Clear instruction history table
    const historyTableBody = document.querySelector("#historyInstructionTable tbody");
    if (historyTableBody) {
        historyTableBody.innerHTML = "";
    }

    const request = {
        action: "expandProgram",
        data: {
            username: username,  // assuming `username` is globally available
            degree: 1            // always 1 as in Java code
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
            showStatus(result.message, "info");
            loadProgramInstructions();
            loadHighlightComboBox();
            setRangeDegree(setDegree + 1);
        } else {
            showStatus(result.message || "Failed to expand program", "warning");
        }
    } catch (err) {
        console.error("Error expanding program:", err);
        showStatus("Server connection error: " + err.message, "error");
    }

});

document.getElementById("collapseButton").addEventListener("click", async () => {
    let setDegree = parseInt(expandField.value.trim(), 10);

    if (setDegree === 0) {
        showAlert("Invalid degree", `Min degree is 0`, "error");
        return;
    }

    // Clear instruction history table
    const historyTableBody = document.querySelector("#historyInstructionTable tbody");
    if (historyTableBody) {
        historyTableBody.innerHTML = "";
    }

    const request = {
            action: "collapseProgram",
            data: {
                username: username
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
                showStatus(result.message, "info"); // info = INFORMATION equivalent
                loadProgramInstructions();
                loadHighlightComboBox();
                setRangeDegree(setDegree - 1); // adjust degree
            } else {
                showStatus(result.message || "Failed to collapse program", "warning");
            }
        } catch (err) {
            console.error("Error collapsing program:", err);
            showStatus("Server connection error: " + err.message, "error");
        }

});

highlightComboBox.addEventListener("change", () => {
    const highlightComboBox = document.getElementById("highlightComboBox");
    highlightText = highlightComboBox.value;
    // Refresh table to apply new highlight
    loadProgramInstructions();
});

async function loadHighlightComboBox() {
    const request = {
        action: "getHighlightOptions",
        data: { username: username } // assuming username is global
    };

    // Clear the combo box
    const highlightComboBox = document.getElementById("highlightComboBox");
    highlightComboBox.innerHTML = "";
    highlightText = "none";

    try {
        const response = await fetch("http://localhost:8080/api", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.ok && Array.isArray(result.data?.highlightOptions)) {
            const highlights = result.data.highlightOptions;

            highlights.forEach(opt => {
                const option = document.createElement("option");
                option.value = opt;
                option.textContent = opt;
                highlightComboBox.appendChild(option);
            });

            if (highlights.length > 0) {
                highlightComboBox.selectedIndex = 0;
                highlightText = highlights[0];
            }
        } else {
            showStatus(result.message || "Failed to load highlight options", "warning");
        }
    } catch (err) {
        console.error("Error loading highlight options:", err);
        showStatus("Server connection error: " + err.message, "error");
    }
}

function showAlert(title, message, type = "info") {
    let color;
    switch (type) {
        case "error": color = "red"; break;
        case "warning": color = "orange"; break;
        case "success": color = "green"; break;
        default: color = "blue";
    }

    // Create a custom alert div
    const alertBox = document.createElement("div");
    alertBox.style.position = "fixed";
    alertBox.style.top = "20px";
    alertBox.style.left = "50%";
    alertBox.style.transform = "translateX(-50%)";
    alertBox.style.backgroundColor = color;
    alertBox.style.color = "white";
    alertBox.style.padding = "10px 20px";
    alertBox.style.borderRadius = "8px";
    alertBox.style.boxShadow = "0 2px 6px rgba(0,0,0,0.2)";
    alertBox.style.zIndex = "1000";
    alertBox.style.fontWeight = "bold";
    alertBox.textContent = `${title}: ${message}`;
    document.body.appendChild(alertBox);

    // Remove after 3 seconds
    setTimeout(() => alertBox.remove(), 3000);

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