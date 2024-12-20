package org.sui.lang.core.psi.ext

import org.sui.cli.containingMovePackage
import org.sui.lang.core.psi.MvElement
import org.sui.lang.core.psi.MvVisibilityModifier
import org.sui.lang.core.psi.containingModule
import org.sui.lang.core.psi.ext.VisKind.*
import org.sui.lang.core.resolve.ref.Visibility2

interface MvVisibilityOwner: MvElement {
    val visibilityModifier: MvVisibilityModifier? get() = childOfType<MvVisibilityModifier>()
//        get() = PsiTreeUtil.getStubChildOfType(this, MvVisibilityModifier::class.java)

    // restricted visibility considered as public
    val isPublic: Boolean get() = visibilityModifier != null
}

// todo: add VisibilityModifier to stubs, rename this one to VisStubKind
enum class VisKind(val keyword: String) {
    PUBLIC("public"),
    FRIEND("public(friend)"),
    PACKAGE("public(package)"),
    SCRIPT("public(script)");
}

val MvVisibilityModifier.stubVisKind: VisKind
    get() = when {
        hasFriend -> FRIEND
        hasPackage -> PACKAGE
        hasPublic -> PUBLIC
        // deprecated, should be at the end
        hasScript -> SCRIPT
        else -> error("exhaustive")
    }

val MvVisibilityOwner.visibility2: Visibility2
    get() {
        val kind = this.visibilityModifier?.stubVisKind ?: return Visibility2.Private
        return when (kind) {
            PACKAGE -> Visibility2.Restricted.Package()
//            PACKAGE -> Visibility2.Restricted.Package(lazy { this.containingMovePackage })
            FRIEND -> {
//                val module = this.containingModule ?: return Visibility2.Private
                Visibility2.Restricted.Friend(/*lazy { module.friendModules }*/)
            }
            // public(script) == public entry
            SCRIPT -> Visibility2.Public
            PUBLIC -> Visibility2.Public
        }
    }




