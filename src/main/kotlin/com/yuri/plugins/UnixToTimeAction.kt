package com.yuri.plugins

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import java.awt.datatransfer.StringSelection
import java.text.SimpleDateFormat

class UnixToTimeAction : DumbAwareAction() {
    // 根据自己需要去调整格式
    private val simpleDateFormat by lazy(LazyThreadSafetyMode.NONE) { SimpleDateFormat("MMMM-dd-yyyy, HH:mm:ss") }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        val selectedText = document.getText(TextRange(start, end))
        val timeStamp = selectedText.toLongOrNull()
        if (timeStamp == null) {
            Messages.showWarningDialog(
                    MyBundle.message("selectedStringNotLong.message", selectedText),
                    MyBundle.message("selectedStringNotLong.title"))
            return
        }

        val time = unixTimestampToTime(timeStamp)

        // 不建议把阅读器里面的都替换了，很抽象
        if (!editor.isViewer) {
            // 如果是直接替换的话
            replaceSelectedString(project, document, primaryCaret, time)
        }

        // 如果是设置剪切板
        setClipboard(time)


        primaryCaret.removeSelection()
    }

    /**
     * 替换选中文字为转换后的
     */
    private fun replaceSelectedString(project: Project, document: Document, caret: Caret, newText: String) {
        if (!document.isWritable) {
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val start = caret.selectionStart
            val end = caret.selectionEnd
            document.replaceString(start, end, newText)
        }
    }

    /**
     * 设置剪切板
     */
    private fun setClipboard(string: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(string))
    }

    private fun unixTimestampToTime(timeStamp: Long): String {
        return simpleDateFormat.format(timeStamp * 1000L)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = shouldShowInMenu(e)
    }

    private fun shouldShowInMenu(e: AnActionEvent): Boolean {
        e.project ?: return false

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return false

        val hasSelection = editor.selectionModel.hasSelection()
        if (!hasSelection) {
            return false
        }

        val selectedText = editor.selectionModel.selectedText ?: return false
        return selectedText.toLongOrNull() != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return super.getActionUpdateThread()
    }
}