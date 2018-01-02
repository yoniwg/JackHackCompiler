package jackToVm.codeGen

import jackToVm.CodeLocation
import jackToVm.codeGen.CodeGeneration.VarKind.*
import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Op
import jackToVm.compilerElements.UnaryOp
import vmToHack.vm.VmCommand
import vmToHack.vm.VmSegment
import java.io.File

val Op.vmCommand get() = when(this){
    Op.ADD -> VmCommand.BinaryCommand.Add
    Op.SUB -> VmCommand.BinaryCommand.Sub
    Op.DIV -> VmCommand.Call("Math.divide", 2)
    Op.MULT -> VmCommand.Call("Math.multiply", 2)
    Op.AND -> VmCommand.BinaryCommand.And
    Op.OR -> VmCommand.BinaryCommand.Or
    Op.LT -> VmCommand.ComparisionCommand.Lt
    Op.GT -> VmCommand.ComparisionCommand.Gt
    Op.EQUAL -> VmCommand.ComparisionCommand.Eq
}

val Op.onType
    get() = when (this){
    Op.ADD, Op.SUB, Op.DIV, Op.MULT,
    Op.LT, Op.GT -> Node.TypeOrVoid.Type.IntType
    Op.EQUAL -> Node.TypeOrVoid.Any
    Op.AND ,Op.OR -> Node.TypeOrVoid.Type.BooleanType
}

val Op.retType get() = when (this){
    Op.ADD, Op.SUB, Op.DIV, Op.MULT -> Node.TypeOrVoid.Type.IntType
    Op.LT, Op.GT, Op.EQUAL,
    Op.AND ,Op.OR -> Node.TypeOrVoid.Type.BooleanType
}

val UnaryOp.vmCommand get() = when (this){
    UnaryOp.NEGATIVE -> VmCommand.UnaryCommand.Neg
    UnaryOp.NOT -> VmCommand.UnaryCommand.Not
}

val UnaryOp.onType get() = when (this){
    UnaryOp.NEGATIVE -> Node.TypeOrVoid.Type.IntType
    UnaryOp.NOT -> Node.TypeOrVoid.Type.BooleanType
}

fun Int?.orZero() = if (this != null) this else 0

class CodeGeneration(private val jackFileName : String, private val classNode: Node.Class) {

    fun generateCode() = classNode.generateCode()

    private val stringClassType = Node.TypeOrVoid.Type.ClassType("String".vmClassName())
    private val arrayClassType = Node.TypeOrVoid.Type.ClassType("Array".vmClassName())
    private val thisClassType get() =  Node.TypeOrVoid.Type.ClassType(className)
    private val thisVarName = Node.VarName("this", CodeLocation(File(""),-1))

    private lateinit var className : Node.ClassName

    var lc = 0


    enum class VarKind {
        FIELD, STATIC, VAR, ARG, THIS
    }

    enum class SubroutineKind {
        CTOR, FUNCTION, METHOD
    }

    private fun VarKind.getSegmentAt(offset : Int) = when (this){
        FIELD -> VmSegment.DynamicSeg.This(offset)
        STATIC -> VmSegment.StaticSeg.Static(className.className,offset)
        VAR -> VmSegment.DynamicSeg.Local(offset)
        ARG -> VmSegment.DynamicSeg.Arg(offset)
        THIS -> VmSegment.StaticSeg.Pointer(0)
    }

    private fun Node.Class.generateCode() : List<VmCommand>{
        if (jackFileName != this@generateCode.className.className){
            throw Exception("Class name must be as same as file name")
        }
        this@CodeGeneration.className = this.className
        val fieldsCount = classVarDecs.map { it.varNames.count() }.sum()
        return subroutineDecs.flatMap { it.generateCode(fieldsCount)}
    }


    private fun Node.VarName.addToSymbolsTableStack(varKind: VarKind, type: Node.TypeOrVoid){
        when (varKind) {
            ARG, VAR -> {
                StackMember.addVariable(varKind, this, type)
            }
            else -> {
                throw RuntimeException("$varKind Should be added to Symbols Table at API phase")
            }
        }
    }

