// Single Page Application (SPA) Controller

let currentUser = null; // Holds login session data { nim, nama, role }
let pendaftarList = []; // Holds list of registrations for admin verifications
let studentList = [];   // Holds list of student accounts for admin CRUD

document.addEventListener('DOMContentLoaded', () => {
    // Check if session data exists in localStorage
    const savedUser = localStorage.getItem('user_session');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
        setupSessionUI();
    } else {
        showLoginView();
    }

    // Bind file upload preview listeners
    bindUploadPreview('foto', 'foto_preview');
    bindUploadPreview('syarat', 'syarat_preview');
    bindUploadPreview('bukti_bayar', 'bukti_preview');
});

// --- View Router ---

function switchView(viewName) {
    // Hide all sections
    const sections = [
        'view-login', 'view-mhs-dashboard', 'view-mhs-daftar', 
        'view-mhs-upload', 'view-mhs-status', 'view-adm-dashboard',
        'view-adm-verifikasi', 'view-adm-laporan'
    ];
    sections.forEach(s => {
        const el = document.getElementById(s);
        if (el) el.style.display = 'none';
    });

    // Display app shell if not login
    const shell = document.getElementById('app-shell');
    if (viewName === 'login') {
        shell.style.display = 'none';
        document.getElementById('view-login').style.display = 'flex';
        return;
    } else {
        shell.style.display = 'flex';
    }

    // Show selected section
    const targetSection = document.getElementById(`view-${viewName}`);
    if (targetSection) targetSection.style.display = 'block';

    // Update active menu link
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => item.classList.remove('active'));
    
    // Find item with matching click trigger
    menuItems.forEach(item => {
        if (item.getAttribute('onclick') && item.getAttribute('onclick').includes(viewName)) {
            item.classList.add('active');
        }
    });

    // Set page header title
    const headerTitle = document.getElementById('page-header-title');
    if (headerTitle) {
        let titleText = 'Dashboard';
        if (viewName === 'mhs-daftar') titleText = 'Isi Formulir Pendaftaran';
        else if (viewName === 'mhs-upload') titleText = 'Unggah Berkas Persyaratan';
        else if (viewName === 'mhs-status') titleText = 'Status Pendaftaran & Kartu Wisuda';
        else if (viewName === 'adm-dashboard') titleText = 'Dashboard Ringkasan Admin';
        else if (viewName === 'adm-verifikasi') titleText = 'Panel Verifikator Persyaratan';
        else if (viewName === 'adm-laporan') titleText = 'Kelola Akun & Laporan Wisuda';
        headerTitle.innerText = titleText;
    }

    // Load data dynamically
    if (viewName === 'mhs-dashboard') loadMhsDashboard();
    else if (viewName === 'mhs-daftar') loadMhsDaftar();
    else if (viewName === 'mhs-upload') loadMhsUpload();
    else if (viewName === 'mhs-status') loadMhsStatus();
    else if (viewName === 'adm-dashboard') loadAdminDashboard();
    else if (viewName === 'adm-verifikasi') loadAdminVerifikasi();
    else if (viewName === 'adm-laporan') loadAdminLaporan();
}

function showLoginView() {
    currentUser = null;
    localStorage.removeItem('user_session');
    switchView('login');
}

// --- Session Initializations ---

function selectRole(role) {
    const btns = document.querySelectorAll('.role-btn');
    btns.forEach(btn => btn.classList.remove('active'));
    
    const label = document.getElementById('username_label');
    const input = document.getElementById('username');
    
    if (role === 'mahasiswa') {
        document.getElementById('role_mhs').checked = true;
        label.innerHTML = '<i class="fas fa-id-card"></i> Nomor Induk Mahasiswa (NIM)';
        input.placeholder = 'Masukkan NIM Anda';
        btns[0].classList.add('active');
    } else {
        document.getElementById('role_admin').checked = true;
        label.innerHTML = '<i class="fas fa-user"></i> Username Admin';
        input.placeholder = 'Masukkan Username Admin';
        btns[1].classList.add('active');
    }
}

