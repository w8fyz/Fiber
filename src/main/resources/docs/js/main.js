// Fetch and display endpoints
async function loadEndpoints() {
    try {
        const response = await fetch('/docs/api');
        const data = await response.json();
        console.log('API Response:', data); // Debug log
        displayEndpoints(data);
        addExpandCollapseAll(); // Add the expand/collapse all button
    } catch (error) {
        console.error('Error loading endpoints:', error);
    }
}

// Display endpoints in the UI
function displayEndpoints(data) {
    const container = document.querySelector('.endpoints-container');
    container.innerHTML = '';

    if (!data.endpoints || !Array.isArray(data.endpoints)) {
        console.error('Invalid API response format:', data);
        return;
    }

    // Sort endpoints by path and method
    const sortedEndpoints = data.endpoints.sort((a, b) => {
        const pathCompare = a.path.localeCompare(b.path);
        if (pathCompare !== 0) return pathCompare;
        return a.method.localeCompare(b.method);
    });

    sortedEndpoints.forEach(endpoint => {
        const card = createEndpointCard(endpoint);
        container.appendChild(card);
    });
}

// Generate example JSON based on fields
function generateExampleJSON(fields) {
    if (!fields || fields.length === 0) {
        return JSON.stringify({}, null, 2);
    }

    const example = {};
    fields.forEach(field => {
        const type = field.type.toLowerCase();
        switch (type) {
            case 'string':
                example[field.name] = `example_${field.name}`;
                break;
            case 'int':
            case 'integer':
            case 'long':
                example[field.name] = 0;
                break;
            case 'double':
            case 'float':
                example[field.name] = 0.0;
                break;
            case 'boolean':
                example[field.name] = false;
                break;
            case 'list':
            case 'arraylist':
                example[field.name] = [];
                break;
            case 'map':
            case 'hashmap':
                example[field.name] = {};
                break;
            default:
                example[field.name] = null;
        }
    });

    return JSON.stringify(example, null, 2);
}

// Create an endpoint card
function createEndpointCard(endpoint) {
    const card = document.createElement('div');
    card.className = 'endpoint-card collapsed'; // Start collapsed

    const header = document.createElement('div');
    header.className = 'endpoint-header';

    // Method badge
    const methodBadge = document.createElement('span');
    methodBadge.className = `method ${endpoint.method.toLowerCase()}`;
    methodBadge.textContent = endpoint.method;

    // Path
    const pathSpan = document.createElement('span');
    pathSpan.className = 'path';
    pathSpan.textContent = endpoint.path;

    // Test button
    const testButton = document.createElement('button');
    testButton.className = 'btn btn-test';
    testButton.innerHTML = '<i class="fas fa-vial"></i> Test';
    testButton.onclick = (e) => {
        e.stopPropagation(); // Prevent header click from triggering
        openTestModal(endpoint);
    };

    header.appendChild(methodBadge);
    header.appendChild(pathSpan);
    header.appendChild(testButton);

    // Add click handler for collapsing/expanding
    header.addEventListener('click', () => {
        card.classList.toggle('collapsed');
    });

    // Details section
    const detailsSection = document.createElement('div');
    detailsSection.className = 'endpoint-details';

    // Description
    if (endpoint.description) {
        const description = document.createElement('p');
        description.className = 'description';
        description.textContent = endpoint.description;
        detailsSection.appendChild(description);
    }

    // Parameters
    if (endpoint.parameters && endpoint.parameters.length > 0) {
        const paramsSection = document.createElement('div');
        paramsSection.className = 'parameters';
        paramsSection.innerHTML = '<h3>Parameters</h3>';
        
        endpoint.parameters.forEach(param => {
            const paramRow = document.createElement('div');
            paramRow.className = 'parameter';
            const required = param.required ? ' (Required)' : ' (Optional)';
            const validationMessages = param.validationMessages && param.validationMessages.length > 0 
                ? `\nValidation: ${param.validationMessages.join(', ')}` 
                : '';
            
            paramRow.innerHTML = `
                <span class="parameter-name">${param.name}${required}</span>
                <span class="parameter-type">${param.type}</span>
                <span class="parameter-description">${validationMessages}</span>
            `;
            paramsSection.appendChild(paramRow);
        });
        detailsSection.appendChild(paramsSection);
    }

    // Request Body
    if (endpoint.requestBodyType) {
        const requestBodySection = document.createElement('div');
        requestBodySection.className = 'request-body';
        requestBodySection.innerHTML = `
            <h3>Request Body</h3>
            <div class="type">${endpoint.requestBodyType}</div>
            ${endpoint.requestBodyExample ? updateRequestBodyExample(endpoint.requestBodyExample) : ''}
        `;
        detailsSection.appendChild(requestBodySection);
    }

    // Response
    if (endpoint.responseType) {
        const responseSection = document.createElement('div');
        responseSection.className = 'response-type';
        responseSection.innerHTML = `
            <h3>Response</h3>
            <div class="type">${endpoint.responseType}</div>
            ${endpoint.responseExample ? updateResponseExample(endpoint.responseExample) : ''}
        `;
        detailsSection.appendChild(responseSection);
    }

    card.appendChild(header);
    card.appendChild(detailsSection);

    // Add keyboard accessibility
    header.setAttribute('role', 'button');
    header.setAttribute('tabindex', '0');
    header.setAttribute('aria-expanded', 'false');
    header.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            card.classList.toggle('collapsed');
            header.setAttribute('aria-expanded', !card.classList.contains('collapsed'));
        }
    });

    return card;
}

