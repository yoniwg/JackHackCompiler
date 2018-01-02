package jackToVm.compilerElements

import com.sun.org.apache.xpath.internal.operations.Equals
import jackToVm.CodeLocation
import jackToVm.compilerElements.Node.Term.Expression
import jackToVm.compilerElements.Node.Term.IdentifierTerm.SubroutineCall
import jackToVm.compilerElements.Node.TypeOrVoid.Type
import java.io.File
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.internal.impl.util.ValueParameterCountCheck


enum class Op { ADD, SUB, DIV, MULT, AND, OR, LT, GT, EQUAL }
enum class UnaryOp { NEGATIVE, NOT}
typealias CharType = Type.IntType
sealed class Node {
    
    class Class(val className: ClassName, val classVarDecs: List<ClassVarDec>, val subroutineDecs: List<SubroutineDec>) : Node()
    sealed class ClassVarDec(val type: Type, val varNames: List<VarName>) : Node() {
        class StaticClassVarDec(type: Type, varNames: List<VarName>) : ClassVarDec(type, varNames)
        class FieldClassVarDec(type: Type, varNames: List<VarName>) : ClassVarDec(type, varNames)
    }

    sealed class TypeOrVoid : Node() {
        object Void : TypeOrVoid()
        object Any : TypeOrVoid()
        sealed class Type : TypeOrVoid() {
            object IntType : Type()
            object BooleanType : Type()
            data class ClassType(val className: ClassName) : Type()
        }
    }

    sealed class SubroutineDec(val retType: TypeOrVoid, val subroutineName: SubroutineName, val parametersList: List<ParameterDec>, val subroutineBody: SubroutineBody) : Node() {
        class ConstructorDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: List<ParameterDec>, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)

        class FunctionDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: List<ParameterDec>, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)

        class MethodDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: List<ParameterDec>, subroutineBody: SubroutineBody)
            : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)
    }
    class ParameterDec(val type: Type, val paramName : VarName) : Node()
    class SubroutineBody(val varDecs: List<VarDec>, val statements : List<Statement>) : Node()
    class VarDec(val type: Type, val varNames: List<VarName>) : Node()
    class ClassName(val className : String, val codeLocation: CodeLocation) : Node(){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ClassName

            if (className != other.className) return false

            return true
        }

        override fun hashCode(): Int {
            return className.hashCode()
        }
    }
    class SubroutineName(val subroutineName: String, val codeLocation: CodeLocation) : Node()
    class VarName(val varName : String, val codeLocation: CodeLocation) : Node()
    class VarOrClassName(val varOrClassName : String, val codeLocation: CodeLocation) : Node()
    sealed class Statement(val codeLocation: CodeLocation) : Node() {
        class LetStatement(val varName: VarName, val arrayOffset: Expression?, val initializer: Expression, codeLocation : CodeLocation) : Statement(codeLocation)
        class IfStatement(val condition: Expression, val statements: List<Statement>, val elseStatements: List<Statement>, codeLocation : CodeLocation) : Statement(codeLocation)
        class WhileStatement(val condition: Expression, val statements: List<Statement>, codeLocation : CodeLocation) : Statement(codeLocation)
        class DoStatement(val subroutineCall: SubroutineCall, codeLocation : CodeLocation) : Statement(codeLocation)
        class ReturnStatement(val returnExpression: Expression?, codeLocation : CodeLocation) : Statement(codeLocation)
    }
    class OpTerm(val op: Op, val term: Term) : Node()
    sealed class Term : Node() {
        class Expression(val term: Term, val opTerms: List<OpTerm>) : Term()
        sealed class Const(val codeLocation: CodeLocation) : Term() {
            class IntConst(val intConst: Int, codeLocation: CodeLocation) : Const(codeLocation)
            class StringConst(val stringConst: String, codeLocation: CodeLocation) : Const(codeLocation)
            class BooleanConst(val booleanConst: Boolean, codeLocation: CodeLocation) : Const(codeLocation)
            class Null(codeLocation: CodeLocation) : Const(codeLocation)
            class This(codeLocation: CodeLocation) : Const(codeLocation)
        }
        sealed class IdentifierTerm : Term() {
            class VarTerm(val varName: VarName) : IdentifierTerm()
            class VarArrayTerm(val varName: VarName, val arrayOffset: Expression) : IdentifierTerm()
            class SubroutineCall(val subroutineSource: SubroutineSource?, val subroutineName: SubroutineName, val expressionsList: List<Expression>) : IdentifierTerm()
        }
        class ExpressionTerm(val expression: Expression) : Term()
        class UnaryOp(val unaryOp: jackToVm.compilerElements.UnaryOp, val term: Term) : Term()
    }
    class SubroutineSource(val varOrClassName: VarOrClassName) : Node()

}

