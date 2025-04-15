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

    // Path Variables
    if (endpoint.pathVariables && endpoint.pathVariables.length > 0) {
        const pathVarsSection = document.createElement('div');
        pathVarsSection.className = 'path-variables';
        pathVarsSection.innerHTML = '<h3>Path Variables</h3>';
        
        endpoint.pathVariables.forEach(pathVar => {
            const pathVarRow = document.createElement('div');
            pathVarRow.className = 'path-variable';
            
            pathVarRow.innerHTML = `
                <span class="path-variable-name">${pathVar.name}</span>
                <span class="path-variable-type">${pathVar.type}</span>
            `;
            pathVarsSection.appendChild(pathVarRow);
        });
        detailsSection.appendChild(pathVarsSection);
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

    // Add path variable inputs
    if (endpoint.pathVariables && endpoint.pathVariables.length > 0) {
        const pathVarsSection = document.createElement('div');
        pathVarsSection.className = 'form-group';
        pathVarsSection.innerHTML = '<h3>Path Variables</h3>';
        
        endpoint.pathVariables.forEach(pathVar => {
            const pathVarGroup = document.createElement('div');
            pathVarGroup.className = 'form-group';
            
            pathVarGroup.innerHTML = `
                <label>${pathVar.name} (${pathVar.type}) *</label>
                <input type="text" name="path_${pathVar.name}" placeholder="Enter ${pathVar.name} value">
            `;
            pathVarsSection.appendChild(pathVarGroup);
        });
        
        testParams.appendChild(pathVarsSection);
    }

    // Add parameter inputs
    if (endpoint.parameters && endpoint.parameters.length > 0) {
        const paramsSection = document.createElement('div');
        paramsSection.className = 'form-group';
        paramsSection.innerHTML = '<h3>Query Parameters</h3>';
        
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
            paramsSection.appendChild(paramGroup);
        });
        
        testParams.appendChild(paramsSection);
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
    
    const method = currentEndpoint.method;
    let path = currentEndpoint.path;
    
    // Get path variables
    const pathVarInputs = document.querySelectorAll('input[name^="path_"]');
    pathVarInputs.forEach(input => {
        const pathVarName = input.name.substring(5); // Remove 'path_' prefix
        const pathVarValue = input.value.trim();
        if (pathVarValue) {
            path = path.replace(`{${pathVarName}}`, pathVarValue);
        }
    });
    
    // Get query parameters
    const params = {};
    const paramInputs = document.querySelectorAll('input[name]:not([name^="path_"])');
    paramInputs.forEach(input => {
        const value = input.value.trim();
        if (value) {
            params[input.name] = value;
        }
    });
    
    // Build URL with query parameters
    let url = path;
    if (Object.keys(params).length > 0) {
        const queryString = new URLSearchParams(params).toString();
        url += `?${queryString}`;
    }
    
    // Get request body
    let body = null;
    if (testBody.style.display !== 'none' && testBodyInput.value.trim()) {
        try {
            body = JSON.parse(testBodyInput.value);
        } catch (e) {
            testResponse.textContent = 'Invalid JSON in request body';
            return;
        }
    }
    
    // Get headers
    const headers = {};
    const headerRows = document.querySelectorAll('.header-row');
    headerRows.forEach(row => {
        const key = row.querySelector('.header-key').value.trim();
        const value = row.querySelector('.header-value').value.trim();
        if (key && value) {
            headers[key] = value;
        }
    });
    
    // Add authorization header if enabled
    const tokenEnabled = document.getElementById('tokenEnabled').checked;
    const bearerToken = document.getElementById('bearerToken').value.trim();
    if (tokenEnabled && bearerToken) {
        headers['Authorization'] = `Bearer ${bearerToken}`;
    }
    
    // Add content-type header for POST/PUT requests with body
    if (body && (method === 'POST' || method === 'PUT')) {
        headers['Content-Type'] = 'application/json';
    }
    
    // Add credentials to include cookies
    const credentials = 'include';
    
    // Send request
    const startTime = performance.now();
    try {
        const response = await fetch(url, {
            method,
            headers,
            body: body ? JSON.stringify(body) : null,
            credentials,
            redirect: 'manual' // Don't follow redirects automatically
        });
        
        const endTime = performance.now();
        const responseTime = Math.round(endTime - startTime);
        
        // Check if this is a redirect response
        if (response.status >= 300 && response.status < 400) {
            const location = response.headers.get('Location');
            if (location) {
                displayRedirectResponse(response, responseTime, location);
                return;
            }
        }
        
        const responseData = await response.text();
        let formattedResponse;
        
        try {
            // Try to parse as JSON
            const jsonResponse = JSON.parse(responseData);
            formattedResponse = JSON.stringify(jsonResponse, null, 2);
        } catch (e) {
            // If not JSON, just use the text
            formattedResponse = responseData;
        }
        
        displayTestResponse(formattedResponse, response.status, responseTime);
        
        // Reload cookies after request
        loadCookies();
    } catch (error) {
        const endTime = performance.now();
        const responseTime = Math.round(endTime - startTime);
        displayTestResponse(`Error: ${error.message}`, 0, responseTime);
    }
}

