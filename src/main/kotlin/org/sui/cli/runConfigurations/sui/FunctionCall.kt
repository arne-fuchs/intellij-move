package org.sui.cli.runConfigurations.sui

import com.intellij.psi.SmartPsiElementPointer
import org.sui.cli.MoveProject
import org.sui.lang.core.psi.MvFunction
import org.sui.lang.core.psi.ext.*
import org.sui.lang.core.psi.parameters
import org.sui.lang.core.psi.parametersAsBindings
import org.sui.lang.core.psi.typeParameters
import org.sui.lang.core.types.ty.*

data class FunctionCallParam(val value: String, val type: String) {
    fun cmdText(): String = "$type:$value"

    companion object {
        fun tyTypeName(ty: Ty): String {
            return when (ty) {
                is TyInteger -> ty.kind.name
                is TyAddress -> "address"
                is TyBool -> "bool"
                is TyVector -> "vector"
                else -> "unknown"
            }
        }
    }
}

data class FunctionCall(
    val item: SmartPsiElementPointer<MvFunction>?,
    val typeParams: MutableMap<String, String?>,
    val valueParams: MutableMap<String, FunctionCallParam?>,
    val packageId: String?,
    val moduleName: String?,
    val gasId: String?,
    val gasBudget: String?
) {
    fun itemName(): String? = item?.element?.qualName?.editorText()
    fun functionId(moveProject: MoveProject): String? = item?.element?.functionId(moveProject)

    fun parametersRequired(): Boolean {
        val fn = item?.element ?: return false
        return when {
            fn.isView -> fn.typeParameters.isNotEmpty() || fn.parameters.isNotEmpty()
            fn.isEntry -> fn.typeParameters.isNotEmpty() || fn.transactionParameters.isNotEmpty()
            else -> true
        }
    }

    companion object {
        fun empty(): FunctionCall = FunctionCall(null, mutableMapOf(), mutableMapOf(), null, null, null, null)

        fun template(function: MvFunction, packageId: String?, gasId: String?, gasBudget: String?): FunctionCall {
            val typeParameterNames = function.functionParameterList?.getFunctionParameterList()?.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames!!) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = function.parametersAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, FunctionCallParam?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }
            return FunctionCall(
                function.asSmartPointer(),
                nullTypeParams,
                nullParams,
                packageId,
                null,
                gasId,
                gasBudget
            )
        }
    }
}
