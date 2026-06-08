# Panduan Instalasi & Menjalankan Aplikasi Sistem Pendaftaran Wisuda

Aplikasi ini sekarang menggunakan arsitektur **Standalone Java Web Server** yang sangat ringan. Anda **tidak membutuhkan Apache Tomcat** atau server servlet eksternal lainnya. Aplikasi dapat dijalankan langsung menggunakan perintah `java`.

Antarmuka menggunakan arsitektur **Single Page Application (SPA)** berbasis **HTML5, CSS3, dan JavaScript (AJAX)** yang langsung terhubung ke API Java backend.

---

## 📋 Prasyarat Sistem
1. **Java Development Kit (JDK) 8** atau versi terbaru (OpenJDK 25 sudah teruji).
2. **Laragon** (untuk menjalankan MySQL Server di port 3306).

---

## 🛠️ Langkah-Langkah Menjalankan Aplikasi

### Langkah 1: Siapkan Database di Laragon
1. Buka **Laragon**, klik tombol **Start All**.
2. Klik tombol **Database** di Laragon untuk membuka **HeidiSQL** (atau phpMyAdmin).
3. Buat database baru bernama `db_wisuda`.
4. Import data skema dan dummy wisuda:
   * Di HeidiSQL, klik menu `File` -> `Load SQL file...`
   * Pilih file `f:\PBO\database\db_wisuda.sql`.
   * Tekan tombol **F9** (atau klik ikon **Run** segitiga biru) untuk mengeksekusi script SQL.

---

### Langkah 2: Kompilasi dan Jalankan Server Java
Aplikasi dapat dijalankan melalui **Command Prompt / PowerShell** di folder `f:\PBO`:

1. **Kompilasi Program**:
   ```bash
   javac -cp "lib/*" -d bin src/app/*.java src/database/*.java src/model/*.java
   ```
   *(Perintah ini akan mengompilasi file Java di folder `src/` dan menaruh output `.class` ke folder `bin/`)*

2. **Jalankan Server**:
   ```bash
   java -cp "bin;lib/*" model.WisudaApp
   ```
   *(Perintah ini akan menyalakan web server lokal di port 8080)*

---

### Langkah 3: Buka di Browser Anda
Setelah server berjalan, buka browser internet Anda dan akses alamat berikut:
👉 [**http://localhost:8080**](http://localhost:8080)

---

## 🔑 Akun Uji Coba (Dummy Data)
* **Login Mahasiswa (Belum Daftar)**: NIM: `111222333` | Password: `rian123`
* **Login Mahasiswa (Sudah Disetujui)**: NIM: `2415061122` | Password: `jaya123`
* **Login Admin**: Username: `admin` | Password: `admin123`

---

## 📂 Struktur Folder Proyek
```
PBO/
├── bin/                   <- Output class Java hasil kompilasi
├── database/
│   └── db_wisuda.sql       <- Skema MySQL & Dummy data
├── lib/
│   └── mysql-connector-java.jar <- Driver koneksi MySQL (JDBC)
├── src/
│   ├── app/               <- Folder handler HTTP
│   │   ├── StaticHandler.java  <- File server static (HTML/CSS/JS)
│   │   └── ApiHandler.java    <- Endpoint REST API (JSON & Base64 uploader)
│   ├── database/          <- Folder transaksi database
│   │   └── DbHelper.java   <- Transaksi database JDBC (PreparedStatement)
│   └── model/             <- Model data OOP & WisudaApp launcher
│       ├── WisudaApp.java     <- Launcher utama server HTTP
│       ├── Admin.java
│       ├── Berkas.java
│       ├── Mahasiswa.java
│       ├── Pembayaran.java
│       ├── Pendaftaran.java
│       └── User.java
└── web/
    ├── css/
    │   └── style.css       <- Styling visual premium (SPA)
    ├── js/
    │   └── app.js          <- Frontend controller (AJAX & view router)
    ├── uploads/            <- Folder foto & berkas terunggah
    └── index.html          <- Halaman tunggal utama aplikasi (SPA)
```