function setupSessionUI() {
    // Populate header info
    document.getElementById('nav-user-name').innerText = currentUser.nama;
    document.getElementById('nav-user-role').innerText = currentUser.role === 'mahasiswa' ? 'Mahasiswa' : 'Administrator';
    document.getElementById('nav-user-avatar').innerText = currentUser.nama.substring(0,1).toUpperCase();

    // Toggle menu items
    if (currentUser.role === 'mahasiswa') {
        document.getElementById('menu-mhs').style.display = 'block';
        document.getElementById('menu-adm').style.display = 'none';
        switchView('mhs-dashboard');
    } else {
        document.getElementById('menu-mhs').style.display = 'none';
        document.getElementById('menu-adm').style.display = 'block';
        switchView('adm-dashboard');
    }
}

// --- API Wrapper Actions ---

async function handleLoginSubmit(e) {
    e.preventDefault();
    const role = document.querySelector('input[name="role"]:checked').value;
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role, username, password })
        });
        const data = await response.json();
        
        if (data.success) {
            currentUser = {
                role: data.role,
                nim: data.nim || null,
                nama: data.nama
            };
            localStorage.setItem('user_session', JSON.stringify(currentUser));
            setupSessionUI();
            showToast('Login berhasil! Selamat datang.', 'success');
        } else {
            showToast(data.error || 'Login gagal.', 'error');
        }
    } catch (err) {
        showToast('Kesalahan jaringan saat login.', 'error');
    }
}

function handleLogout() {
    if (confirm('Apakah Anda yakin ingin keluar?')) {
        showLoginView();
        showToast('Logout berhasil.', 'success');
    }
}

// --- Student Panel Logic ---

async function loadMhsDashboard() {
    try {
        // Load Profile details
        const profRes = await fetch(`/api/mahasiswa/profil?nim=${currentUser.nim}`);
        const profile = await profRes.json();
        
        document.getElementById('profile-nama').innerText = profile.nama || '-';
        document.getElementById('profile-nim').innerText = profile.nim || '-';
        document.getElementById('profile-prodi').innerText = profile.prodi || '-';
        document.getElementById('profile-fakultas').innerText = profile.fakultas || '-';
        document.getElementById('profile-ipk').innerText = parseFloat(profile.ipk).toFixed(2) || '-';
        document.getElementById('profile-email').innerText = profile.email || '-';

        // Load Registration Status summary
        const statRes = await fetch(`/api/mahasiswa/status?nim=${currentUser.nim}`);
        const status = await statRes.json();

        const summaryBox = document.getElementById('mhs-status-summary-box');
        const actionBtn = document.getElementById('mhs-dashboard-action-btn');

        if (!status.registered) {
            summaryBox.innerHTML = `
                <div class="stat-icon warning" style="margin: 0 auto 15px auto; width: 60px; height: 60px; font-size: 26px;">
                    <i class="fas fa-file-signature"></i>
                </div>
                <h4 style="font-weight: 700;">Belum Mendaftar</h4>
                <p style="color: var(--text-light); font-size: 13px; margin-top: 8px; line-height: 1.5;">Anda belum mengisi formulir pendaftaran wisuda. Silakan isi form pendaftaran sekarang.</p>
            `;
            actionBtn.innerHTML = `
                <button onclick="switchView('mhs-daftar')" class="btn-primary">
                    <i class="fas fa-arrow-right" style="margin-right: 8px;"></i> Mulai Pendaftaran
                </button>
            `;
        } else {
            let statusIcon = 'fa-clock';
            let statusClass = 'warning';
            let statusTitle = 'Menunggu Verifikasi';
            if (status.status === 'APPROVED') {
                statusIcon = 'fa-check-double';
                statusClass = 'success';
                statusTitle = 'Pendaftaran Disetujui';
            } else if (status.status === 'REJECTED') {
                statusIcon = 'fa-times-circle';
                statusClass = 'danger';
                statusTitle = 'Pendaftaran Ditolak';
            }

            summaryBox.innerHTML = `
                <div class="stat-icon ${statusClass}" style="margin: 0 auto 15px auto; width: 60px; height: 60px; font-size: 26px;">
                    <i class="fas ${statusIcon}"></i>
                </div>
                <h4 style="font-weight: 700; color: var(--${statusClass});">${statusTitle}</h4>
                <p style="color: var(--text-light); font-size: 13px; margin-top: 8px; line-height: 1.5;">${status.keterangan}</p>
            `;
            actionBtn.innerHTML = `
                <button onclick="switchView('mhs-status')" class="btn-primary" style="background: linear-gradient(135deg, var(--primary-light) 0%, var(--secondary) 100%); box-shadow: 0 4px 15px rgba(0, 180, 216, 0.3);">
                    <i class="fas fa-eye" style="margin-right: 8px;"></i> Lihat Rincian Status
                </button>
            `;
        }
    } catch (err) {
        showToast('Gagal memuat profil mahasiswa.', 'error');
    }
}