    private fun Node.SubroutineDec.generateCode(fieldsCount: Int): List<VmCommand> {
        val subroutineKind = when (this) {
            is Node.SubroutineDec.ConstructorDec -> SubroutineKind.CTOR
            is Node.SubroutineDec.FunctionDec -> SubroutineKind.FUNCTION
            is Node.SubroutineDec.MethodDec -> SubroutineKind.METHOD
        }
        val isStatic = subroutineKind == SubroutineKind.FUNCTION
        if (!isStatic) StackMember.addVariable(THIS, thisVarName, thisClassType)
        if (subroutineKind == SubroutineKind.METHOD) StackMember.addVariable(ARG, Node.VarName("this", CodeLocation(File(""),-1)),thisClassType)
        parametersList.forEach { it.addToSymbolsTableStack()}
        val varsCount = subroutineBody.varDecs.map { it.varNames.count() }.sum()
        return listOf(VmCommand.Function("${className.className}.${subroutineName.subroutineName}", varsCount)) +
                when (this) {
                    is Node.SubroutineDec.ConstructorDec -> {
                        listOf(VmCommand.Push(VmSegment.Constant(fieldsCount)),
                                VmCommand.Call("Memory.alloc",1),
                                VmCommand.Pop(VmSegment.StaticSeg.Pointer(0)))
                    }
                    is Node.SubroutineDec.MethodDec -> {
                        listOf(VmCommand.Push(VmSegment.DynamicSeg.Arg(0)),
                                VmCommand.Pop(VmSegment.StaticSeg.Pointer(0)))
                    }
                    is Node.SubroutineDec.FunctionDec -> emptyList()
                } + subroutineBody.generateCode(isStatic, retType)
    }

    private fun Node.ParameterDec.addToSymbolsTableStack() {
        paramName.addToSymbolsTableStack(ARG, type)
    }

    private fun Node.SubroutineBody.generateCode(isStatic : Boolean, subroutineRetType : Node.TypeOrVoid) : List<VmCommand>{
        varDecs.forEach { it.addToSymbolsTableStack()}
        return statements.flatMap { it.generateCode(isStatic)} + returnStatement.generateCode(isStatic, subroutineRetType)
    }

    private fun Node.VarDec.addToSymbolsTableStack(){
        varNames.forEach { it.addToSymbolsTableStack(VAR, type)}
    }

    private fun Node.Statement.generateCode(isStatic: Boolean) : List<VmCommand> = try{
        listOf(VmCommand.Comment("${this.javaClass.simpleName} at ${codeLocation.lineNumber}")) +
                when (this){
                    is Node.Statement.LetStatement -> {
                        val indexedVar = SymbolsTable.getVarOrFieldOrStatic(varName, className)
                        assertNotStatic(isStatic, indexedVar.varKind)
                        initializer.generateCode(isStatic, indexedVar.type) +
                                when {
                                    arrayOffset == null ->
                                        listOf(VmCommand.Pop(indexedVar.varKind.getSegmentAt(indexedVar.index)))
                                    indexedVar.type == arrayClassType ->
                                        arrayOffset.generateCode(isStatic,Node.TypeOrVoid.Type.IntType) +
                                                VmCommand.Push(indexedVar.varKind.getSegmentAt(indexedVar.index))+
                                                VmCommand.BinaryCommand.Add +
                                                VmCommand.Pop(VmSegment.StaticSeg.Pointer(1)) +
                                                VmCommand.Pop(VmSegment.DynamicSeg.That(0))
                                    else -> throw TypeMismatchException("${varName.varName} is not an Array instance")
                                }
                    }
                    is Node.Statement.IfStatement -> {
                        val elseLabel = "\$if_goto_${lc++}"
                        val endLabel = "\$if_goto_${lc++}"
                        condition.generateCode(isStatic, Node.TypeOrVoid.Type.BooleanType) +
                                VmCommand.UnaryCommand.Not +
                                VmCommand.IfGoto(elseLabel) +
                                statements.flatMap { it.generateCode(isStatic) } +
                                VmCommand.Goto(endLabel) +
                                VmCommand.Label(elseLabel) +
                                elseStatements.flatMap { it.generateCode(isStatic) } +
                                VmCommand.Label(endLabel)
                    }
                    is Node.Statement.WhileStatement -> {
                        val loopLabel = "\$if_goto_${lc++}"
                        val endLabel = "\$if_goto_${lc++}"
                        listOf(VmCommand.Label(loopLabel)) +
                                condition.generateCode(isStatic, Node.TypeOrVoid.Type.BooleanType) +
                                VmCommand.UnaryCommand.Not +
                                VmCommand.IfGoto(endLabel) +
                                statements.flatMap { it.generateCode(isStatic) } +
                                VmCommand.Goto(loopLabel) +
                                VmCommand.Label(endLabel)
                    }
                    is Node.Statement.DoStatement -> subroutineCall.generateCode(isStatic, Node.TypeOrVoid.Any)+
                            VmCommand.Pop(VmSegment.StaticSeg.Temp(0))

                }
    } catch (e : Exception) {if (e !is VmCodeGenerationException) throw VmCodeGenerationException(codeLocation, e.message ?: "Unknown Error", e) else throw e}

