package jackToVm.codeGen

import jackToVm.codeGen.CodeGeneration.VarKind.*
import jackToVm.compilerElements.Node
import sun.reflect.generics.scope.AbstractScope
import vmToHack.vm.VmCommand
import vmToHack.vm.VmSegment
import kotlin.test.assertSame

val VmSegment.name get() = when(this){

    is VmSegment.Constant -> "constant"
    is VmSegment.DynamicSeg -> when (this){
        is VmSegment.DynamicSeg.Local -> "local"
        is VmSegment.DynamicSeg.Arg -> "argument"
        is VmSegment.DynamicSeg.This -> "this"
        is VmSegment.DynamicSeg.That -> "that"
    }
    is VmSegment.StaticSeg -> when (this){
        is VmSegment.StaticSeg.Pointer -> "pointer"
        is VmSegment.StaticSeg.Temp -> "temp"
        is VmSegment.StaticSeg.Static -> "static"
    }
}

fun VmCommand.Push.generateCode(number: Int) = "push ${this.vmSegment.name} $number"
fun VmCommand.Pop.generateCode(number: Int) = "push ${this.vmSegment.name} $number"
fun VmCommand.BinaryCommand.generateCode()= when (this){
    is VmCommand.BinaryCommand.Add -> "add"
    is VmCommand.BinaryCommand.Sub -> "sub"
    is VmCommand.BinaryCommand.And -> "and"
    is VmCommand.BinaryCommand.Or -> "or"
}
fun VmCommand.UnaryCommand.generateCode()= when (this){
    is VmCommand.UnaryCommand.Neg -> "neg"
    is VmCommand.UnaryCommand.Not -> "not"
}
fun VmCommand.ComparisionCommand.generateCode() = when (this){
    is VmCommand.ComparisionCommand.Eq -> "eq"
    is VmCommand.ComparisionCommand.Gt -> "gt"
    is VmCommand.ComparisionCommand.Lt -> "lt"
}
fun VmCommand.Label.generateCode(label: String) = "label $label"
fun VmCommand.Goto.generateCode(label: String) = "goto $label"
fun VmCommand.IfGoto.generateCode(label: String) = "if-goto $label"
fun VmCommand.Function.generateCode(functionName: String, nVars : Int) = "goto $functionName $nVars"
fun VmCommand.Call.generateCode(functionName: String, nArgs : Int) = "goto $functionName $nArgs"
fun VmCommand.Return.generateCode() = "return"


class CodeGeneration {

    enum class VarKind {
        FIELD, STATIC, VAR, ARG
    }

    enum class SubroutineKind {
        CTOR, FUNCTION, METHOD
    }

    fun Node.Class.generateCode() : List<VmCommand>{
        return this.classVarDecs?.generateCode(className).orEmpty() +
            this.subroutineDecs?.generateCode(className).orEmpty()
    }

    fun Node.ClassVarDecs.generateCode(className : Node.ClassName) : List<VmCommand>{
        return classVarDec.generateCode(className) +
            this.classVarDecs?.generateCode(className).orEmpty()
    }

    fun Node.SubroutineDecs.generateCode(className : Node.ClassName) : List<VmCommand>{
        return subroutineDec.generateCode(className) +
            this.subroutineDecs?.generateCode(className).orEmpty()
    }

    fun Node.ClassVarDec.generateCode(className : Node.ClassName) : List<VmCommand> {
        return when (this){
            is Node.ClassVarDec.StaticClassVarDec -> varNames.generateCode(STATIC,className,type)
            is Node.ClassVarDec.FieldClassVarDec -> varNames.generateCode(FIELD,className,type)
        }
    }

    fun Node.VarNames.generateCode(varKind: VarKind, className : Node.ClassName?, type: Node.TypeOrVoid) : List<VmCommand> {
        if (varKind == VAR) {
            StackMember.addVariable(varName,type)
        } else {
            if (className == null) throw RuntimeException("$varKind must provide class-name")
            HeapMember.addVariable(varKind, className, varName, type)
        }
        TODO()
        return varNames?.generateCode(varKind,className,type).orEmpty()
    }

    fun Node.SubroutineDec.generateCode(className : Node.ClassName) : List<VmCommand> {
        val parametersList =  parametersList?.aggregateDecs().orEmpty()
        val methodKind = when (this) {
            is Node.SubroutineDec.ConstructorDec -> SubroutineKind.CTOR
            is Node.SubroutineDec.FunctionDec -> SubroutineKind.FUNCTION
            is Node.SubroutineDec.MethodDec -> SubroutineKind.METHOD
        }
        HeapMember.addSubroutine(methodKind, className, subroutineName, retType, parametersList)
        val parametersInit = TODO()
        val subroutineBody = subroutineBody.generateCode(className)
    }
    fun Node.ParametersList.aggregateDecs() : List<Node.ParameterDec>{
        return listOf(parameterDec) + parametersList?.aggregateDecs().orEmpty()
    }
    fun Node.ParameterDec.generateCode() : List<VmCommand>{
        StackMember.addVariable(paramName, type)
        TODO()
    }

    fun Node.SubroutineBody.generateCode(scope : Node.ClassName) : List<VmCommand>{
        return varDecs?.generateCode().orEmpty() +
                statements.generateCode(scope)
    }
    fun Node.VarDecs.generateCode() : List<VmCommand>{
        return varDec.generateCode() +
                varDecs?.generateCode().orEmpty()
    }
    fun Node.VarDec.generateCode() : List<VmCommand>{
        return varNames.generateCode(VAR, null, type)
    }

    fun Node.Statements.generateCode(scope : Node.ClassName) : List<VmCommand>{
        return statement?.generateCode(scope).orEmpty() +
                statements?.generateCode(scope).orEmpty()
    }

    fun Node.Statement.generateCode(scope : Node.ClassName) : List<VmCommand> = when (this){
        is Node.Statement.LetStatement -> {
            val typedVar = VariablesTable.getVarOrFieldOrStatic(varName, scope)
            val expressionType = initializer.getResolvedType()
            assertSameType(typedVar.type, expressionType)
            TODO()
        }
        is Node.Statement.IfStatement -> {
            TODO()
        }
        is Node.Statement.WhileStatement -> TODO()
        is Node.Statement.DoStatement -> TODO()
        is Node.Statement.ReturnStatement -> TODO()
    }

    fun Node.OpTerm.generateCode() : List<VmCommand>{ TODO()}
    fun Node.Term.generateCode() : List<VmCommand>{TODO()}
    fun Node.Term.getResolvedType() : Node.TypeOrVoid{ TODO()}
    fun Node.SubroutineSource.generateCode() : List<VmCommand>{TODO()}
    fun Node.ExpressionsList.generateCode() : List<VmCommand>{TODO()}

    private fun assertSameType(lValType: Node.TypeOrVoid, rValType: Node.TypeOrVoid) {
        if (lValType != rValType){
            throw Exception("let statement's lvalue type must fit rvalue type")
        }
    }
}