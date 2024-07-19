package org.sui.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.sui.lang.core.psi.MvMixPathItem
import org.sui.lang.core.psi.impl.MvNamedElementImpl
import org.sui.lang.core.resolve.ref.MvModuleReferenceImpl
import org.sui.lang.core.resolve.ref.MvPolyVariantReference

//val MvModuleRef.isSelfModuleRef: Boolean
//    get() =
//        this !is MvFQModuleRef
//                && this.referenceName == "Self"
//                && this.containingModule != null

abstract class MvMixPathItemMixin(node: ASTNode) : MvNamedElementImpl(node), MvMixPathItem {

    override fun getReference(): MvPolyVariantReference? = MvModuleReferenceImpl(this.moduleRef)

    override fun getName(): String? {
        val name = super.getText()
        return name
    }
}

//abstract class MvImportedModuleRefMixin(node: ASTNode) : MvReferenceElementImpl(node),
//                                                           MvImportedModuleRef {
//    override val identifier: PsiElement
//        get() {
//            throw NotImplementedError()
////            if (this is MvImportedModuleRef) return this.identifier
////            if (this is MvFQModuleRef) return this.identifier
//////            if (self is MvImportedModuleRef
//////                || self is MvFQModuleRef) return self.identifier
////            return null
//        }
//
//    override fun getReference(): MvReference {
//        return MvModuleReferenceImpl(this)
//    }
//
//    override val isUnresolved: Boolean
//        get() = super<MvReferenceElementImpl>.isUnresolved
//}
