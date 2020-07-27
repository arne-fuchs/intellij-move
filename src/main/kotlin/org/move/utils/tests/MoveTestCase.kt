/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path
import java.nio.file.Paths

fun camelOrWordsToSnake(name: String): String {
    if (' ' in name) return name.trim().replace(" ", "_")

    return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::toLowerCase)
}

interface TestCase {
    val testFileExtension: String
    fun getTestDataPath(): String
    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        const val testResourcesPath = "src/test/resources"
    }
}

fun TestCase.pathToSourceTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.$testFileExtension")

fun TestCase.pathToGoldTestFile(): Path =
    Paths.get("${TestCase.testResourcesPath}/${getTestDataPath()}/${getTestName(true)}.txt")


abstract class MoveTestCase : BasePlatformTestCase() {
    protected inline fun <reified T : PsiElement> findElementInEditor(marker: String = "^"): T =
        findElementInEditor(T::class.java, marker)

    protected fun <T : PsiElement> findElementInEditor(psiClass: Class<T>, marker: String): T {
        val (element, data) = findElementWithDataAndOffsetInEditor(psiClass, marker)
        check(data.isEmpty()) { "Did not expect marker data" }
        return element
    }

    protected fun <T : PsiElement> findElementWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String
    ): Triple<T, String, Int> {
        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInEditor(psiClass, marker)
        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${myFixture.file.text}" }
        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${myFixture.file.text}" }
        return elementsWithDataAndOffset.first()
    }

    protected fun <T : PsiElement> findElementsWithDataAndOffsetInEditor(
        psiClass: Class<T>,
        marker: String
    ): List<Triple<T, String, Int>> {
        val commentPrefix =
            LanguageCommenters.INSTANCE.forLanguage(myFixture.file.language).lineCommentPrefix ?: "//"
        val caretMarker = "$commentPrefix$marker"
        val text = myFixture.file.text
        val triples = mutableListOf<Triple<T, String, Int>>()
        var markerOffset = -caretMarker.length
        while (true) {
            markerOffset = text.indexOf(caretMarker, markerOffset + caretMarker.length)
            if (markerOffset == -1) break
            val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
            val markerPosition =
                myFixture.editor.offsetToLogicalPosition(markerOffset + caretMarker.length - 1)
            val previousLine = LogicalPosition(markerPosition.line - 1, markerPosition.column)
            val elementOffset = myFixture.editor.logicalPositionToOffset(previousLine)
            val elementAtMarker = myFixture.file.findElementAt(elementOffset)!!
            val element = PsiTreeUtil.getParentOfType(elementAtMarker, psiClass, false)
            if (element != null) {
                triples.add(Triple(element, data, elementOffset))
            } else {
                val injectionElement = InjectedLanguageManager.getInstance(project)
                    .findInjectedElementAt(myFixture.file, elementOffset)
                    ?.let { PsiTreeUtil.getParentOfType(it, psiClass, false) }
                    ?: error("No ${psiClass.simpleName} at ${elementAtMarker.text}")
                val injectionOffset = (injectionElement.containingFile.virtualFile as VirtualFileWindow)
                    .documentWindow.hostToInjected(elementOffset)
                triples.add(Triple(injectionElement, data, injectionOffset))
            }
        }
        return triples
    }

}