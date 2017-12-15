package jackToVm.compilerElements

import java.util.*
import kotlin.jvm.internal.Reflection
import kotlin.reflect.full.memberProperties

enum class Op { ADD, SUB, DIV, MULT, AND, OR, LT, GT, EQUAL }
enum class UnaryOp { NEGATIVE, NOT}

sealed class Node {

    class Class(val className : ClassName, val classVarDecs : ClassVarDecs?, val subroutineDecs : SubroutineDecs?) : Node()
    class ClassVarDecs(val classVarDec : ClassVarDec, val classVarDecs: ClassVarDecs?) : Node()
    class SubroutineDecs(val subroutineDec : SubroutineDec, val subroutineDecs: SubroutineDecs?) : Node()
    abstract class ClassVarDec(val type : Type, val varNames : VarNames) : Node()
    class StaticClassVarDec(type: Type, varNames: VarNames) : ClassVarDec(type, varNames)
    class FieldClassVarDec(type: Type, varNames: VarNames) : ClassVarDec(type, varNames)
    abstract class TypeOrVoid : Node()
    object Void : TypeOrVoid()
    abstract class Type : TypeOrVoid()
    class IntType : Type()
    class CharType : Type()
    class BooleanType : Type()
    class ClassType(val className: ClassName) : Type()
    class VarNames(val varName : VarName, val varNames: VarNames?) : Node()
    abstract class SubroutineDec(val retType: TypeOrVoid, val subroutineName : SubroutineName, val parametersList : ParametersList?, val subroutineBody : SubroutineBody) : Node()
    class ConstructorDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
        : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)
    class FunctionDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
        : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)
    class MethodDec(retType: TypeOrVoid, subroutineName: SubroutineName, parametersList: ParametersList?, subroutineBody: SubroutineBody)
        : SubroutineDec(retType, subroutineName, parametersList, subroutineBody)
    class ParametersList(val parameterDec : ParameterDec, val parametersList: ParametersList?) : Node()
    class ParameterDec(val type: Type, val paramName : VarName) : Node()
    class SubroutineBody(val varDecs: VarDecs?, val statements : Statements) : Node()
    class VarDecs(val varDec : VarDec, val varDecs : VarDecs? = null) : Node()
    class VarDec(val type: Type, val varNames: VarNames) : Node()
    class ClassName(val className : String) : Node()
    class SubroutineName(val subroutineName: String) : Node()
    class VarName(val varName : String) : Node()
    class Statements(val statement: Statement?, val statements: Statements?) : Node()
    abstract class Statement : Node()
    class LetStatement(val varName: VarName, val arrayOffset: Expression?, val initializer : Expression) : Statement()
    class IfStatement(val condition : Expression, val statements: Statements, val elseStatements: Statements?) : Statement()
    class WhileStatement(val condition : Expression, val statements: Statements) : Statement()
    class DoStatement(val subroutineCall: SubroutineCall) : Statement()
    class ReturnStatement(val returnExpression: Expression?) : Statement()
    class Expression(val lTerm: Term, val op: Op, val rTerm: Term) : Node()
    abstract class Term : Node()
    class IntConst(val intConst: Int) : Term()
    class StringConst(val stringConst: String) : Term()
    class BooleanConst(val booleanConst: Boolean) : Term()
    object Null : Term()
    object This : Term()
    class VarTerm(val varName: VarName) : Term()
    class VarArrayTerm(val varName: VarName, val arrayOffset: Expression) : Term()
    class SubroutineCallTerm(val subroutineCall: SubroutineCall) : Term()
    class ExpressionTerm(val expression: Expression) : Term()
    class UnaryOpTerm(val unaryOp : UnaryOp, val term: Term) : Term()
    class SubroutineCall(val subroutineSource: SubroutineSource?, val subroutineName: SubroutineName, val expressionList: ExpressionList) : Term()
    abstract class SubroutineSource : Node()
    class VarSubroutineSource(val varName : VarName) : SubroutineSource()
    class StaticSubroutineSource(val className : ClassName) : SubroutineSource()
    class ExpressionList(val expression: Expression?, val expressionList: ExpressionList?) : Node()
}