async function loadMhsDaftar() {
    // Populate form placeholders
    const res = await fetch(`/api/mahasiswa/profil?nim=${currentUser.nim}`);
    const profile = await res.json();

    document.getElementById('reg-nim-ro').value = profile.nim;
    document.getElementById('reg-nama-ro').value = profile.nama;
    document.getElementById('reg-prodi-ro').value = `${profile.fakultas} / ${profile.prodi}`;
    document.getElementById('reg-ipk-ro').value = parseFloat(profile.ipk).toFixed(2);
}

async function handleRegistrationSubmit(e) {
    e.preventDefault();
    const nim = currentUser.nim;
    const periode = document.getElementById('periode_wisuda').value;
    const judul = document.getElementById('judul_skripsi').value.trim();

    if (!periode || judul === '') {
        showToast('Semua input wajib diisi.', 'error');
        return;
    }

    try {
        const response = await fetch('/api/mahasiswa/daftar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nim, periode, judul })
        });
        const data = await response.json();

        if (data.success) {
            showToast('Formulir wisuda berhasil disimpan!', 'success');
            switchView('mhs-upload');
        } else {
            showToast(data.error || 'Pendaftaran gagal.', 'error');
        }
    } catch (err) {
        showToast('Kesalahan jaringan pendaftaran.', 'error');
    }
}

async function loadMhsUpload() {
    // Fetch upload statuses
    const res = await fetch(`/api/mahasiswa/status?nim=${currentUser.nim}`);
    const status = await res.json();

    setupUploadBadge('status-foto-badge', status.fotoPath, status.statusBerkas);
    setupUploadBadge('status-syarat-badge', status.syaratPath, status.statusBerkas);
    setupUploadBadge('status-bukti-badge', status.buktiPath, status.statusPembayaran);
}

function setupUploadBadge(elementId, path, status) {
    const el = document.getElementById(elementId);
    if (!el) return;
    
    if (path && path !== 'null' && path !== '') {
        el.innerHTML = `
            <span class="badge badge-approved"><i class="fas fa-check"></i> Sudah Diunggah</span>
            <a href="${path}" target="_blank" style="font-size:12px; margin-left: 10px; color: var(--primary-light); text-decoration:none; font-weight:600;"><i class="fas fa-external-link-alt"></i> Lihat File</a>
        `;
    } else {
        el.innerHTML = `<span class="badge badge-pending"><i class="fas fa-exclamation-triangle"></i> Belum Diunggah</span>`;
    }
}

