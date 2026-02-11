# Distributed File Storage System (DFS)

A robust and efficient distributed file storage system designed for secure file management, version control, and storage optimization.

![Dashboard](https://neal-public-assets.s3.us-east-2.amazonaws.com/s/2026-02-11/b3c67537-88ab-4cc2-9842-880c85b5465e.png)

## 🚀 Features

-   **File Upload & Download**: Seamlessly upload and download files of any type.
-   **Chunk-Based Storage**: Files are split into 4MB chunks for efficient storage and transfer.
-   **Deduplication**: Saves storage space by storing identical chunks only once (Content-Addressable Storage).
-   **Versioning**: Automatically maintains file versions. Restore previous versions anytime.
-   **Trash Bin**: Soft-delete files with the ability to restore them or permanently delete.
-   **Secure Authentication**: User registration and login protected by Spring Security.
-   **Dashboard**: Clean, responsive user interface to manage files and view storage stats.

## 🏗 Application Architecture

The application follows a standard **Controller-Service-Repository** layered architecture:

1.  **Controller Layer**: Handles HTTP requests (REST API) for Auth and File operations.
2.  **Service Layer**: Contains business logic (Chunking, Hashing, Deduplication, Version Control).
3.  **Repository Layer**: Interacts with the H2 database for metadata persistence.
4.  **Storage Layer**: Physical storage of encrypted/hashed file chunks on the disk.

## 🛠 Tech Stack

-   **Java 17+**
-   **Spring Boot 3.x**
-   **Spring Security**
-   **Database H2** (Embedded)
-   **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
-   **Scheduler**: Enabled (For Garbage Collection)

## 📂 Project Structure

```
src/main/java/com/example/dfs
├── config/          # Security configurations
├── controller/      # API Controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA Entities (User, FileMetadata, FileVersion, Chunk)
├── repository/      # JPA Repositories
├── service/         # Business Logic (FileService, ChunkService, Deduplication)
└── util/            # Utility classes (Hashing)

src/main/resources
├── static/          # Frontend Assets (HTML, CSS, JS)
└── application.properties
```

## 🔄 Application Flow

1.  **User Upload**: User uploads a file via the Dashboard.
2.  **Processing**:
    -   File is split into **4MB Chunks**.
    -   **SHA-256 Hash** is calculated for each chunk.
    -   **Bloom Filter** checks if the chunk already exists.
3.  **Storage**:
    -   **New Chunk**: Saved to disk.
    -   **Duplicate Chunk**: Reference count incremented (No new storage used).
4.  **Metadata**: File metadata and Version info are saved in H2 Database.

## 🏃 How to Run the Project

### Prerequisites
-   Java 17 or higher
-   Maven

### Steps
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/karansingh008/Distributed-File-Storage-System-DFS-.git
    cd Distributed-File-Storage-System-DFS-
    ```

2.  **Build the project**:
    ```bash
    mvn clean package
    ```
    *(Or use the provided `run_mvn.bat` on Windows)*

3.  **Run the application**:
    ```bash
    java -jar target/dfs-0.0.1-SNAPSHOT.jar
    ```
    *(Or use `run_app.bat` on Windows)*

4.  **Access the Dashboard**:
    Open your browser and go to: `http://localhost:8080`

## 👤 Author

**Karan Singh**
