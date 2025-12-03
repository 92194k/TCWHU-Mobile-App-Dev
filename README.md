TCWHU Student Community Platform (Android)

Project Overview

The TCWHU Student Community Platform is a robust, production-ready Android application designed for students of Taguig City University (TCWHU). The core mission of this application is to foster secure, verified student interaction while providing administrators with powerful, centralized tools for moderation and safety management.

Key Goals:

Verified Identity: Ensure every user is an actual TCU student through a rigorous photo verification process.

User Privacy: Allow students to connect based on shared interests using anonymous profiles (nicknames) without exposing personal data.

Safety & Moderation: Implement a zero-tolerance reporting system connected to administrative actions (Warning, Suspension, Ban).

Technical Achievements and Stack

The application is built on a modern, scalable, serverless architecture primarily leveraging Google Firebase services.

Technology

Implementation Detail

Function

Backend / DB

Firebase Firestore

Real-time storage for all chat messages, user data, and report logs.

Media Storage

Cloudinary

Used for scalable storage and delivery of all uploaded files (IDs, selfies, chat images, event photos).

Core Language

Java (Native Android)

Provides high performance and full access to Android APIs.

Theming

Material Components 3

Implements cohesive branding and automatic Light/Dark Mode switching across all screens.

Messaging

Custom Logic (POJO)

Implements Per-User Deletion (one user deleting a chat does not affect the other user's history).

Core Features Implemented

1. Robust Account Lifecycle & Security

The system ensures that all users are authenticated and properly authorized before accessing the platform.

Secure Sign-In/Sign-Up: Utilizes Firebase Auth.

Two-Step Verification: Students submit a selfie and an ID photo during signup.

Status Enforcement: The app checks a user's status (isBanned, isSuspended) at login via SplashActivity and StudentLoginActivity, redirecting them to a restrictive screen if access is denied.

Verification Tool: StudentVerificationActivity allows admins to view uploaded photos and change isVerified status.

2. Advanced Chat & Communication

The chat system includes several modern features critical for privacy and functionality:

Private Deletion (One-Sided): When a user deletes a conversation, the application adds the user's ID to the deletedFor list in the chat document. The conversation is then immediately hidden from their list in ChatFragment, but remains visible to the other participant.

Unread Status Logic: Chats are correctly marked as bold (unread) in the Support Inbox or Chat Inbox, and the status clears instantly for the viewer when they open the ChatWindowActivity.

File and Media Upload: Users can upload images (photos) and document files (PDFs, Word files). The system enforces a 25MB size limit check using getFileSize().

UI Resilience: The chat window (ChatWindowActivity) is configured with android:windowSoftInputMode="adjustResize" to ensure the message list resizes correctly and is never blocked by the keyboard.

3. Safety and Administrative Control

This system provides administrators with the tools necessary to maintain a safe environment.

Admin Dashboard: Centralized screen (AdminDashboardActivity) using a GridLayout for easy navigation.

Support Inbox (Real-time Triage): The admin's chat list (AdminChatListActivity) is a dedicated inbox that loads support conversations in real-time, allowing admins to triage appeals and warnings quickly.

System Warning Protocol: When an admin issues a warning from the ReportsManagementActivity, the app:

Automatically opens a chat with the system_admin user.

Pre-fills the chat box with an official warning template.

When the student views the warning, the regular input box is replaced with "Confirm" and "Appeal" buttons (adminWarningActions).

The student's response sets a warningAcknowledged: true flag in Firestore, permanently hiding the buttons for that user.

Status Management: UserOverviewActivity allows the admin to filter users and execute immediate state changes: Suspend (30d), Permanent Ban, and Unban/Unsuspend.
