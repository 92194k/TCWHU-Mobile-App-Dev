// index.js
// This file contains all FOUR of your required cloud functions.

// --- Imports ---
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { getAuth } from "firebase-admin/auth";
import {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentDeleted,
} from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler"; // <-- NEW IMPORT
import { logger } from "firebase-functions";

// --- Initialization ---
initializeApp();
const db = getFirestore();
const messaging = getMessaging();
const auth = getAuth();


/**
 * ----------------------------------------------------------------
 * FUNCTION 1: Send a 1-to-1 chat notification
 * ----------------------------------------------------------------
 */
export const sendChatNotification = onDocumentUpdated("chats/{chatId}", async (event) => {
    logger.info("Chat update detected!");
    const chatData = event.data.after.data();
    const beforeData = event.data.before.data();

    if (beforeData.lastMessage === chatData.lastMessage) {
        logger.info("No new message. This was a 'read' status update. Skipping notification.");
        return;
    }
    const lastMessage = chatData.lastMessage;
    const lastSenderId = chatData.lastSenderId;
    const userIds = chatData.users;
    if (!lastMessage || !lastSenderId || !userIds) {
        logger.error("Chat summary is missing data.");
        return;
    }
    const receiverId = userIds.find(id => id !== lastSenderId);
    if (!receiverId) {
        logger.error("Could not determine receiverId.");
        return;
    }
    let senderName;
    let receiverToken;
    try {
        const senderDoc = await db.collection("users").doc(lastSenderId).get();
        senderName = senderDoc.exists ? senderDoc.data().nickname : "Someone";
        const receiverDoc = await db.collection("users").doc(receiverId).get();
        if (!receiverDoc.exists) {
            logger.error(`Receiver document ${receiverId} does not exist.`);
            return;
        }
        receiverToken = receiverDoc.data().notificationToken;
        if (!receiverToken) {
            logger.warn(`Receiver ${receiverId} does not have a notificationToken.`);
            return;
        }
    } catch (error) {
        logger.error(`Error fetching user documents: ${error}`);
        return;
    }
    const payload = {
        notification: { title: `New message from ${senderName}`, body: lastMessage },
        token: receiverToken,
    };
    try {
        logger.info(`Sending notification from ${senderName} to ${receiverId}`);
        await messaging.send(payload);
        logger.info("Chat notification sent successfully!");
    } catch (error) {
        logger.error(`Error sending chat notification: ${error}`);
    }
});


/**
 * ----------------------------------------------------------------
 * FUNCTION 2: Send an event/announcement notification
 * ----------------------------------------------------------------
 */
export const sendAnnouncementNotification = onDocumentCreated("events/{eventId}", async (event) => {
    logger.info("New event detected in 'events' collection!");
    const newEvent = event.data.data();
    if (!newEvent) {
        logger.error("No data in event document.");
        return;
    }
    if (!newEvent.title || !newEvent.description) {
        logger.error("Event document is missing 'title' or 'description'.");
        return;
    }
    const topic = "all_users";
    const payload = {
        notification: { title: newEvent.title, body: newEvent.description },
        topic: topic,
    };
    try {
        logger.info(`Sending event notification to topic: ${topic}`);
        await messaging.send(payload);
        logger.info("Event notification sent successfully!");
    } catch (error) {
        logger.error(`Error sending event notification: ${error}`);
    }
});


/**
 * ----------------------------------------------------------------
 * FUNCTION 3: Delete a user's Auth account (for Admin Dashboard)
 * ----------------------------------------------------------------
 */
export const deleteUserAccount = onDocumentDeleted("users/{userId}", async (event) => {
    logger.info(`User document deleted: ${event.params.userId}. Deleting Auth account.`);
    const userId = event.params.userId;
    try {
        await auth.deleteUser(userId);
        logger.info(`Successfully deleted Auth account for ${userId}`);
    } catch (error) {
        logger.error(`Error deleting Auth user ${userId}: ${error.message}`);
    }
});


/**
 * ----------------------------------------------------------------
 * FUNCTION 4: Daily cleanup of banned users (NEW FUNCTION)
 * ----------------------------------------------------------------
 * This function runs automatically every 24 hours ("every day 00:00").
 * It finds all banned users whose 30-day period is over
 * and permanently deletes them.
 */
export const dailyUserCleanup = onSchedule("every 24 hours", async (event) => {
    logger.info("Running daily user cleanup job...");
    const now = Date.now();

    // 1. Find all users who are banned AND whose deletion date is in the past
    const query = db.collection("users")
        .where("isBanned", "==", true)
        .where("deletionDate", "<=", now)
        .where("deletionDate", "!=", 0); // Make sure date is not 0

    const usersToDelete = await query.get();

    if (usersToDelete.empty) {
        logger.info("No users found for deletion today.");
        return;
    }

    logger.info(`Found ${usersToDelete.size} users for deletion.`);
    const deletePromises = [];

    // 2. Loop through and delete both Auth and Firestore data
    usersToDelete.forEach(doc => {
        const userId = doc.id;

        // Add auth deletion to the promise list
        const authDeletePromise = auth.deleteUser(userId)
            .then(() => logger.info(`Successfully deleted Auth user: ${userId}`))
            .catch(err => logger.error(`Failed to delete Auth user ${userId}:`, err));

        deletePromises.push(authDeletePromise);

        // Add Firestore document deletion to the promise list
        const firestoreDeletePromise = doc.ref.delete()
            .then(() => logger.info(`Successfully deleted Firestore doc: ${userId}`))
            .catch(err => logger.error(`Failed to delete Firestore doc ${userId}:`, err));

        deletePromises.push(firestoreDeletePromise);
    });

    // 3. Wait for all deletions to finish
    await Promise.all(deletePromises);
    logger.info("Daily user cleanup job finished.");
});