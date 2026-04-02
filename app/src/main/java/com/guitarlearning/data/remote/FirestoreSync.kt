package com.guitarlearning.data.remote

import com.guitarlearning.domain.model.Goal
import com.guitarlearning.domain.model.GoalType
import com.guitarlearning.domain.model.Session
import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.model.Difficulty
import com.guitarlearning.presentation.main.ThemeMode
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSync {

    private val db = FirebaseFirestore.getInstance()

    // ─── Settings ────────────────────────────────────────────────

    suspend fun syncSettingsToCloud(uid: String, themeMode: ThemeMode, normalSpeed: Float, practiceSpeed: Float) {
        db.collection("users").document(uid).collection("settings").document("prefs").set(
            mapOf(
                "themeMode" to themeMode.name,
                "normalSpeed" to normalSpeed,
                "practiceSpeed" to practiceSpeed
            )
        ).await()
    }

    suspend fun syncSettingsFromCloud(uid: String): Map<String, Any>? {
        return db.collection("users").document(uid)
            .collection("settings").document("prefs")
            .get().await().data
    }

    // ─── Tab Progress ─────────────────────────────────────────────

    suspend fun syncTabProgressToCloud(uid: String, tabs: List<TabItem>) {
        val batch = db.batch()
        tabs.filter { !it.isUserTab }.forEach { tab ->
            val ref = db.collection("users").document(uid)
                .collection("tab_progress").document(tab.id)
            batch.set(ref, mapOf("isCompleted" to tab.isCompleted))
        }
        batch.commit().await()
    }

    suspend fun syncTabProgressFromCloud(uid: String): Map<String, Boolean> {
        val snap = db.collection("users").document(uid)
            .collection("tab_progress").get().await()
        return snap.documents.associate { it.id to (it.getBoolean("isCompleted") ?: false) }
    }

    // ─── User Tabs (metadata only, files remain local) ───────────

    suspend fun syncUserTabsToCloud(uid: String, userTabs: List<TabItem>) {
        val batch = db.batch()
        userTabs.forEach { tab ->
            val ref = db.collection("users").document(uid)
                .collection("user_tabs").document(tab.id)
            batch.set(ref, mapOf(
                "id" to tab.id,
                "name" to tab.name,
                "description" to tab.description,
                "difficulty" to tab.difficulty.name,
                "lessonNumber" to tab.lessonNumber
            ))
        }
        batch.commit().await()
    }

    suspend fun syncUserTabsFromCloud(uid: String): List<Map<String, Any>> {
        val snap = db.collection("users").document(uid)
            .collection("user_tabs").get().await()
        return snap.documents.mapNotNull { it.data }
    }

    // ─── Sessions ─────────────────────────────────────────────────

    suspend fun syncSessionsToCloud(uid: String, sessions: List<Session>) {
        val batch = db.batch()
        sessions.forEach { session ->
            val ref = db.collection("users").document(uid)
                .collection("sessions").document(session.id.toString())
            batch.set(ref, mapOf(
                "startTime" to session.startTime.time,
                "endTime" to session.endTime.time,
                "duration" to session.duration
            ))
        }
        batch.commit().await()
    }

    // ─── Goals ────────────────────────────────────────────────────

    suspend fun syncGoalsToCloud(uid: String, goals: List<Goal>) {
        val batch = db.batch()
        goals.forEach { goal ->
            val ref = db.collection("users").document(uid)
                .collection("goals").document(goal.id.toString())
            batch.set(ref, mapOf(
                "type" to goal.type.name,
                "description" to goal.description,
                "target" to goal.target,
                "progress" to goal.progress,
                "deadline" to goal.deadline,
                "isCompleted" to goal.isCompleted,
                "isOverdue" to goal.isOverdue
            ))
        }
        batch.commit().await()
    }

    suspend fun syncGoalsFromCloud(uid: String): List<Map<String, Any>> {
        val snap = db.collection("users").document(uid)
            .collection("goals").get().await()
        return snap.documents.mapNotNull { it.data }
    }
}
