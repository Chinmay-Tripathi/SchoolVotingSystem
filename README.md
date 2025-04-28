# Student Voting System

A modern Android-based voting system designed for educational institutions to conduct student elections efficiently and securely. The system consists of two separate applications: one for students and one for teachers/administrators.

## Project Structure

The project is divided into two main applications:

1. **StudentApp**: The student-facing application that allows students to:
   - Register and login
   - View available elections
   - Cast votes
   - View election results
   - Access their voting history

2. **TeacherApp**: The administrative application that enables teachers/administrators to:
   - Create and manage elections
   - Add candidates
   - Monitor voting progress
   - Generate and view results
   - Manage student accounts

## Features

### Student Application
- Secure authentication system
- Real-time election updates
- Intuitive voting interface
- Election result viewing
- Voting history tracking

### Teacher Application
- Election management dashboard
- Candidate management
- Real-time voting statistics
- Result generation and export
- Student account management

## Technical Stack

- **Frontend**: Android (Java/Kotlin)
- **Backend**: Firebase (Authentication, Realtime Database, Cloud Storage)
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI/UX**: Material Design components

## Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK
- Firebase account
- Android device or emulator

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Chinmay-Tripathi/SchoolVotingSystem.git
   ```

2. Open the project in Android Studio:
   - Open `StudentApp` and `TeacherApp` as separate projects
   - Sync Gradle files
   - Configure Firebase for both applications

3. Build and run the applications on your device or emulator

## Configuration

1. Set up Firebase:
   - Create a new Firebase project
   - Add both Android applications to the project
   - Download and add the `google-services.json` file to both apps
   - Enable Authentication and Realtime Database in Firebase console

2. Configure application settings:
   - Update Firebase configuration in both apps
   - Set up necessary permissions in AndroidManifest.xml

## Usage

### For Students
1. Install the StudentApp
2. Register with your student credentials
3. Log in to view available elections
4. Cast your vote in active elections
5. View results when elections are completed

### For Teachers
1. Install the TeacherApp
2. Log in with administrative credentials
3. Create and manage elections
4. Add candidates and set election parameters
5. Monitor voting progress and generate results

## Security Features

- Secure authentication using Firebase
- Role-based access control
- Encrypted data transmission
- Vote verification system
- Anti-tampering measures

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, please open an issue in the GitHub repository or contact the development team.

## Screenshots

Check the `screenshots` directory for application screenshots and demonstrations.