async function handleUploadSubmit(e) {
    e.preventDefault();
    
    const fotoFile = document.getElementById('foto').files[0];
    const syaratFile = document.getElementById('syarat').files[0];
    const buktiFile = document.getElementById('bukti_bayar').files[0];

    // Validate uploads
    if (!fotoFile && !syaratFile && !buktiFile) {
        showToast('Harap pilih minimal salah satu berkas untuk diupload.', 'error');
        return;
    }

    showToast('Memproses kompresi dan upload berkas...', 'info');

    try {
        const payload = { nim: currentUser.nim };
        
        if (fotoFile) payload.foto = await convertToBase64(fotoFile);
        if (syaratFile) payload.syarat = await convertToBase64(syaratFile);
        if (buktiFile) payload.bukti = await convertToBase64(buktiFile);

        const response = await fetch('/api/mahasiswa/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await response.json();

        if (data.success) {
            showToast('Seluruh dokumen berhasil diupload!', 'success');
            // Clean files
            document.getElementById('foto').value = '';
            document.getElementById('syarat').value = '';
            document.getElementById('bukti_bayar').value = '';
            document.getElementById('foto_preview').innerText = '';
            document.getElementById('syarat_preview').innerText = '';
            document.getElementById('bukti_preview').innerText = '';
            
            switchView('mhs-status');
        } else {
            showToast(data.error || 'Gagal mengupload berkas.', 'error');
        }
    } catch (err) {
        showToast('Kesalahan jaringan saat mengunggah berkas.', 'error');
    }
}

async function loadMhsStatus() {
    try {
        const res = await fetch(`/api/mahasiswa/status?nim=${currentUser.nim}`);
        const status = await res.json();

        const notReg = document.getElementById('status-not-registered-card');
        const details = document.getElementById('status-details-container');
        
        if (!status.registered) {
            notReg.style.display = 'block';
            details.style.display = 'none';
            return;
        }

        notReg.style.display = 'none';
        details.style.display = 'block';

        // 1. Setup Table Statuses
        setupBadgeMarkup('status-berkas-val', status.statusBerkas, 'Approved (Berkas Lolos)', 'Proses Verifikasi', 'Ditolak (Upload Ulang)');
        setupBadgeMarkup('status-bayar-val', status.statusPembayaran, 'Approved (Pembayaran Lunas)', 'Proses Verifikasi', 'Ditolak (Struk Palsu/Gagal)');
        setupBadgeMarkup('status-pendaftaran-val', status.status, 'DISETUJUI (Siap Wisuda)', 'MENUNGGU VERIFIKASI AKHIR', 'PENDAFTARAN DITOLAK');

        // 2. Toggles printable card if APPROVED
        const warning = document.getElementById('status-unapproved-warning');
        const printBox = document.getElementById('status-printable-card-box');

        if (status.status === 'APPROVED') {
            warning.style.display = 'none';
            printBox.style.display = 'block';

            // Populate Card Data
            document.getElementById('card-nim').innerText = `: ${currentUser.nim}`;
            document.getElementById('card-nama').innerText = `: ${currentUser.nama}`;
            document.getElementById('card-periode').innerText = `: ${status.periode}`;
            document.getElementById('card-judul').innerText = `: "${status.judul}"`;
            
            // Re-fetch profile for GPA/major
            const profRes = await fetch(`/api/mahasiswa/profil?nim=${currentUser.nim}`);
            const profile = await profRes.json();
            document.getElementById('card-fakultas').innerText = `: ${profile.fakultas}`;
            document.getElementById('card-prodi').innerText = `: ${profile.prodi}`;
            document.getElementById('card-ipk').innerText = `: ${parseFloat(profile.ipk).toFixed(2)}`;

            // Photo Preview
            const photoBox = document.getElementById('card-photo-box');
            if (status.fotoPath) {
                photoBox.innerHTML = `<img src="${status.fotoPath}" alt="Foto Resmi">`;
            } else {
                photoBox.innerHTML = `<i class="fas fa-user" style="font-size: 40px; color: var(--text-light);"></i>`;
            }

            document.getElementById('card-print-time').innerText = `Dicetak otomatis pada: ${new Date().toLocaleString('id-ID')}`;
        } else {
            warning.style.display = 'block';
            printBox.style.display = 'none';
        }
    } catch (err) {
        showToast('Gagal memuat status pendaftaran wisuda.', 'error');
    }
}

function setupBadgeMarkup(elId, status, appText, pendText, rejText) {
    const el = document.getElementById(elId);
    if (!el) return;
    
    if (status === 'APPROVED') el.innerHTML = `<span class="badge badge-approved"><i class="fas fa-check"></i> ${appText}</span>`;
    else if (status === 'REJECTED') el.innerHTML = `<span class="badge badge-rejected"><i class="fas fa-times"></i> ${rejText}</span>`;
    else el.innerHTML = `<span class="badge badge-pending"><i class="fas fa-clock"></i> ${pendText}</span>`;
}

// --- Admin Panel Logic ---

async function loadAdminDashboard() {
    try {
        const statsRes = await fetch('/api/admin/stats');
        const stats = await statsRes.json();

        document.getElementById('adm-stat-total').innerText = stats.total;
        document.getElementById('adm-stat-pending').innerText = stats.pending;
        document.getElementById('adm-stat-approved').innerText = stats.approved;
        document.getElementById('adm-stat-ipk').innerText = parseFloat(stats.avgIpk).toFixed(2);

        // Fetch pendaftar to calculate faculty distribution and logs
        const regRes = await fetch('/api/admin/pendaftar');
        pendaftarList = await regRes.json();

        // 1. Chart Progress Bars
        const facultyCounts = {};
        pendaftarList.forEach(p => {
            facultyCounts[p.fakultas] = (facultyCounts[p.fakultas] || 0) + 1;
        });

        const chartBox = document.getElementById('adm-faculty-chart-box');
        chartBox.innerHTML = '';

        const total = pendaftarList.length;
        if (total === 0) {
            chartBox.innerHTML = `<p style="text-align: center; color: var(--text-light); font-size: 14px;">Belum ada sebaran pendaftar.</p>`;
        } else {
            Object.keys(facultyCounts).forEach(fak => {
                const count = facultyCounts[fak];
                const pct = Math.round(count * 100 / total);
                
                chartBox.innerHTML += `
                    <div>
                        <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 600; margin-bottom: 6px;">
                            <span>${fak}</span>
                            <span>${count} Mhs (${pct}%)</span>
                        </div>
                        <div style="width: 100%; height: 8px; background-color: #e2e8f0; border-radius: 4px; overflow: hidden;">
                            <div style="width: ${pct}%; height: 100%; background-color: var(--primary); border-radius: 4px;"></div>
                        </div>
                    </div>
                `;
            });
        }

        // 2. Recent logs
        const recentBody = document.getElementById('admRecentTable').getElementsByTagName('tbody')[0];
        recentBody.innerHTML = '';
        
        const limit = pendaftarList.slice(0, 5);
        limit.forEach(p => {
            let badgeClass = 'pending';
            if (p.status === 'APPROVED') badgeClass = 'approved';
            else if (p.status === 'REJECTED') badgeClass = 'rejected';

            recentBody.innerHTML += `
                <tr>
                    <td>${p.nim}</td>
                    <td>${p.nama}</td>
                    <td>${p.tanggal}</td>
                    <td><span class="badge badge-${badgeClass}" style="padding: 4px 8px; font-size:11px;">${p.status}</span></td>
                </tr>
            `;
        });
        
        if (limit.length === 0) {
            recentBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-light);">Belum ada log pendaftar.</td></tr>`;
        }
    } catch (err) {
        showToast('Gagal memuat dashboard admin.', 'error');
    }
}

