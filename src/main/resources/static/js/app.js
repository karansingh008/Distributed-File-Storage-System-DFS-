const API_BASE = 'http://localhost:8080/api';

function getAuthHeader() {
    return { 'Authorization': 'Basic ' + localStorage.getItem('dfs_auth') };
}

const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        const auth = btoa(email + ':' + password);

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': 'Basic ' + auth },
                body: JSON.stringify({ email, password })
            });

            if (res.ok) {
                localStorage.setItem('dfs_auth', auth);
                window.location.href = 'dashboard.html';
            } else {
                document.getElementById('authMessage').innerText = 'Login failed';
            }
        } catch (err) {
            document.getElementById('authMessage').innerText = 'Error: ' + err.message;
        }
    });
}

const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;

        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (res.ok) {
                alert('Registration successful! Please login.');
                document.getElementById('login-tab').click();
            } else {
                const text = await res.text();
                document.getElementById('authMessage').innerText = 'Register failed: ' + text;
            }
        } catch (err) {
            document.getElementById('authMessage').innerText = 'Error: ' + err.message;
        }
    });
}

function logout() {
    localStorage.removeItem('dfs_auth');
    window.location.href = 'index.html';
}

if (window.location.pathname.endsWith('dashboard.html')) {
    if (!localStorage.getItem('dfs_auth')) {
        window.location.href = 'index.html';
    } else {
        loadFiles();
    }
}

function formatSize(bytes) {
    if (bytes === 0 || bytes === null || bytes === undefined) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

async function loadFiles() {
    try {
        const res = await fetch(`${API_BASE}/files`, { headers: getAuthHeader() });
        if (res.status === 401) return logout();
        const files = await res.json();

        const tbody = document.getElementById('fileTableBody');
        tbody.innerHTML = '';

        let totalFiles = 0;

        files.forEach((file, index) => {
            totalFiles++;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <th scope="row">${index + 1}</th>
                <td><a href="#" onclick="showVersions(${file.id}, '${file.name}')" class="fw-bold text-decoration-none">${file.name}</a></td>
                <td>${formatSize(file.size)}</td>
                <td>${new Date(file.createdAt).toLocaleDateString()}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="showVersions(${file.id}, '${file.name}')"><i class="fas fa-code-branch"></i> Versions</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        await loadStats();

    } catch (err) {
        console.error(err);
    }
}

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/files/stats`, { headers: getAuthHeader() });
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('totalFilesCount').innerText = stats.totalFiles;
            document.getElementById('storageUsed').innerText = formatSize(stats.usedStorageBytes);
        }
    } catch (err) {
        console.error('Failed to load stats', err);
    }
}

async function showVersions(fileId, fileName) {
    try {
        const res = await fetch(`${API_BASE}/files/${fileId}/versions`, { headers: getAuthHeader() });
        if (!res.ok) throw new Error('Failed to fetch versions');
        const versions = await res.json();

        const titleEl = document.getElementById('versionsModalTitle');
        if (titleEl) titleEl.innerText = 'Versions of ' + fileName;

        const tbody = document.getElementById('versionsTableBody');
        tbody.innerHTML = '';
        versions.forEach(v => {
            const date = new Date(v.uploadTime).toLocaleDateString() + ' ' + new Date(v.uploadTime).toLocaleTimeString();

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><span class="badge bg-secondary">v${v.versionNo}</span></td>
                <td>${date}</td>
                <td>${formatSize(v.sizeBytes)}</td>
                <td>
                    <a href="#" class="btn btn-sm btn-success me-2 version-download-btn"><i class="fas fa-download"></i> Download</a>
                    <button class="btn btn-sm btn-danger" onclick="deleteVersion(${v.id}, ${fileId}, '${fileName}')"><i class="fas fa-trash-alt"></i></button>
                </td>
            `;

            tr.querySelector('.version-download-btn').onclick = (e) => {
                e.preventDefault();
                downloadFile(v.id, v.fileName || fileName);
            };
            tbody.appendChild(tr);
        });

        const trashBtn = document.getElementById('trashFileBtn');
        trashBtn.onclick = async () => {
            if (!confirm('Move this file to trash?')) return;
            try {
                const tRes = await fetch(`${API_BASE}/files/${fileId}/trash`, {
                    method: 'POST',
                    headers: getAuthHeader()
                });
                if (tRes.ok) {
                    const modal = bootstrap.Modal.getInstance(document.getElementById('versionsModal'));
                    if (modal) modal.hide();
                    loadFiles();
                    loadStats();
                } else {
                    alert('Failed to move file to trash');
                }
            } catch (err) {
                console.error(err);
            }
        };

        new bootstrap.Modal(document.getElementById('versionsModal')).show();
    } catch (err) {
        console.error(err);
        alert('Error loading versions: ' + err.message);
    }
}

