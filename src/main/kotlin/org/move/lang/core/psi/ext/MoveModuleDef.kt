package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.FQModule
import javax.swing.Icon

fun List<MvAttr>.findSingleItemAttr(name: String): MvAttr? =
    this.find {
        it.attrItemList.size == 1
                && it.attrItemList.first().identifier.text == name
    }

val MvModuleDef.isTestOnly: Boolean get() = this.attrList.findSingleItemAttr("test_only") != null

fun MvModuleDef.definedAddressRef(): MvAddressRef? =
    this.addressRef ?: (this.ancestorStrict<MvAddressDef>())?.addressRef

fun MvModuleDef.fqModule(): FQModule? {
    val address = this.containingAddress.normalized()
    val name = this.name ?: return null
    return FQModule(address, name)
}

val MvModuleDef.fqName: String?
    get() {
        val address = this.definedAddressRef()?.text?.let { "$it::" } ?: ""
        val module = this.name ?: return null
        return address + module
    }

val MvModuleDef.friendModules: Set<FQModule>
    get() {
        val block = this.moduleBlock ?: return emptySet()
        val moduleRefs = block.friendStatementList.mapNotNull { it.fqModuleRef }

        val friends = mutableSetOf<FQModule>()
        for (moduleRef in moduleRefs) {
            val address = moduleRef.addressRef.toNormalizedAddress() ?: continue
            val identifier = moduleRef.identifier?.text ?: continue
            friends.add(FQModule(address, identifier))
        }
        return friends
    }

fun MvModuleDef.allFunctions() = moduleBlock?.functionList.orEmpty()

fun MvModuleDef.builtinFunctions(): List<MvFunction> {
    return listOf(
        createBuiltinFunction("""
            /// Removes `T` from address and returns it. 
            /// Aborts if address does not hold a `T`.
            native fun move_from<T: key>(addr: address): T acquires T;
            """, project),
        createBuiltinFunction("""
            /// Publishes `T` under `signer.address`. 
            /// Aborts if `signer.address` already holds a `T`.
            native fun move_to<T: key>(acc: &signer, res: T);
            """, project),
        createBuiltinFunction("native fun borrow_global<T: key>(addr: address): &T acquires T;", project),
        createBuiltinFunction(
            "native fun borrow_global_mut<T: key>(addr: address): &mut T acquires T;",
            project
        ),
        createBuiltinFunction("""
            /// Returns `true` if a `T` is stored under address
            native fun exists<T: key>(addr: address): bool;
            """, project),
        createBuiltinFunction("native fun freeze<S>(mut_ref: &mut S): &S;", project),
        createBuiltinFunction("native fun assert(_: bool, err: u64);", project),
    )
}

fun MvModuleDef.functions(visibility: Visibility): List<MvFunction> =
    when (visibility) {
        is Visibility.Public ->
            allFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC }
        is Visibility.PublicScript ->
            allFunctions()
                .filter { it.visibility == FunctionVisibility.PUBLIC_SCRIPT }
        is Visibility.PublicFriend -> {
            if (visibility.currentModule in this.friendModules) {
                allFunctions().filter { it.visibility == FunctionVisibility.PUBLIC_FRIEND }
            } else {
                emptyList()
            }
        }
        is Visibility.Internal -> allFunctions()
    }

fun createBuiltinFunction(text: String, project: Project): MvFunction {
    val trimmedText = text.trimIndent()
    val function = project.psiFactory.createFunction(trimmedText, moduleName = "builtin_functions")
    (function as MvFunctionMixin).builtIn = true
    return function
}

fun MvModuleDef.structs(): List<MvStruct_> = moduleBlock?.struct_List.orEmpty()

fun MvModuleDef.constBindings(): List<MvBindingPat> =
    moduleBlock?.constDefList.orEmpty().mapNotNull { it.bindingPat }


abstract class MvModuleDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                 MvModuleDef {
    override fun getIcon(flags: Int): Icon = MvIcons.MODULE

    override fun getPresentation(): ItemPresentation? {
        val name = this.name ?: return null
        val locationString = this.containingAddress.text
        return PresentationData(
            name,
            locationString,
            MvIcons.MODULE,
            null
        )
    }

    override val importStatements: List<MvImportStatement>
        get() =
            moduleBlock?.importStatementList.orEmpty()
}