async function loadAdminVerifikasi() {
    try {
        const res = await fetch('/api/admin/pendaftar');
        pendaftarList = await res.json();
        renderVerificationTable();
    } catch (err) {
        showToast('Gagal memuat daftar verifikasi.', 'error');
    }
}

function renderVerificationTable() {
    const tbody = document.getElementById('verificationTable').getElementsByTagName('tbody')[0];
    tbody.innerHTML = '';

    pendaftarList.forEach(p => {
        let berkasBadge = `<span class="badge badge-pending">Pending</span>`;
        if (p.statusBerkas === 'APPROVED') berkasBadge = `<span class="badge badge-approved">Approved</span>`;
        else if (p.statusBerkas === 'REJECTED') berkasBadge = `<span class="badge badge-rejected">Rejected</span>`;

        let bayarBadge = `<span class="badge badge-pending">Pending</span>`;
        if (p.statusPembayaran === 'APPROVED') bayarBadge = `<span class="badge badge-approved">Lunas</span>`;
        else if (p.statusPembayaran === 'REJECTED') bayarBadge = `<span class="badge badge-rejected">Ditolak</span>`;

        let statusBadge = `<span class="badge badge-pending">PENDING</span>`;
        if (p.status === 'APPROVED') statusBadge = `<span class="badge badge-approved" style="font-weight:700;">APPROVED</span>`;
        else if (p.status === 'REJECTED') statusBadge = `<span class="badge badge-rejected" style="font-weight:700;">REJECTED</span>`;

        tbody.innerHTML += `
            <tr data-status="${p.status}" data-fakultas="${p.fakultas}">
                <td style="font-weight: 600;">${p.nim}</td>
                <td>${p.nama}</td>
                <td>${p.prodi}</td>
                <td style="font-weight: 700; color: var(--primary-light);">${parseFloat(p.ipk).toFixed(2)}</td>
                <td>${berkasBadge}</td>
                <td>${bayarBadge}</td>
                <td>${statusBadge}</td>
                <td>
                    <button onclick="openAdminVerifyModal('${p.nim}')" class="btn-sm btn-outline">
                        <i class="fas fa-eye"></i> Periksa
                    </button>
                </td>
            </tr>
        `;
    });

    if (pendaftarList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-light); padding: 30px;">Belum ada berkas terkirim.</td></tr>`;
    }
}