// Display redirect response with iframe
function displayRedirectResponse(response, responseTime, location) {
    const responseContainer = document.querySelector('.test-response');
    const statusClass = 'redirect';
    
    // Create a container for the redirect information and iframe
    const redirectContainer = document.createElement('div');
    redirectContainer.className = 'redirect-container';
    
    // Add redirect information
    const redirectInfo = document.createElement('div');
    redirectInfo.className = 'redirect-info';
    redirectInfo.innerHTML = `
        <div class="response-info">
            <span class="response-status ${statusClass}">${response.status} Redirect</span>
            <span class="response-time">${responseTime}ms</span>
        </div>
        <div class="redirect-location">
            <strong>Redirecting to:</strong> <a href="${location}" target="_blank">${location}</a>
        </div>
    `;
    
    // Create iframe container
    const iframeContainer = document.createElement('div');
    iframeContainer.className = 'iframe-container';
    
    // Create iframe
    const iframe = document.createElement('iframe');
    iframe.src = location;
    iframe.className = 'redirect-iframe';
    iframe.title = 'Redirect Preview';
    
    // Add controls for the iframe
    const iframeControls = document.createElement('div');
    iframeControls.className = 'iframe-controls';
    iframeControls.innerHTML = `
        <button class="btn btn-secondary open-in-new-tab">
            <i class="fas fa-external-link-alt"></i> Open in New Tab
        </button>
        <button class="btn btn-secondary reload-iframe">
            <i class="fas fa-sync-alt"></i> Reload
        </button>
    `;
    
    // Add event listeners for the controls
    iframeControls.querySelector('.open-in-new-tab').addEventListener('click', () => {
        window.open(location, '_blank');
    });
    
    iframeControls.querySelector('.reload-iframe').addEventListener('click', () => {
        iframe.src = iframe.src;
    });
    
    // Assemble the redirect container
    iframeContainer.appendChild(iframe);
    iframeContainer.appendChild(iframeControls);
    
    redirectContainer.appendChild(redirectInfo);
    redirectContainer.appendChild(iframeContainer);
    
    // Clear previous content and add the redirect container
    responseContainer.innerHTML = '';
    responseContainer.appendChild(redirectContainer);
    
    // Reload cookies after request
    loadCookies();
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

// Cookie management
const cookiesModal = document.getElementById('cookiesModal');
const cookiesToggle = document.getElementById('cookiesToggle');
const cookiesList = document.querySelector('.cookies-list');
const addCookieBtn = document.getElementById('addCookie');
const cookieNameInput = document.getElementById('cookieName');
const cookieValueInput = document.getElementById('cookieValue');
const cookieDomainInput = document.getElementById('cookieDomain');
const cookiePathInput = document.getElementById('cookiePath');
const cookieExpiresInput = document.getElementById('cookieExpires');
const cookieSecureInput = document.getElementById('cookieSecure');
const cookieHttpOnlyInput = document.getElementById('cookieHttpOnly');
const cookieSameSiteInput = document.getElementById('cookieSameSite');

// Open cookies modal
cookiesToggle.addEventListener('click', () => {
    cookiesModal.style.display = 'block';
    loadCookies();
});

// Close cookies modal
cookiesModal.querySelector('.close').addEventListener('click', () => {
    cookiesModal.style.display = 'none';
});

// Close modal when clicking outside
window.addEventListener('click', (event) => {
    if (event.target === cookiesModal) {
        cookiesModal.style.display = 'none';
    }
});

// Load and display cookies
function loadCookies() {
    cookiesList.innerHTML = '';
    const cookies = document.cookie.split(';');
    
    if (cookies.length === 1 && cookies[0] === '') {
        cookiesList.innerHTML = '<p class="no-cookies">No cookies found</p>';
        return;
    }
    
    cookies.forEach(cookie => {
        if (cookie.trim() === '') return;
        
        const [name, value] = cookie.split('=').map(part => part.trim());
        const cookieItem = document.createElement('div');
        cookieItem.className = 'cookie-item';
        
        cookieItem.innerHTML = `
            <div class="cookie-info">
                <span class="cookie-name">${name}</span>
                <span class="cookie-value">${value}</span>
            </div>
            <div class="cookie-actions">
                <button class="btn btn-icon delete-cookie" data-name="${name}">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        
        cookiesList.appendChild(cookieItem);
    });
    
    // Add event listeners to delete buttons
    document.querySelectorAll('.delete-cookie').forEach(button => {
        button.addEventListener('click', (e) => {
            const name = e.currentTarget.getAttribute('data-name');
            deleteCookie(name);
        });
    });
}

// Add a new cookie
addCookieBtn.addEventListener('click', () => {
    const name = cookieNameInput.value.trim();
    const value = cookieValueInput.value.trim();
    
    if (!name || !value) {
        alert('Cookie name and value are required');
        return;
    }
    
    let cookieString = `${name}=${value}`;
    
    // Add optional attributes
    if (cookieDomainInput.value.trim()) {
        cookieString += `; domain=${cookieDomainInput.value.trim()}`;
    }
    
    if (cookiePathInput.value.trim()) {
        cookieString += `; path=${cookiePathInput.value.trim()}`;
    }
    
    if (cookieExpiresInput.value) {
        const date = new Date(cookieExpiresInput.value);
        cookieString += `; expires=${date.toUTCString()}`;
    }
    
    if (cookieSecureInput.checked) {
        cookieString += '; secure';
    }
    
    if (cookieHttpOnlyInput.checked) {
        cookieString += '; HttpOnly';
    }
    
    if (cookieSameSiteInput.checked) {
        cookieString += '; SameSite=Strict';
    }
    
    // Set the cookie
    document.cookie = cookieString;
    
    // Reload cookies
    loadCookies();
    
    // Reset form
    cookieNameInput.value = '';
    cookieValueInput.value = '';
    cookieDomainInput.value = '';
    cookiePathInput.value = '/';
    cookieExpiresInput.value = '';
    cookieSecureInput.checked = false;
    cookieHttpOnlyInput.checked = false;
    cookieSameSiteInput.checked = false;
});

// Delete a cookie
function deleteCookie(name) {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`;
    loadCookies();
}

// CSRF token handling
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

// Add CSRF token to all non-GET requests
document.addEventListener('DOMContentLoaded', () => {
    const token = getCookie('XSRF-TOKEN');
    if (token) {
        // Add CSRF token to all fetch requests
        const originalFetch = window.fetch;
        window.fetch = function(url, options = {}) {
            if (!options.method || options.method.toUpperCase() !== 'GET') {
                options.headers = {
                    ...options.headers,
                    'X-CSRF-TOKEN': token
                };
            }
            return originalFetch(url, options);
        };

        // Add CSRF token to all XHR requests
        const originalXHROpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) {
            const xhr = this;
            originalXHROpen.apply(xhr, arguments);
            if (method.toUpperCase() !== 'GET') {
                xhr.setRequestHeader('X-CSRF-TOKEN', token);
            }
        };
    }
}); 