const params = new URLSearchParams(window.location.search);
const username = params.get("username");
const programName = params.get("programName");
const availableCredit = params.get("availableCredit");

const availCreditField = document.getElementById("availCredit");


if (username) {
    const userDisplay = document.getElementById("loggedInUser");
    if (userDisplay) {
        userDisplay.textContent = `${username}`;
        availCreditField.value = `${availableCredit}`;
      //  loadUserCredits(username);
     //   selectedUser = username;
     //   refreshAll();
    //    setInterval(refreshAll, 1000); // one second interval
    }
} else {
    console.warn("No username provided in URL");
    showStatus();("No username provided in URL", "error");
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