// Modal functionality
const modal = document.getElementById('testModal');
const closeBtn = document.querySelector('.close');
const testMethod = document.getElementById('testMethod');
const testPath = document.getElementById('testPath');
const testParams = document.getElementById('testParams');
const testBody = document.getElementById('testBody');
const testBodyInput = document.getElementById('testBodyInput');
const sendTest = document.getElementById('sendTest');
const testResponse = document.getElementById('testResponse');

let currentEndpoint = null;

function openTestModal(endpoint) {
    currentEndpoint = endpoint;
    modal.style.display = 'block';
    
    // Set method and path
    testMethod.textContent = endpoint.method;
    testMethod.className = `method ${endpoint.method.toLowerCase()}`;
    testPath.textContent = endpoint.path;

    // Clear previous content
    testParams.innerHTML = '';
    testBody.style.display = 'none';
    testBodyInput.value = '';
    testResponse.textContent = '';

    // Add parameter inputs
    if (endpoint.parameters && endpoint.parameters.length > 0) {
        endpoint.parameters.forEach(param => {
            const paramGroup = document.createElement('div');
            paramGroup.className = 'form-group';
            const required = param.required ? ' *' : '';
            const validationMessages = param.validationMessages && param.validationMessages.length > 0 
                ? `\nValidation: ${param.validationMessages.join(', ')}` 
                : '';
            
            paramGroup.innerHTML = `
                <label>${param.name} (${param.type})${required}</label>
                <input type="text" name="${param.name}" placeholder="${validationMessages || ''}">
            `;
            testParams.appendChild(paramGroup);
        });
    }

    // Show request body for POST/PUT methods
    if ((endpoint.method === 'POST' || endpoint.method === 'PUT') && endpoint.requestBodyExample) {
        testBody.style.display = 'block';
        testBodyInput.value = JSON.stringify(endpoint.requestBodyExample, null, 2);
    }
}

// Close modal
closeBtn.onclick = () => {
    modal.style.display = 'none';
};

window.onclick = (event) => {
    if (event.target === modal) {
        modal.style.display = 'none';
    }
};

// Theme handling
const themeToggle = document.getElementById('themeToggle');
const html = document.documentElement;
const themeIcon = themeToggle.querySelector('i');

function setTheme(isDark) {
    const theme = isDark ? 'dark' : 'light';
    html.setAttribute('data-theme', theme);
    themeIcon.className = isDark ? 'fas fa-sun' : 'fas fa-moon';
    localStorage.setItem('theme', theme);
    
    // Update meta theme-color
    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
        metaThemeColor.setAttribute('content', isDark ? '#1a1a1a' : '#ffffff');
    }
}

