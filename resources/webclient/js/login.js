// webclient/js/login.js

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const usernameInput = document.getElementById("username");
    const errorMsg = document.getElementById("errorMsg");

    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault(); // prevent form from submitting normally
        errorMsg.textContent = ""; // clear previous error

        const username = usernameInput.value.trim();
        if (!username) {
            errorMsg.textContent = "Username cannot be empty";
            errorMsg.style.color = "orange";
            return;
        }

        try {
            const response = await sendRequest("login", { username });
            if (response.ok) {
                // login successful → redirect to dashboard page
                // You can create a dashboard.html similar to Dashboard.fxml
                window.location.href = "dashboard.html?username=" + encodeURIComponent(username);
            } else {
                errorMsg.textContent = response.message;
                errorMsg.style.color = "red";
            }
        } catch (err) {
            errorMsg.textContent = "Server error: " + err.message;
            errorMsg.style.color = "red";
        }
    });
});

// Helper function to send request to your server API
async function sendRequest(action, data) {
    const url = "http://localhost:8080/api";
    const payload = {
        action: action,
        data: data
    };

    const resp = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });

    if (!resp.ok) {
        throw new Error("HTTP error " + resp.status);
    }

    const json = await resp.json();
    return json; // this matches BaseResponse structure
}
