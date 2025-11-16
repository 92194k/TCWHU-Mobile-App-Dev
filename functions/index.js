// index.js
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { getAuth } from "firebase-admin/auth";

import {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentDeleted
} from "firebase-functions/v2/firestore";

import { onSchedule } from "firebase-functions/v2/scheduler";
import { logger } from "firebase-functions";
import { defineString } from "firebase-functions/params";
import nodemailer from "nodemailer";

// ---------------------------
// PARAMS (Gmail credentials via environment params)
// ---------------------------
const gmailEmail = defineString("GMAIL_EMAIL");
const gmailPassword = defineString("GMAIL_PASSWORD");

// ---------------------------
// INIT
// ---------------------------
initializeApp();
const db = getFirestore();
const auth = getAuth();
const messaging = getMessaging();

// ---------------------------
// HELPER: Create nodemailer transporter
// ---------------------------
const createTransporter = () => {
  return nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: {
      user: gmailEmail.value(),
      pass: gmailPassword.value(),
    },
  });
};

// ---------------------------
// 1. Chat Notification
// ---------------------------
export const sendChatNotification = onDocumentUpdated(
  {
    region: "asia-southeast2",
    document: "chats/{chatId}",
  },
  async (event) => {
    try {
      const after = event.data.after.data();
      const before = event.data.before.data();
      if (!before || !after) return;

      if (before.lastMessage === after.lastMessage) return;

      const lastMessage = after.lastMessage;
      const lastSenderId = after.lastSenderId;
      const users = after.users || [];
      const receiverId = users.find(id => id !== lastSenderId);
      if (!receiverId) return;

      const senderDoc = await db.collection("users").doc(lastSenderId).get();
      const senderName = senderDoc.exists ? senderDoc.data().nickname : "Someone";

      const receiverDoc = await db.collection("users").doc(receiverId).get();
      if (!receiverDoc.exists) return;

      const token = receiverDoc.data().notificationToken;
      if (!token) return;

      await messaging.send({
        notification: {
          title: `New message from ${senderName}`,
          body: lastMessage,
        },
        token,
      });

      logger.info(`Chat notification sent from ${senderName} to ${receiverId}`);
    } catch (err) {
      logger.error("Error sending chat notification:", err);
    }
  }
);

// ---------------------------
// 2. Event Announcement
// ---------------------------
export const sendAnnouncementNotification = onDocumentCreated(
  {
    region: "asia-southeast2",
    document: "events/{eventId}",
  },
  async (event) => {
    try {
      const data = event.data.data();
      if (!data) return;

      await messaging.send({
        notification: {
          title: data.title,
          body: data.description,
        },
        topic: "all_users",
      });

      logger.info("Event notification sent.");
    } catch (err) {
      logger.error("Error sending event notification:", err);
    }
  }
);

// ---------------------------
// 3. Delete User Account
// ---------------------------
export const deleteUserAccount = onDocumentDeleted(
  {
    region: "asia-southeast2",
    document: "users/{userId}",
  },
  async (event) => {
    try {
      const userId = event.params.userId;
      await auth.deleteUser(userId);
      logger.info(`Deleted auth account for ${userId}`);
    } catch (err) {
      logger.error("Error deleting auth account:", err);
    }
  }
);

// ---------------------------
// 4. Daily Cleanup (banned users)
// ---------------------------
export const dailyUserCleanup = onSchedule(
  {
    region: "asia-southeast2",
    schedule: "every 24 hours",
  },
  async () => {
    try {
      logger.info("Running cleanup...");
      const now = Date.now();

      const query = db.collection("users")
        .where("isBanned", "==", true)
        .where("deletionDate", "<=", now)
        .where("deletionDate", "!=", 0);

      const snapshot = await query.get();

      if (snapshot.empty) {
        logger.info("No users to delete.");
        return;
      }

      const tasks = [];
      snapshot.forEach(doc => {
        tasks.push(auth.deleteUser(doc.id));
        tasks.push(doc.ref.delete());
      });

      await Promise.all(tasks);
      logger.info("Cleanup completed.");
    } catch (err) {
      logger.error("Cleanup error:", err);
    }
  }
);

// ---------------------------
// 5. SEND VERIFICATION EMAIL
// Trigger when isVerified changes from false -> true
// ---------------------------
export const sendVerificationEmail = onDocumentUpdated(
  {
    region: "asia-southeast2",
    document: "users/{userId}",
  },
  async (event) => {
    try {
      const userId = event.params.userId;
      logger.info(`sendVerificationEmail TRIGGERED for: ${userId}`);

      const before = event.data.before.data();
      const after = event.data.after.data();
      if (!before || !after) return;

      logger.info(`Old isVerified: ${before.isVerified}, New isVerified: ${after.isVerified}`);

      if (before.isVerified === false && after.isVerified === true) {
        const email = after.email;
        const name = after.nickname || after.name || "Student";

        if (!email) {
          logger.error("No email on user; cannot send verification email.");
          return;
        }

        const transporter = createTransporter();

        const mailOptions = {
          from: `TCWHU Admin <${gmailEmail.value()}>`,
          to: email,
          subject: "Your Account Has Been Approved ✓",
          html: `
            <h2>Account Approved 🎉</h2>
            <p>Hello <strong>${name}</strong>,</p>
            <p>Your TCWHU account has been verified and approved. You may now log in to the mobile app.</p>
          `,
        };

        await transporter.sendMail(mailOptions);
        logger.info(`Verification email SENT to ${email}`);
      } else {
        logger.info("isVerified did NOT change false → true; skipping.");
      }
    } catch (err) {
      logger.error("sendVerificationEmail error:", err);
    }
  }
);