    private fun Node.ReturnStatement.generateCode(isStatic: Boolean, subroutineRetType: Node.TypeOrVoid) : List<VmCommand>{
        try{
            if ((subroutineRetType is Node.TypeOrVoid.Void) != (returnExpression == null)) {
                throw TypeMismatchException("")
            }
            val commands = (returnExpression?.generateCode(isStatic, subroutineRetType) ?:
                    listOf(VmCommand.Push(VmSegment.Constant(0)))) +
                    VmCommand.Return
            StackMember.initStack()
            return commands
        }catch (e : TypeMismatchException){
            throw VmCodeGenerationException(codeLocation, "Subroutine return type must fit its declaration")
        }
    }

    private fun Node.Term.resolveType() : Node.TypeOrVoid = when (this){

        is Node.Term.Expression -> if (opTerm.isEmpty()) { term.resolveType()} else { opTerm[0].term.resolveType() }
        is Node.Term.Const.IntConst -> Node.TypeOrVoid.Type.IntType
        is Node.Term.Const.StringConst -> stringClassType
        is Node.Term.Const.BooleanConst -> Node.TypeOrVoid.Type.BooleanType
        is Node.Term.Const.Null -> throw TypeMismatchException("lvalue cannot refer to 'null'")
        is Node.Term.Const.This -> thisClassType
        is Node.Term.IdentifierTerm.VarTerm -> SymbolsTable.getVarOrFieldOrStatic(varName, className).type
        is Node.Term.IdentifierTerm.VarArrayTerm -> Node.TypeOrVoid.Type.IntType
        is Node.Term.IdentifierTerm.SubroutineCall -> SymbolsTable.getMethodOrFunction(subroutineName, subroutineSource, className).returnedType
        is Node.Term.ExpressionTerm -> expression.resolveType()
        is Node.Term.UnaryOp -> unaryOp.onType
    }

