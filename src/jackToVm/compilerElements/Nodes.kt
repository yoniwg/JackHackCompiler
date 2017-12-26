package jackToVm.compilerElements

import jackToVm.CodeLocation
import jackToVm.compilerElements.Node.Term.Expression
import jackToVm.compilerElements.Node.Term.IdentifierTerm.SubroutineCall
import jackToVm.compilerElements.Node.TypeOrVoid.Type
import jackToVm.lexical.FileToken
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties


enum class Op { ADD, SUB, DIV, MULT, AND, OR, LT, GT, EQUAL }
enum class UnaryOp { NEGATIVE, NOT}

sealed class Node() {
    
    class Class(val className: ClassName, val classVarDecs: ClassVarDecs?, val subroutineDecs: SubroutineDecs?) : Node()
    class ClassVarDecs(val classVarDec: ClassVarDec, val classVarDecs: ClassVarDecs?) : Node()
    class SubroutineDecs(val subroutineDec: SubroutineDec, val subroutineDecs: SubroutineDecs?) : Node()
    sealed class ClassVarDec(val type: Type, val varNames: VarNames) : Node() {
        class StaticClassVarDec(type: Type, varNames: VarNames) : ClassVarDec(type, varNames)
        class FieldClassVarDec(type: Type, varNames: VarNames) : ClassVarDec(type, varNames)
    }

    sealed class TypeOrVoid : Node() {
        object Void : TypeOrVoid()
        sealed class Type : TypeOrVoid() {
            object IntType : Type()
            object CharType : Type()
            object BooleanType : Type()
            data class ClassType(val className: ClassName) : Type()
        }
    }

    class VarNames(val varName: VarName, val varNames: VarNames?) : Node()
    sealed class SubroutineDec(val retType: TypeOrVoid, val subroutineName: SubroutineName, val parametersList: ParametersList?, val subroutineBody: SubroutineBody) : Node() {
        class ConstructorDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)

        class FunctionDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)

        class MethodDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)
    }
    class ParametersList(val parameterDec : ParameterDec, val parametersList: ParametersList?) : Node()
    class ParameterDec(val type: Type, val paramName : VarName) : Node()
    class SubroutineBody(val varDecs: VarDecs?, val statements : Statements) : Node()
    class VarDecs(val varDec : VarDec, val varDecs : VarDecs? = null) : Node()
    class VarDec(val type: Type, val varNames: VarNames) : Node()
    class ClassName(val className : String) : Node()
    class SubroutineName(val subroutineName: String, val codeLocation: CodeLocation) : Node()
    class VarName(val varName : String, val codeLocation: CodeLocation) : Node()
    class VarOrClassName(val varOrClassName : String) : Node()
    class Statements(val statement: Statement?, val statements: Statements?) : Node()
    sealed class Statement(val codeLocation: CodeLocation) : Node() {
        class LetStatement(val varName: VarName, val arrayOffset: Expression?, val initializer: Expression, codeLocation : CodeLocation) : Statement(codeLocation)
        class IfStatement(val condition: Expression, val statements: Statements, val elseStatements: Statements?, codeLocation : CodeLocation) : Statement(codeLocation)
        class WhileStatement(val condition: Expression, val statements: Statements, codeLocation : CodeLocation) : Statement(codeLocation)
        class DoStatement(val subroutineCall: SubroutineCall, codeLocation : CodeLocation) : Statement(codeLocation)
        class ReturnStatement(val returnExpression: Expression?, codeLocation : CodeLocation) : Statement(codeLocation)
    }
    class OpTerm(val op: Op, val term: Term) : Node()
    sealed class Term : Node() {
        class Expression(val term: Term, val opTerm: OpTerm?) : Term()
        sealed class Const : Term() {
            class IntConst(val intConst: Int) : Const()
            class StringConst(val stringConst: String) : Const()
            class BooleanConst(val booleanConst: Boolean) : Const()
            object Null : Const()
            object This : Const()
        }
        sealed class IdentifierTerm : Term() {
            class VarTerm(val varName: VarName) : IdentifierTerm()
            class VarArrayTerm(val varName: VarName, val arrayOffset: Expression) : IdentifierTerm()
            class SubroutineCall(val subroutineSource: SubroutineSource?, val subroutineName: SubroutineName, val expressionsList: ExpressionsList?) : IdentifierTerm()
        }
        class ExpressionTerm(val expression: Expression) : Term()
        class UnaryOp(val unaryOp: jackToVm.compilerElements.UnaryOp, val term: Term) : Term()
    }
    class SubroutineSource(val varOrClassName: VarOrClassName) : Node()
    //    class VarSubroutineSource(val varName : VarName) : SubroutineSource()
//    class StaticSubroutineSource(val className : ClassName) : SubroutineSource()
    class ExpressionsList(val expression: Expression, val expressionsList: ExpressionsList?) : Node()

    override fun toString(): String = buildString { appendNode(this@Node) }
    fun printTo(appendable: Appendable) {
        appendable.apply { appendNode(this@Node) }
    }

    private fun Appendable.appendNode(
            node: Node,
            prefix: String = "",
            isRoot: Boolean = true,
            lastInPeers: Boolean = true
    ) {

        val memberProperties = node::class.memberProperties
        val props = mutableListOf<Pair<String,String>>()
        val children = mutableListOf<Node>()
        memberProperties.forEach { p ->
            @Suppress("UNCHECKED_CAST")
            p as KProperty1<Node, Any?>
            val name = p.name
            val value = p.get(node)
            when (value) {
                is Node -> children += value
                else -> props += name to value.toString()
            }
        }


        append(prefix)
        if (!isRoot) {
            append(if (lastInPeers) "└── " else "├── ")
        } else {
            append(" ── ")
        }
        appendTitle(node.javaClass.simpleName, props)
        append('\n')

        val indentation = if (isRoot) "    " else "    "
        children.forEachIndexed { index, child ->
            if (index != children.lastIndex) {
                appendNode(child, prefix + if (lastInPeers) indentation else "│   ", false, false)
            } else {
                appendNode(child, prefix + if (lastInPeers) indentation else "│   ", false, true)
            }
        }

//        
//        repeat(offset) { append(" ") }
//        append(if (slash) "\\ " else "| ")
//        appendHeader(node.javaClass.simpleName, props)
//        append(")\n")
//        children.forEachIndexed { index, value ->
//            appendNode(value, offset + 1, index == 0)
//        }
    }

    private fun Appendable.appendTitle(title: String, props: Iterable<Pair<String, String>>) {
        append(title)
        append("(")
        props.joinTo(this, separator = "; ") { (name, value) ->
            append(name)
            append(" = ")
            append(value)
            ""
        }
        append(")")
    }
}

