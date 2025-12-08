# Building Fireporter

## Prerequisites

Before building, ensure you have the following installed:

1.  **JDK 21:** Download and install JDK 21 for your specific operating system from the official Oracle Java Archive: [Java Archive Downloads - Java SE 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

## Building the Application

1.  **Clone the Repository:** Download or clone the Fireporter repository to your local machine.
2.  **Configure `build.gradle`:** Open the `build.gradle` file in the project directory.

    * Find the `targetPlatform()` entries.
    * **Remove all `targetPlatform()` blocks except the one for your operating system.**

    **Example (MacOS x64):**

    If your `build.gradle` looks like this:

    ```gradle
    // Before (all platforms)
    targetPlatform("linux") { ... }
    targetPlatform("mac") { ... } // MacOS x64
    targetPlatform("mac-aarch64") { ... }
    targetPlatform("win") { ... }
    ```

    Edit the file to keep only the `mac` block:

    ```gradle
    // After (only MacOS x64)
    targetPlatform("mac") { // mac x64
        jdkHome = jdkDownload("https://is3.cloudhost.id/jdk-javafx-bundle/21/jdk21-macos_x64.tar.gz") {
            archiveName = "mac-x64-jdk21"
        }
    }
    ```

3.  **Build the Runtime:** Open a terminal in the project directory and execute the following commands:
    ```bash
    gradle build runtime -x test
    ```
4.  **Create the Installer:**
    ```bash
    gradle jpackage
    ```
5.  **Access the Installer:** If the build is successful, the application installer will be located in the `build/distributions/installer` directory.