// Initialize theme based on system preference or saved preference
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
const savedTheme = localStorage.getItem('theme');
const initialTheme = savedTheme || (prefersDark ? 'dark' : 'light');
setTheme(initialTheme === 'dark');

// Listen for system theme changes
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    if (!localStorage.getItem('theme')) {
        setTheme(e.matches);
    }
});

themeToggle.addEventListener('click', () => {
    const isDark = html.getAttribute('data-theme') === 'light';
    setTheme(isDark);
});

// Bearer token handling
const bearerToken = document.getElementById('bearerToken');
const toggleToken = document.getElementById('toggleToken');
const tokenEnabled = document.getElementById('tokenEnabled');

// Initialize token
const savedToken = localStorage.getItem('bearerToken') || '';
const savedTokenEnabled = localStorage.getItem('tokenEnabled') === 'true';
bearerToken.value = savedToken;
tokenEnabled.checked = savedTokenEnabled;

bearerToken.addEventListener('input', () => {
    localStorage.setItem('bearerToken', bearerToken.value);
});

toggleToken.addEventListener('click', () => {
    const type = bearerToken.type === 'password' ? 'text' : 'password';
    bearerToken.type = type;
    toggleToken.querySelector('i').className = `fas fa-eye${type === 'password' ? '' : '-slash'}`;
});

tokenEnabled.addEventListener('change', () => {
    localStorage.setItem('tokenEnabled', tokenEnabled.checked);
});

// Headers handling
const addHeader = document.getElementById('addHeader');
const requestHeaders = document.getElementById('requestHeaders');

function createHeaderRow() {
    const row = document.createElement('div');
    row.className = 'header-row';
    row.innerHTML = `
        <input type="text" class="header-key" placeholder="Header Name">
        <input type="text" class="header-value" placeholder="Header Value">
        <button class="btn btn-icon remove-header">
            <i class="fas fa-times"></i>
        </button>
    `;
    
    row.querySelector('.remove-header').addEventListener('click', () => {
        row.remove();
    });
    
    return row;
}

addHeader.addEventListener('click', () => {
    requestHeaders.insertBefore(createHeaderRow(), addHeader);
});

// JSON formatting and copying
const formatJson = document.getElementById('formatJson');
const copyJson = document.getElementById('copyJson');
const copyResponse = document.getElementById('copyResponse');

formatJson.addEventListener('click', () => {
    try {
        const parsed = JSON.parse(testBodyInput.value);
        testBodyInput.value = JSON.stringify(parsed, null, 2);
    } catch (e) {
        // Invalid JSON, ignore
    }
});

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
    }).catch(err => {
        console.error('Failed to copy text:', err);
    });
}

copyJson.addEventListener('click', () => {
    copyToClipboard(testBodyInput.value);
});

copyResponse.addEventListener('click', () => {
    copyToClipboard(document.getElementById('testResponse').textContent);
});

// Update sendTest function to include headers and bearer token
async function sendTestRequest() {
    if (!currentEndpoint) return;

    const headers = {
        'Content-Type': 'application/json'
    };

    // Add bearer token if enabled
    if (tokenEnabled.checked && bearerToken.value) {
        headers['Authorization'] = `Bearer ${bearerToken.value}`;
    }

    // Add custom headers
    requestHeaders.querySelectorAll('.header-row').forEach(row => {
        const key = row.querySelector('.header-key').value;
        const value = row.querySelector('.header-value').value;
        if (key && value) {
            headers[key] = value;
        }
    });

    // Build URL with parameters
    let url = currentEndpoint.path;
    const params = new URLSearchParams();
    testParams.querySelectorAll('input').forEach(input => {
        if (input.value) {
            params.append(input.name, input.value);
        }
    });
    if (params.toString()) {
        url += '?' + params.toString();
    }

    const startTime = performance.now();
    const responseContainer = document.querySelector('.test-response');
    
    try {
        const response = await fetch(url, {
            method: currentEndpoint.method,
            headers: headers,
            body: testBody.style.display !== 'none' ? testBodyInput.value : undefined
        });

        const endTime = performance.now();
        const responseTime = Math.round(endTime - startTime);
        const responseData = await response.json();

        // Create response HTML with syntax highlighting
        const responseHtml = `
            <div class="response-info">
                <span class="response-status ${response.ok ? 'success' : 'error'}">${response.status} ${response.statusText}</span>
                <span class="response-time">${responseTime}ms</span>
            </div>
            <div class="response-controls">
                <button class="btn btn-secondary" onclick="copyResponse()">
                    <i class="fas fa-copy"></i> Copy
                </button>
            </div>
            <div class="example">
                <pre>${syntaxHighlightJson(JSON.stringify(responseData, null, 2))}</pre>
            </div>
        `;
        
        responseContainer.innerHTML = responseHtml;
    } catch (error) {
        responseContainer.innerHTML = `
            <div class="response-info">
                <span class="response-status error">Error</span>
            </div>
            <div class="example">
                <pre class="error">${error.message}</pre>
            </div>
        `;
    }
}

