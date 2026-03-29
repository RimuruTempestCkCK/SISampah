# Aplikasi Pemantauan dan Pelaporan Pengangkutan Sampah Berbasis Android (Lapor-Sampah)

Lapor-Sampah adalah aplikasi Android berbasis Jetpack Compose yang dirancang untuk mempermudah pemantauan data petugas dan pelaporan jadwal pengangkutan sampah secara digital. Aplikasi ini memungkinkan administrator untuk mengelola pengguna (petugas dan masyarakat), mengatur jadwal pengangkutan di berbagai lokasi, serta menugaskan petugas secara spesifik untuk setiap jadwal guna meningkatkan efisiensi pelaporan di lapangan.

## Fitur Utama

*   **Manajemen Pengguna (User Management)**: 
    *   Kelola data Admin, Petugas LPS, Masyarakat, dan DLH.
    *   Dukungan untuk Nama Lengkap dan Username.
    *   Sistem migrasi database otomatis untuk penambahan kolom baru.
*   **Manajemen Jadwal (Schedule Management)**:
    *   Atur lokasi, hari, dan jam pengangkutan sampah.
    *   Dilengkapi dengan *Time Picker* yang intuitif.
    *   Penugasan Petugas secara langsung pada jadwal tertentu untuk pemantauan yang lebih akurat.
*   **Data Petugas**:
    *   Daftar petugas LPS yang terdaftar.
    *   Fitur detail petugas untuk melihat informasi lengkap.
    *   Pencarian petugas berdasarkan nama atau username.
*   **Antarmuka Modern**:
    *   Dibangun sepenuhnya menggunakan Jetpack Compose.
    *   Desain responsif dengan tema warna hijau yang segar dan bersih.
    *   Navigasi yang mudah antar layar admin.

## Arsitektur & Teknologi

*   **Bahasa Pemrograman**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Database**: MySQL (dikoneksikan melalui JDBC Driver)
*   **Networking/Database Helper**: `MySqlHelper` untuk manajemen koneksi asinkron menggunakan Kotlin Coroutines.
*   **Asynchronous Processing**: Kotlin Coroutines untuk operasi I/O yang efisien.

## Prasyarat Instalasi

1.  **Android Studio**: Versi terbaru (Ladybug atau lebih baru disarankan).
2.  **Server MySQL**: Pastikan server MySQL berjalan (misal: XAMPP/WAMP).
3.  **Konfigurasi Database**:
    *   Buat database bernama `sisampah_db`.
    *   Sesuaikan IP address dan kredensial di `com.example.sisampah.data.MySqlHelper`.
    *   Gunakan `10.0.2.2` jika menjalankan aplikasi melalui Emulator Android untuk mengakses `localhost`.

## Cara Menjalankan

1.  Clone repository ini ke mesin lokal Anda.
2.  Buka project di Android Studio.
3.  Sinkronkan Gradle (Gradle Sync).
4.  Pastikan koneksi internet tersedia untuk mengunduh dependensi (termasuk MySQL Connector).
5.  Jalankan aplikasi di Emulator atau Perangkat Fisik.

## Struktur Tabel Database (Otomatis Dibuat/Dimigrasi)

*   **users**: Menyimpan data pengguna (id, username, nama, password, role).
*   **schedules**: Menyimpan data jadwal (id, lokasi, hari, jam, petugas_id).

---
*Dikembangkan dengan ❤️ untuk lingkungan yang lebih bersih.*
