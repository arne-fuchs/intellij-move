package org.move.lang.core.types.infer

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiElement
import org.move.ide.inspections.fixes.AddCastFix
import org.move.ide.presentation.expectedBindingFormText
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvReturnExpr
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInteger

sealed class TypeError(open val element: PsiElement) : TypeFoldable<TypeError> {
    abstract fun message(): String

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = true

    open fun fix(): LocalQuickFix? = null

    data class TypeMismatch(
        override val element: PsiElement,
        val expectedTy: Ty,
        val actualTy: Ty
    ) : TypeError(element) {
        override fun message(): String {
            return when (element) {
                is MvReturnExpr -> "Invalid return type '${actualTy.name()}', expected '${expectedTy.name()}'"
                else -> "Incompatible type '${actualTy.name()}', expected '${expectedTy.name()}'"
            }
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return TypeMismatch(element, folder(expectedTy), folder(actualTy))
        }

        override fun fix(): LocalQuickFix? {
            if (element !is MvExpr) return null
            if (expectedTy !is TyInteger || actualTy !is TyInteger) return null
            if (expectedTy.isDefault() || actualTy.isDefault()) return null
            return AddCastFix(element, expectedTy)
        }
    }

    data class UnsupportedBinaryOp(
        override val element: PsiElement,
        val ty: Ty,
        val op: String
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid argument to '$op': " +
                    "expected integer type, but found '${ty.text()}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return UnsupportedBinaryOp(element, folder(ty), op)
        }
    }

    data class IncompatibleArgumentsToBinaryExpr(
        override val element: PsiElement,
        val leftTy: Ty,
        val rightTy: Ty,
        val op: String,
    ) : TypeError(element) {
        override fun message(): String {
            return "Incompatible arguments to '$op': " +
                    "'${leftTy.text()}' and '${rightTy.text()}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return IncompatibleArgumentsToBinaryExpr(element, folder(leftTy), folder(rightTy), op)
        }
    }

    data class InvalidUnpacking(
        override val element: PsiElement,
        val assignedTy: Ty,
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid unpacking. Expected ${assignedTy.expectedBindingFormText()}"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError {
            return InvalidUnpacking(element, folder(assignedTy))
        }
    }

    data class CircularType(
        override val element: PsiElement,
        val structItem: MvStruct
    ) : TypeError(element) {
        override fun message(): String {
            return "Circular reference of type '${structItem.name}'"
        }

        override fun innerFoldWith(folder: TypeFolder): TypeError = this
    }
}