// Update event listener
document.getElementById('sendTest').addEventListener('click', sendTestRequest);

// Load endpoints on page load
loadEndpoints();

// Add this function for JSON syntax highlighting
function syntaxHighlightJson(json) {
    if (typeof json !== 'string') {
        json = JSON.stringify(json, null, 2);
    }
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        let cls = 'json-number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'json-key';
                // Keep the colon as part of the key span
                return '<span class="' + cls + '">' + match + '</span>';
            } else {
                cls = 'json-string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'json-boolean';
        } else if (/null/.test(match)) {
            cls = 'json-null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    })
    // Add highlighting for brackets and braces
    .replace(/[{}\[\]]/g, function(match) {
        return '<span class="json-bracket">' + match + '</span>';
    });
}

// Update the function that displays request body example
function updateRequestBodyExample(example) {
    if (!example) return '';
    const highlighted = syntaxHighlightJson(example);
    return `<div class="example">
        <pre>${highlighted}</pre>
    </div>`;
}

// Update the function that displays response example
function updateResponseExample(example) {
    if (!example) return '';
    const highlighted = syntaxHighlightJson(example);
    return `<div class="example">
        <pre>${highlighted}</pre>
    </div>`;
}

// Update the test response display
function displayTestResponse(response, status, time) {
    const responseContainer = document.querySelector('.test-response');
    const statusClass = status >= 200 && status < 300 ? 'success' : 'error';
    
    let responseHtml = `
        <div class="response-info">
            <span class="response-status ${statusClass}">${status}</span>
            <span class="response-time">${time}ms</span>
        </div>
        <div class="response-controls">
            <button class="btn btn-secondary" onclick="copyResponse()">
                <i class="fas fa-copy"></i> Copy
            </button>
        </div>
        <pre>${syntaxHighlightJson(response)}</pre>
    `;
    
    responseContainer.innerHTML = responseHtml;
}

// Add this function to expand/collapse all endpoints
function addExpandCollapseAll() {
    const container = document.querySelector('.endpoints-container');
    const toggleButton = document.createElement('button');
    toggleButton.className = 'btn btn-secondary toggle-all';
    toggleButton.innerHTML = '<i class="fas fa-expand-alt"></i> Expand All';
    
    let allExpanded = false;
    
    toggleButton.addEventListener('click', () => {
        const cards = document.querySelectorAll('.endpoint-card');
        allExpanded = !allExpanded;
        
        cards.forEach(card => {
            if (allExpanded) {
                card.classList.remove('collapsed');
                card.querySelector('.endpoint-header').setAttribute('aria-expanded', 'true');
            } else {
                card.classList.add('collapsed');
                card.querySelector('.endpoint-header').setAttribute('aria-expanded', 'false');
            }
        });
        
        toggleButton.innerHTML = allExpanded ? 
            '<i class="fas fa-compress-alt"></i> Collapse All' : 
            '<i class="fas fa-expand-alt"></i> Expand All';
    });
    
    // Insert the button before the container
    container.parentNode.insertBefore(toggleButton, container);
} 