function openAdminVerifyModal(nim) {
    const p = pendaftarList.find(x => x.nim === nim);
    if (!p) return;

    document.getElementById('modal_nim').value = p.nim;
    document.getElementById('modal_nim_display').innerText = p.nim;
    document.getElementById('modal_nama').innerText = p.nama;
    document.getElementById('modal_judul').innerText = p.judul;

    // Foto Preview
    const img = document.getElementById('modal_img_preview');
    const noImg = document.getElementById('no-photo-preview');
    if (p.fotoPath && p.fotoPath !== 'null' && p.fotoPath !== '') {
        img.src = p.fotoPath;
        img.style.display = 'block';
        noImg.style.display = 'none';
    } else {
        img.style.display = 'none';
        noImg.style.display = 'block';
    }

    // PDF Link
    const pdfLink = document.getElementById('modal_pdf_link');
    const noPdf = document.getElementById('no-pdf-preview');
    if (p.syaratPath && p.syaratPath !== 'null' && p.syaratPath !== '') {
        pdfLink.href = p.syaratPath;
        pdfLink.style.display = 'inline-block';
        noPdf.style.display = 'none';
    } else {
        pdfLink.style.display = 'none';
        noPdf.style.display = 'block';
    }

    // Receipt Preview
    const receipt = document.getElementById('modal_receipt_preview');
    const noReceipt = document.getElementById('no-receipt-preview');
    if (p.buktiPath && p.buktiPath !== 'null' && p.buktiPath !== '') {
        receipt.src = p.buktiPath;
        receipt.style.display = 'block';
        noReceipt.style.display = 'none';
    } else {
        receipt.style.display = 'none';
        noReceipt.style.display = 'block';
    }

    // Status Badges
    const bBadge = document.getElementById('badge_berkas');
    bBadge.className = `badge badge-${p.statusBerkas.toLowerCase()}`;
    bBadge.innerText = p.statusBerkas;

    const pBadge = document.getElementById('badge_pembayaran');
    pBadge.className = `badge badge-${p.statusPembayaran.toLowerCase()}`;
    pBadge.innerText = p.statusPembayaran === 'APPROVED' ? 'LUNAS' : p.statusPembayaran;

    // Remarks
    document.getElementById('modal_keterangan').value = p.keterangan || '';

    openModal('verifyModal');
}

async function submitVerifyAction(type, status) {
    const nim = document.getElementById('modal_nim').value;
    const remarks = document.getElementById('modal_keterangan').value.trim();

    try {
        const response = await fetch('/api/admin/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nim, type, status, remarks })
        });
        const data = await response.json();

        if (data.success) {
            showToast('Aksi verifikasi berhasil diproses!', 'success');
            closeModal('verifyModal');
            loadAdminVerifikasi();
        } else {
            showToast(data.error || 'Gagal memproses verifikasi.', 'error');
        }
    } catch (err) {
        showToast('Kesalahan jaringan verifikasi.', 'error');
    }
}

// --- Admin Student CRUD Panel ---

async function loadAdminLaporan() {
    try {
        const res = await fetch('/api/admin/mhs');
        studentList = await res.json();
        renderLaporanTable();
    } catch (err) {
        showToast('Gagal memuat list data mahasiswa.', 'error');
    }
}

