package com.non.generator

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateAction
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.util.*

class NewFile : CreateFileFromTemplateAction("Generate DataSource Pattern", "Generate dataSource pattern", null) {

    companion object {
        fun createProperties(project: Project, className: String, dir: PsiDirectory): Properties {
            val properties = FileTemplateManager.getInstance(project).defaultProperties
            properties += "VERSION" to "1.0.0"
            properties += "NAME" to className
            return properties
        }

        fun createNewProperties(project: Project, featName:String, fileName: String, dir: PsiDirectory, templateName: String, fileTemplateManager: FileTemplateManager): Properties {
            val properties = fileTemplateManager.defaultProperties
            properties += "NAME" to fileName.capitalize()
            properties += "templateName" to templateName
            properties += "featureName" to featName
            properties += "capFeatName" to featName.capitalize()
            var name: String
            var packageName = ""
            var directory: PsiDirectory? = dir
            do {
                name = directory!!.name
                packageName = if (packageName.isEmpty()) {
                    name
                } else {
                    name.plus(".").plus(packageName)
                }
                directory = directory.parent
            } while (name != "com" && directory != null)
            properties += "packageName" to packageName
            return properties
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        LOG.setLevel(LogLevel.DEBUG)
        builder.addKind("DataSourceWithRemoteDS", null, "x")
    }

    override fun getActionName(project: PsiDirectory?, directory: String, builder: String?): String {
        return "NEW_FILE_ACTION"
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) = try {
        val className = FileUtilRt.getNameWithoutExtension(name)
        val project = dir.project
        val manager = FileTemplateManager.getInstance(project)
        val featDir=dir.checkOrCreateSubDir(className)
        when (template.name) {
            "x" -> {
                val newProperties =
                        listOf(
                                featDir.checkOrCreateSubDir("repo").let { Pair(it, createNewProperties(project, className,"${className}RemoteDS", it, "x", manager)) },
                                featDir.checkOrCreateSubDir("repo").let { Pair(it, createNewProperties(project, className,"${className}RemoteDSImpl", it, "nonce", manager)) },
                        )
                newProperties.forEachIndexed { index, pair ->
                    if (index == newProperties.lastIndex) {
                        return CreateFromTemplateDialog(project, pair.first, manager.getTemplate(pair.second.getProperty("templateName")), AttributesDefaults(pair.second.getProperty("NAME")).withFixedName(true), pair.second).create().containingFile
                    }
                    CreateFromTemplateDialog(project, pair.first, manager.getTemplate(pair.second.getProperty("templateName")), AttributesDefaults(pair.second.getProperty("NAME")).withFixedName(true), pair.second).create()
                }
            }
        }
        val properties = createProperties(project, className, dir)
        CreateFromTemplateDialog(project, dir, template, AttributesDefaults(className).withFixedName(true), properties)
                .create()
                .containingFile
    } catch (e: Exception) {
        LOG.error("Error while creating new file", e)
        null
    }

    private fun PsiDirectory.checkOrCreateSubDir(subdir:String) =
        findSubdirectory(subdir) ?: createSubdirectory(subdir)


}