    private fun Node.Term.generateCode(isStatic : Boolean, lvalueType : Node.TypeOrVoid) : List<VmCommand>{
        return when(this){
            is Node.Term.Expression -> {
                if (opTerm.isEmpty()){
                    term.generateCode(isStatic, lvalueType)
                } else {
                    term.generateCode(isStatic, Node.TypeOrVoid.Any) +
                    opTerm.mapIndexed{ i, it ->
                        val nextType = if ( i==opTerm.lastIndex ) lvalueType else opTerm[i+1].op.onType
                        assertSameType(nextType, it.op.retType)
                        it.term.generateCode(isStatic, if (i==0) term.resolveType() else opTerm[i-1].op.retType) + it.op.vmCommand
                    }.flatten()

                }
//                return if (opTerm.isNotEmpty()){
//                    assertSameType(lvalueType, opTerm.op.retType,"'${opTerm.op}' doesn't return '${lvalueType::class.simpleName}'")
//                    val termType = term.resolveType()
//                    term.generateCode(isStatic, termType) + opTerm.generateCode(isStatic, termType)
//                }else {
//                    term.generateCode(isStatic, lvalueType)
//                }
            }
            is Node.Term.Const.IntConst -> {
                assertSameType(lvalueType, Node.TypeOrVoid.Type.IntType)
                return listOf(VmCommand.Push(VmSegment.Constant(intConst)))
            }
            is Node.Term.Const.StringConst -> {
                assertSameType(lvalueType, stringClassType)
                return listOf(VmCommand.Push(VmSegment.Constant(stringConst.length)),
                        VmCommand.Call("String.new", 1)) +
                        stringConst.flatMap { listOf(VmCommand.Push(VmSegment.Constant(it.toInt())),
                                VmCommand.Call("String.appendChar", 2)) }
            }
            is Node.Term.Const.BooleanConst -> {
                assertSameType(lvalueType, Node.TypeOrVoid.Type.BooleanType)
                return if (booleanConst)
                    listOf(VmCommand.Push(VmSegment.Constant(1)), VmCommand.UnaryCommand.Neg)
                else
                    listOf(VmCommand.Push(VmSegment.Constant(0)))
            }
            is Node.Term.Const.Null -> {
                if (lvalueType !is Node.TypeOrVoid.Type.ClassType){
                    throw Exception("null cannot be refer to primitive type")
                }
                return listOf(VmCommand.Push(VmSegment.Constant(0)))
            }
            is Node.Term.Const.This -> {
                assertSameType(lvalueType, thisClassType)
                return listOf(VmCommand.Push(VmSegment.StaticSeg.Pointer(0)))
            }
            is Node.Term.IdentifierTerm.VarTerm -> {
                val indexedVar = SymbolsTable.getVarOrFieldOrStatic(varName, className)
                assertNotStatic(isStatic, indexedVar.varKind)
                assertSameType(lvalueType, indexedVar.type)
                return listOf(VmCommand.Push(indexedVar.varKind.getSegmentAt(indexedVar.index)))
            }
            is Node.Term.IdentifierTerm.VarArrayTerm -> {
                val indexedVar = SymbolsTable.getVarOrFieldOrStatic(varName, className)
                assertSameType(indexedVar.type, arrayClassType,
                        "${varName.varName} is not an Array instance")
                assertNotStatic(isStatic, indexedVar.varKind)
                assertSameType(lvalueType, indexedVar.type)
                return try {
                    arrayOffset.generateCode(isStatic, Node.TypeOrVoid.Type.IntType)
                } catch (e : TypeMismatchException) {throw Exception("Array offset must be of Int type")} +
                        VmCommand.Push(indexedVar.varKind.getSegmentAt(indexedVar.index)) +
                        VmCommand.BinaryCommand.Add +
                        VmCommand.Pop(VmSegment.StaticSeg.Pointer(1)) +
                        VmCommand.Push(VmSegment.DynamicSeg.That(0))
            }
            is Node.Term.IdentifierTerm.SubroutineCall -> {
                val typedSubroutine = SymbolsTable.getMethodOrFunction(subroutineName,subroutineSource, className)
                subroutineSource ?: assertNotStatic(isStatic, typedSubroutine.subroutineKind)
                assertSameType(lvalueType, typedSubroutine.returnedType)
                return if (typedSubroutine.subroutineKind == SubroutineKind.METHOD) {
                    subroutineSource?.generateCode() ?: listOf(VmCommand.Push( VmSegment.StaticSeg.Pointer(0)))
                } else {emptyList()} +
                        if (expressionsList.count() == typedSubroutine.parametersList.count()) {
                            expressionsList.mapIndexed { index, expression ->
                                expression.generateCode(isStatic, typedSubroutine.parametersList[index])
                            }.flatten()
                        } else { throw Exception("number of subroutine arguments mismatch deceleration") } +
                        VmCommand.Call(typedSubroutine.ownerClassName + "." +
                                subroutineName.subroutineName, typedSubroutine.parametersList.size
                                + if (typedSubroutine.subroutineKind == SubroutineKind.METHOD) 1 else 0)
            }
            is Node.Term.ExpressionTerm -> expression.generateCode(isStatic, lvalueType)
            is Node.Term.UnaryOp -> {
                val opType = unaryOp.onType
                term.generateCode(isStatic, opType) +
                        listOf(unaryOp.vmCommand)
            }
        }
    }
    private fun Node.SubroutineSource.generateCode() : List<VmCommand>{
        val indexedVar = SymbolsTable.getVarOrFieldOrStatic(varOrClassName, className)
        return listOf(VmCommand.Push(indexedVar.varKind.getSegmentAt(indexedVar.index)))
    }

    private fun assertSameType(lValType: Node.TypeOrVoid, rValType: Node.TypeOrVoid, msg: String? = null) {
        if (lValType != Node.TypeOrVoid.Any && rValType != Node.TypeOrVoid.Any &&
                lValType != arrayClassType &&
                rValType != arrayClassType &&
                lValType != rValType){
            throw TypeMismatchException(msg ?: "lvalue must fit rvalue")
        }
    }

    private fun assertNotStatic(isStatic: Boolean, varKind: VarKind, msg: String? = null) {
        if (isStatic && varKind == FIELD){
            throw TypeMismatchException(msg ?: "cannot access non-static field from static context")
        }
    }

    private fun assertNotStatic(isStatic: Boolean, subroutineKind: SubroutineKind, msg: String? = null) {
        if (isStatic && subroutineKind != SubroutineKind.FUNCTION){
            throw TypeMismatchException(msg ?: "cannot access non-static method from static context")
        }
    }
}