async function deleteVersion(versionId, fileId, fileName) {
    if (!confirm('Are you sure you want to delete this version?')) return;
    try {
        const res = await fetch(`${API_BASE}/files/versions/${versionId}`, {
            method: 'DELETE',
            headers: getAuthHeader()
        });
        if (res.ok) {
            const vRes = await fetch(`${API_BASE}/files/${fileId}/versions`, { headers: getAuthHeader() });
            const vData = await vRes.json();

            if (vData.length === 0) {
                loadFiles();
                loadStats();
                const modal = bootstrap.Modal.getInstance(document.getElementById('versionsModal'));
                if (modal) modal.hide();
            } else {
                showVersions(fileId, fileName);
                loadFiles();
                loadStats();
            }
        } else {
            alert('Failed to delete version');
        }
    } catch (err) {
        console.error(err);
    }
}

async function downloadFile(versionId, filename) {
    try {
        const res = await fetch(`${API_BASE}/files/download/${versionId}`, { headers: getAuthHeader() });
        if (!res.ok) throw new Error('Download failed');
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || 'downloaded_file';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
    } catch (err) {
        alert('Download error: ' + err.message);
    }
}

async function uploadFile(file) {
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const progressContainer = document.getElementById('uploadProgressContainer');
    const progressBar = document.getElementById('uploadProgressBar');
    const percentSpan = document.getElementById('uploadPercentage');
    const fileNameSpan = document.getElementById('uploadingFileName');

    if (progressContainer) {
        progressContainer.classList.remove('d-none');
        progressBar.style.width = '0%';
        percentSpan.innerText = '0%';
        fileNameSpan.innerText = file.name;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API_BASE}/files/upload`, true);
    xhr.setRequestHeader('Authorization', 'Basic ' + localStorage.getItem('dfs_auth'));

    xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100);
            if (progressBar) progressBar.style.width = percent + '%';
            if (percentSpan) percentSpan.innerText = percent + '%';
        }
    };

    xhr.onload = () => {
        if (xhr.status === 200) {
            alert('Upload successful!');
            loadFiles();
        } else {
            alert(`Upload failed: ${xhr.status} ${xhr.statusText}`);
        }
        if (progressContainer) progressContainer.classList.add('d-none');
    };

    xhr.onerror = () => {
        alert('Upload error');
        if (progressContainer) progressContainer.classList.add('d-none');
    };

    xhr.send(formData);
}

window.currentView = 'files';

function switchView(view) {
    window.currentView = view;

    document.querySelectorAll('.list-group-item').forEach(el => el.classList.remove('active'));
    document.getElementById('nav-' + view).classList.add('active');

    if (view === 'files') {
        document.getElementById('filesView').classList.remove('d-none');
        document.getElementById('trashView').classList.add('d-none');
        loadFiles();
    } else if (view === 'trash') {
        document.getElementById('filesView').classList.add('d-none');
        document.getElementById('trashView').classList.remove('d-none');
        loadTrash();
    }
}

async function loadTrash() {
    try {
        const res = await fetch(`${API_BASE}/files/trash`, { headers: getAuthHeader() });
        if (res.status === 401) return logout();
        const files = await res.json();

        const tbody = document.getElementById('trashTableBody');
        tbody.innerHTML = '';

        if (files.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">Trash is empty</td></tr>';
            return;
        }

        files.forEach((file, index) => {
            const date = file.deletedAt ? new Date(file.deletedAt).toLocaleDateString() : '-';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${file.name}</td>
                <td>${date}</td>
                <td>
                    <button class="btn btn-sm btn-success me-2" onclick="restoreFile(${file.id})"><i class="fas fa-trash-restore"></i> Restore</button>
                    <button class="btn btn-sm btn-danger" onclick="deletePermanent(${file.id})"><i class="fas fa-times"></i> Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error(err);
    }
}

async function trashFile(id) {
    if (!confirm('Move this file to trash?')) return;
    try {
        const res = await fetch(`${API_BASE}/files/${id}/trash`, {
            method: 'POST',
            headers: getAuthHeader()
        });
        if (res.ok) {
            loadFiles();
            loadStats();
        } else {
            alert('Failed to move to trash');
        }
    } catch (err) {
        console.error(err);
    }
}

async function restoreFile(id) {
    try {
        const res = await fetch(`${API_BASE}/files/${id}/restore`, {
            method: 'POST',
            headers: getAuthHeader()
        });
        if (res.ok) {
            loadTrash();
            loadStats();
        } else {
            alert('Failed to restore file');
        }
    } catch (err) {
        console.error(err);
    }
}

async function deletePermanent(id) {
    if (!confirm('Permanently delete this file? This cannot be undone.')) return;
    try {
        const res = await fetch(`${API_BASE}/files/${id}`, {
            method: 'DELETE',
            headers: getAuthHeader()
        });
        if (res.ok) {
            loadTrash();
            loadStats();
        } else {
            alert('Failed to delete user');
        }
    } catch (err) {
        console.error(err);
    }
}
