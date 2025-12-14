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
// CONFIGURATION & CONSTANTS
// ---------------------------
const REGION = "asia-southeast2";
const DEFAULT_TONE = "default_tone";

const gmailEmail = defineString("GMAIL_EMAIL");
const gmailPassword = defineString("GMAIL_PASSWORD");

// ---------------------------
// INITIALIZATION
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

/**
 * 1. CHAT NOTIFICATION
 * Trigger: Update on chats/{chatId}
 * Description: Sends a HIGH PRIORITY push notification to the receiver.
 */
export const sendChatNotification = onDocumentUpdated(
  {
    region: REGION,
    document: "chats/{chatId}",
  },
  async (event) => {
    try {
      const before = event.data.before.data();
      const after = event.data.after.data();

      // Ensure data exists and the lastMessage actually changed
      if (!before || !after || before.lastMessage === after.lastMessage) return;

      const { lastMessage, lastSenderId, users = [] } = after;

      // Identify Receiver (assumes 1-on-1 chat logic)
      const receiverId = users.find((id) => id !== lastSenderId);
      if (!receiverId) return;

      // Fetch Sender and Receiver details in parallel for efficiency
      const [senderSnap, receiverSnap] = await Promise.all([
        db.collection("users").doc(lastSenderId).get(),
        db.collection("users").doc(receiverId).get(),
      ]);

      if (!receiverSnap.exists) return;

      const receiverData = receiverSnap.data();
      const token = receiverData?.notificationToken;

      if (!token) return;

      const senderName = senderSnap.exists ? senderSnap.data()?.nickname : "Someone";

      const messagePayload = {
        token,
        // --- ADDED: Android High Priority Config ---
        android: {
          priority: "high", // Wakes the device immediately
          notification: {
            channelId: "high_importance_channel", // Matches Android code
            sound: "default",
            defaultSound: true,
            priority: "max",
            visibility: "public",
          },
        },
        // -------------------------------------------
        notification: {
          title: `New message from ${senderName}`,
          body: lastMessage,
        },
        data: {
          type: "chat",
          senderId: lastSenderId,
          otherUserId: lastSenderId,
          title: `New message from ${senderName}`,
          body: lastMessage,
        },
      };

      await messaging.send(messagePayload);
      logger.info(`Chat notification sent to ${receiverId}`);
    } catch (err) {
      logger.error("Error sending chat notification:", err);
    }
  }
);

/**
 * 2. EVENT ANNOUNCEMENT
 * Trigger: Create on events/{eventId}
 * Description: Multicasts a HIGH PRIORITY notification to all users.
 */
export const sendAnnouncementNotification = onDocumentCreated(
  {
    region: REGION,
    document: "events/{eventId}",
  },
  async (event) => {
    try {
      const data = event.data.data();
      if (!data) return;

      const { title, description: body } = data;
      const eventId = event.params.eventId;

      // Fetch all users to find tokens
      const usersSnapshot = await db.collection("users").get();
      const messages = [];

      usersSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (userData.notificationToken) {
          messages.push({
            token: userData.notificationToken,
            // --- ADDED: Android High Priority Config ---
            android: {
              priority: "high",
              notification: {
                channelId: "high_importance_channel",
                sound: "default",
                defaultSound: true,
                priority: "max",
                visibility: "public",
              },
            },
            // -------------------------------------------
            notification: { title, body },
            data: {
              type: "event",
              title,
              body,
              eventId,
            },
          });
        }
      });

      if (messages.length === 0) {
        logger.info("No tokens found for event notification.");
        return;
      }

      // Send via Multicast
      await messaging.sendEach(messages);
      logger.info(`Event notification sent to ${messages.length} users.`);
    } catch (err) {
      logger.error("Error sending event notification:", err);
    }
  }
);

/**
 * 3. DELETE USER ACCOUNT
 */
export const deleteUserAccount = onDocumentDeleted(
  {
    region: REGION,
    document: "users/{userId}",
  },
  async (event) => {
    try {
      const userId = event.params.userId;
      await auth.deleteUser(userId);
      logger.info(`Deleted auth account for ${userId}`);
    } catch (err) {
      logger.error(`Error deleting auth account for ${event.params.userId}:`, err);
    }
  }
);

/**
 * 4. DAILY CLEANUP
 */
export const dailyUserCleanup = onSchedule(
  {
    region: REGION,
    schedule: "every 24 hours",
  },
  async () => {
    try {
      logger.info("Running daily cleanup...");
      const now = Date.now();

      const snapshot = await db.collection("users")
        .where("isBanned", "==", true)
        .where("deletionDate", "<=", now)
        .where("deletionDate", "!=", 0)
        .get();

      if (snapshot.empty) {
        logger.info("No users to delete.");
        return;
      }

      const tasks = [];
      snapshot.forEach((doc) => {
        tasks.push(
          auth.deleteUser(doc.id).catch((e) =>
            logger.warn(`Auth delete failed for ${doc.id}`, e)
          )
        );
        tasks.push(doc.ref.delete());
      });

      await Promise.all(tasks);
      logger.info(`Cleanup completed. Removed ${snapshot.size} users.`);
    } catch (err) {
      logger.error("Cleanup error:", err);
    }
  }
);

/**
 * 5. SEND VERIFICATION EMAIL
 */
export const sendVerificationEmail = onDocumentUpdated(
  {
    region: REGION,
    document: "users/{userId}",
  },
  async (event) => {
    try {
      const before = event.data.before.data();
      const after = event.data.after.data();

      if (
        !before ||
        !after ||
        !(before.isVerified === false && after.isVerified === true)
      ) {
        return;
      }

      const { email, nickname, name } = after;
      const recipientName = nickname || name || "Student";

      if (!email) {
        logger.warn(`User ${event.params.userId} verified but has no email.`);
        return;
      }

      const transporter = createTransporter();
      const htmlContent = getVerificationEmailTemplate(recipientName);

      await transporter.sendMail({
        from: `TCWHU Admin <${gmailEmail.value()}>`,
        to: email,
        subject: "Your Account Has Been Approved ✓",
        html: htmlContent,
      });

      logger.info(`Verification email SENT to ${email}`);
    } catch (err) {
      logger.error("sendVerificationEmail error:", err);
    }
  }
);

// ---------------------------
// HTML TEMPLATES
// ---------------------------
function getVerificationEmailTemplate(name) {
  return `
    <!DOCTYPE html>
    <html>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
        <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
          <h2 style="color: #2c3e50;">Welcome, ${name}!</h2>
          <p>We are pleased to inform you that your account has been successfully <strong>approved</strong>.</p>
          <p>You may now log in to the application and access all features.</p>
          <br />
          <p>Best Regards,<br/><strong>TCWHU Admin Team</strong></p>
        </div>
      </body>
    </html>
  `;
}