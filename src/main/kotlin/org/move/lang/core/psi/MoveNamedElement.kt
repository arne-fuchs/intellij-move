package org.move.lang.core.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

interface MoveNamedElement : MoveElement, PsiNamedElement, NavigatablePsiElement

interface MoveNameIdentifierOwner : MoveNamedElement, PsiNameIdentifierOwner