package org.koin.compiler.plugin.ir

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

/**
 * Kotlin 2.4 folds receivers and value parameters into IrFunction.parameters, and
 * call arguments are now keyed by the target IrValueParameter. Keep the plugin's
 * IR construction code expressed in the pre-2.4 vocabulary while targeting the
 * new parameter model.
 */
internal var IrFunction.extensionReceiverParameter: IrValueParameter?
    get() = parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
    set(value) {
        parameters = parameters
            .filterNot { it.kind == IrParameterKind.ExtensionReceiver }
            .let { existing -> if (value == null) existing else listOf(value) + existing }
            .withRegularParameterKind()
    }

internal var IrFunction.valueParameters: List<IrValueParameter>
    get() = parameters.filter { it.kind == IrParameterKind.Regular }
    set(value) {
        parameters = parameters
            .filterNot { it.kind == IrParameterKind.Regular }
            .plus(value.onEach { it.kind = IrParameterKind.Regular })
            .withRegularParameterKind()
    }

internal var IrMemberAccessExpression<IrFunctionSymbol>.extensionReceiver: IrExpression?
    get() = symbol.owner.extensionReceiverParameter?.let { arguments[it] }
    set(value) {
        val parameter = symbol.owner.extensionReceiverParameter ?: return
        arguments[parameter] = value
    }

internal val IrMemberAccessExpression<IrFunctionSymbol>.valueArgumentsCount: Int
    get() = symbol.owner.valueParameters.size

internal val IrMemberAccessExpression<IrFunctionSymbol>.typeArgumentsCount: Int
    get() = typeArguments.size

internal fun IrMemberAccessExpression<IrFunctionSymbol>.getValueArgument(index: Int): IrExpression? {
    val parameter = symbol.owner.valueParameters.getOrNull(index) ?: return null
    return arguments[parameter]
}

internal fun IrMemberAccessExpression<IrFunctionSymbol>.putValueArgument(index: Int, value: IrExpression?) {
    val parameter = symbol.owner.valueParameters.getOrNull(index) ?: return
    arguments[parameter] = value
}

internal fun IrMemberAccessExpression<IrFunctionSymbol>.getTypeArgument(index: Int): IrType? =
    typeArguments.getOrNull(index)

internal fun IrMemberAccessExpression<IrFunctionSymbol>.putTypeArgument(index: Int, value: IrType?) {
    while (typeArguments.size <= index) {
        typeArguments.add(null)
    }
    typeArguments[index] = value
}

internal fun IrConstructorCall.getValueArgument(index: Int): IrExpression? {
    val parameter = symbol.owner.valueParameters.getOrNull(index) ?: return null
    return arguments[parameter]
}

internal fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
    val parameter = symbol.owner.valueParameters.firstOrNull { it.name == name } ?: return null
    return arguments[parameter]
}

private fun List<IrValueParameter>.withRegularParameterKind(): List<IrValueParameter> {
    for (parameter in this) {
        if (parameter.kind == IrParameterKind.Regular) {
            parameter.kind = IrParameterKind.Regular
        }
    }
    return this
}