function renderLaporanTable() {
    const tbody = document.getElementById('mhsTable').getElementsByTagName('tbody')[0];
    tbody.innerHTML = '';

    studentList.forEach(m => {
        let regBadge = `<span class="badge" style="background-color:#e2e8f0; color:#4a5568;">BELUM DAFTAR</span>`;
        if (m.regStatus === 'PENDING') regBadge = `<span class="badge badge-pending">PENDING</span>`;
        else if (m.regStatus === 'APPROVED') regBadge = `<span class="badge badge-approved">APPROVED</span>`;
        else if (m.regStatus === 'REJECTED') regBadge = `<span class="badge badge-rejected">REJECTED</span>`;

        tbody.innerHTML += `
            <tr data-status="${m.regStatus}" data-fakultas="${m.fakultas}">
                <td style="font-weight: 600;">${m.nim}</td>
                <td style="font-weight: 500;">${m.nama}</td>
                <td>${m.email}</td>
                <td>${m.telepon}</td>
                <td>${m.fakultas} / ${m.prodi}</td>
                <td style="font-weight: 700; color: var(--primary-light);">${parseFloat(m.ipk).toFixed(2)}</td>
                <td>${regBadge}</td>
                <td>
                    <div style="display: flex; gap: 6px;">
                        <button onclick="openEditMhsModal('${m.nim}')" class="btn-sm btn-outline" style="border-color: var(--secondary); color: var(--secondary);">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button onclick="deleteStudent('${m.nim}')" class="btn-sm btn-danger">
                            <i class="fas fa-trash-alt"></i> Hapus
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    if (studentList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-light); padding: 30px;">Belum ada data mahasiswa.</td></tr>`;
    }
}

function openAddMhsModal() {
    document.getElementById('student-modal-title').innerText = 'Tambah Mahasiswa Baru';
    document.getElementById('student_is_edit').value = 'false';
    document.getElementById('mhs_nim').readOnly = false;
    document.getElementById('mhs_nim').style.backgroundColor = 'white';
    document.getElementById('mhs_nim').style.cursor = 'text';

    document.getElementById('mhs_nim').value = '';
    document.getElementById('mhs_nama').value = '';
    document.getElementById('mhs_email').value = '';
    document.getElementById('mhs_telepon').value = '';
    document.getElementById('mhs_fakultas').value = '';
    document.getElementById('mhs_prodi').value = '';
    document.getElementById('mhs_ipk').value = '';
    document.getElementById('mhs_password').value = '';
    
    document.getElementById('mhs-pwd-label').innerHTML = 'Password Akun <span style="color:var(--danger)">*</span>';
    document.getElementById('mhs_password').required = true;

    openModal('studentModal');
}

function openEditMhsModal(nim) {
    const m = studentList.find(x => x.nim === nim);
    if (!m) return;

    document.getElementById('student-modal-title').innerText = 'Edit Informasi Mahasiswa';
    document.getElementById('student_is_edit').value = 'true';
    document.getElementById('mhs_nim').value = m.nim;
    document.getElementById('mhs_nim').readOnly = true;
    document.getElementById('mhs_nim').style.backgroundColor = '#e2e8f0';
    document.getElementById('mhs_nim').style.cursor = 'not-allowed';

    document.getElementById('mhs_nama').value = m.nama;
    document.getElementById('mhs_email').value = m.email;
    document.getElementById('mhs_telepon').value = m.telepon;
    document.getElementById('mhs_fakultas').value = m.fakultas;
    document.getElementById('mhs_prodi').value = m.prodi;
    document.getElementById('mhs_ipk').value = m.ipk;
    document.getElementById('mhs_password').value = '';
    
    document.getElementById('mhs-pwd-label').innerHTML = 'Password Baru <span style="font-size:11px; color:var(--text-light)">(Kosongkan jika tidak diubah)</span>';
    document.getElementById('mhs_password').required = false;

    openModal('studentModal');
}

async function handleStudentFormSubmit(e) {
    e.preventDefault();
    const isEdit = document.getElementById('student_is_edit').value === 'true';
    
    const nim = document.getElementById('mhs_nim').value.trim();
    const nama = document.getElementById('mhs_nama').value.trim();
    const email = document.getElementById('mhs_email').value.trim();
    const telepon = document.getElementById('mhs_telepon').value.trim();
    const fakultas = document.getElementById('mhs_fakultas').value;
    const prodi = document.getElementById('mhs_prodi').value.trim();
    const ipk = document.getElementById('mhs_ipk').value;
    const password = document.getElementById('mhs_password').value;

    try {
        const response = await fetch('/api/admin/mhs', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ isEdit, nim, nama, email, telepon, fakultas, prodi, ipk, password })
        });
        const data = await response.json();

        if (data.success) {
            showToast(isEdit ? 'Profil mahasiswa berhasil diupdate!' : 'Mahasiswa baru berhasil didaftarkan!', 'success');
            closeModal('studentModal');
            loadAdminLaporan();
        } else {
            showToast(data.error || 'Operasi simpan gagal.', 'error');
        }
    } catch (err) {
        showToast('Kesalahan jaringan saat menyimpan data.', 'error');
    }
}

