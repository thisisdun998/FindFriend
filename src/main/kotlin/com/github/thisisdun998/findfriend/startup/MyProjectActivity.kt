package com.github.thisisdun998.findfriend.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.github.thisisdun998.findfriend.services.WebSocketService

class MyProjectActivity : StartupActivity {
    private val logger = Logger.getInstance(MyProjectActivity::class.java)

    override fun runActivity(project: Project) {
        logger.info("Project opened: ${project.name}")
        // Initialize WebSocketService to ensure connection starts
        ApplicationManager.getApplication().getService(WebSocketService::class.java)
    }
}