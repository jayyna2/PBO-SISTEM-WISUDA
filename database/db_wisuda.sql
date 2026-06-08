-- Database Creation
CREATE DATABASE IF NOT EXISTS db_wisuda;
USE db_wisuda;

-- Drop tables if they exist to start fresh (ordered by dependency)
DROP TABLE IF EXISTS tabel_pembayaran;
DROP TABLE IF EXISTS tabel_berkas;
DROP TABLE IF EXISTS tabel_pendaftaran_wisuda;
DROP TABLE IF EXISTS tabel_mahasiswa;
DROP TABLE IF EXISTS tabel_admin;

-- 1. Table Admin
CREATE TABLE tabel_admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nama_lengkap VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2. Table Mahasiswa
CREATE TABLE tabel_mahasiswa (
    nim VARCHAR(20) PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telepon VARCHAR(20) NOT NULL,
    fakultas VARCHAR(100) NOT NULL,
    prodi VARCHAR(100) NOT NULL,
    ipk DECIMAL(3,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 3. Table Pendaftaran Wisuda
CREATE TABLE tabel_pendaftaran_wisuda (
    id_pendaftaran INT AUTO_INCREMENT PRIMARY KEY,
    nim VARCHAR(20) NOT NULL,
    periode_wisuda VARCHAR(50) NOT NULL,
    judul_skripsi TEXT NOT NULL,
    tanggal_daftar DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    keterangan TEXT,
    CONSTRAINT fk_pendaftaran_mhs FOREIGN KEY (nim) 
        REFERENCES tabel_mahasiswa(nim) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 4. Table Berkas
CREATE TABLE tabel_berkas (
    id_berkas INT AUTO_INCREMENT PRIMARY KEY,
    nim VARCHAR(20) NOT NULL,
    foto_path VARCHAR(255),
    persyaratan_path VARCHAR(255),
    status_verifikasi VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    CONSTRAINT fk_berkas_mhs FOREIGN KEY (nim) 
        REFERENCES tabel_mahasiswa(nim) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 5. Table Pembayaran
CREATE TABLE tabel_pembayaran (
    id_pembayaran INT AUTO_INCREMENT PRIMARY KEY,
    nim VARCHAR(20) NOT NULL,
    jumlah_bayar DECIMAL(12,2) NOT NULL,
    bukti_bayar_path VARCHAR(255) NOT NULL,
    status_pembayaran VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    tanggal_bayar DATE NOT NULL,
    CONSTRAINT fk_pembayaran_mhs FOREIGN KEY (nim) 
        REFERENCES tabel_mahasiswa(nim) 
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------
-- SEED DATA DUMMY
-- --------------------------------------------------------

-- Insert Admin
INSERT INTO tabel_admin (username, password, nama_lengkap) VALUES 
('admin', 'admin123', 'Administrator Wisuda');

-- Insert Mahasiswa
INSERT INTO tabel_mahasiswa (nim, nama, password, email, telepon, fakultas, prodi, ipk) VALUES 
('2415061122', 'Jaya Pratama', 'jaya123', 'jaya.pratama@univ.ac.id', '081234567890', 'Teknik', 'Teknik Informatika', 3.75),
('987654321', 'Siti Aminah', 'siti123', 'siti.aminah@univ.ac.id', '081298765432', 'Ekonomi dan Bisnis', 'Akuntansi', 3.82),
('111222333', 'Rian Hidayat', 'rian123', 'rian.hidayat@univ.ac.id', '085711122233', 'Matematika dan IPA', 'Matematika', 2.95);

-- Insert dummy registrations
-- Jaya Pratama (Approved)
INSERT INTO tabel_pendaftaran_wisuda (nim, periode_wisuda, judul_skripsi, tanggal_daftar, status, keterangan) VALUES
('2415061122', 'Periode II - September 2026', 'Analisis Sentimen Menggunakan Algoritma Naive Bayes', '2026-06-01', 'APPROVED', 'Pendaftaran memenuhi semua berkas dan pembayaran.');

INSERT INTO tabel_berkas (nim, foto_path, persyaratan_path, status_verifikasi) VALUES
('2415061122', 'uploads/2415061122_foto.jpg', 'uploads/2415061122_syarat.pdf', 'APPROVED');

INSERT INTO tabel_pembayaran (nim, jumlah_bayar, bukti_bayar_path, status_pembayaran, tanggal_bayar) VALUES
('2415061122', 750000.00, 'uploads/2415061122_bukti.jpg', 'APPROVED', '2026-06-02');

-- Siti Aminah (Pending verification)
INSERT INTO tabel_pendaftaran_wisuda (nim, periode_wisuda, judul_skripsi, tanggal_daftar, status, keterangan) VALUES
('987654321', 'Periode II - September 2026', 'Audit Kinerja Keuangan Daerah Menggunakan SAP', '2026-06-05', 'PENDING', 'Pendaftaran diterima. Menunggu verifikasi berkas dan bukti transfer.');

INSERT INTO tabel_berkas (nim, foto_path, persyaratan_path, status_verifikasi) VALUES
('987654321', 'uploads/987654321_foto.jpg', 'uploads/987654321_syarat.pdf', 'PENDING');

INSERT INTO tabel_pembayaran (nim, jumlah_bayar, bukti_bayar_path, status_pembayaran, tanggal_bayar) VALUES
('987654321', 750000.00, 'uploads/987654321_bukti.jpg', 'PENDING', '2026-06-05');