async function deleteStudent(nim) {
    if (confirm(`Apakah Anda yakin ingin menghapus data mahasiswa dengan NIM ${nim}? Seluruh data pendaftaran dan file usulan wisudanya juga akan dihapus permanen.`)) {
        try {
            const response = await fetch('/api/admin/mhs/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nim })
            });
            const data = await response.json();

            if (data.success) {
                showToast(`Data mahasiswa NIM ${nim} telah dihapus.`, 'success');
                loadAdminLaporan();
            } else {
                showToast(data.error || 'Gagal menghapus data.', 'error');
            }
        } catch (err) {
            showToast('Kesalahan jaringan saat menghapus.', 'error');
        }
    }
}

function exportCsv() {
    window.location.href = '/api/admin/export';
}

// --- Frontend Helpers & Toast Notifier ---

function openModal(modalId) {
    document.getElementById(modalId).style.display = 'flex';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

function bindUploadPreview(inputId, previewId) {
    const input = document.getElementById(inputId);
    const preview = document.getElementById(previewId);

    if (input && preview) {
        input.addEventListener('change', () => {
            if (input.files.length > 0) {
                const file = input.files[0];
                const size = (file.size / (1024 * 1024)).toFixed(2);
                preview.innerHTML = `<i class="fas fa-file-alt"></i> ${file.name} (${size} MB)`;
                if (file.size > 5 * 1024 * 1024) {
                    showToast(`Ukuran berkas ${file.name} melebihi batas 5MB!`, 'error');
                    input.value = '';
                    preview.innerHTML = '';
                }
            } else {
                preview.innerHTML = '';
            }
        });
    }
}

function convertToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

function showToast(msg, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast-msg toast-${type}`;
    
    let icon = 'fa-check-circle';
    if (type === 'error') icon = 'fa-exclamation-circle';
    else if (type === 'info') icon = 'fa-info-circle';

    toast.innerHTML = `<i class="fas ${icon}" style="margin-right: 10px; font-size:16px;"></i> ${msg}`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        setTimeout(() => toast.remove(), 400);
    }, 4500);
}

// Client-side table keyword filter helper
function filterTable(keywordId, statusId, fakultasId, tableId) {
    const key = document.getElementById(keywordId).value.toLowerCase().trim();
    const status = document.getElementById(statusId).value.toLowerCase();
    const fak = document.getElementById(fakultasId).value.toLowerCase();

    const rows = document.getElementById(tableId).getElementsByTagName('tbody')[0].getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        const text = row.textContent.toLowerCase();
        
        const rStatus = row.getAttribute('data-status') ? row.getAttribute('data-status').toLowerCase() : '';
        const rFak = row.getAttribute('data-fakultas') ? row.getAttribute('data-fakultas').toLowerCase() : '';

        const matchKey = text.includes(key);
        const matchStatus = status === 'all' || rStatus === status || (status === 'pending' && rStatus === 'belum_daftar');
        const matchFak = fak === 'all' || rFak === fak;

        if (matchKey && matchStatus && matchFak) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    }
}
