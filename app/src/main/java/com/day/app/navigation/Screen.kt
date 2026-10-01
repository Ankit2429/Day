package com.day.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Notes : Screen("notes")
    object Documents : Screen("documents")
    object Settings : Screen("settings")
    object AddTask : Screen("add_task?time={time}") {
        fun createRoute(time: Long? = null) = if (time != null && time > 0L) "add_task?time=$time" else "add_task?time=-1"
    }
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    object TaskDetails : Screen("task_details/{taskId}") {
        fun createRoute(taskId: Long) = "task_details/$taskId"
    }
    object Esp8266 : Screen("esp8266")
    object PdfViewer : Screen("pdf_viewer/{documentId}") {
        fun createRoute(documentId: Long) = "pdf_viewer/$documentId"
    }
    object DocumentDetails : Screen("document_details/{documentId}") {
        fun createRoute(documentId: Long) = "document_details/$documentId"
    }
    object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long) = "note_editor/$noteId"
    }
}
