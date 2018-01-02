package jackToVm.syntactic

import jackToVm.compilerElements.*

sealed class ProgramStructureVar : Variable(){

    object Class : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.Class {
            Terminal(Keyword.CLASS).assert(sp.nextToken())
            val className = ClassName.generateNode(sp)
            Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
            val classVarDecs = listClassVarDecs(sp)
            val subroutineDecs = listSubroutineDecs(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            return Node.Class(className, classVarDecs, subroutineDecs)
        }
    }

    fun listClassVarDecs(sp: SyntacticParser): List<Node.ClassVarDec> {
        val classVarDecsList = mutableListOf<Node.ClassVarDec>()
        var classVarDec = ClassVarDec.generateNode(sp)
        while (classVarDec != null){
            classVarDecsList.add(classVarDec)
            classVarDec = ClassVarDec.generateNode(sp)
        }
        return classVarDecsList
    }


    object ClassVarDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ClassVarDec? {
            val decType = Terminal(Keyword.STATIC, Keyword.FIELD).matchingOrNull(sp.tipToken) ?:
                    return null
            sp.nextToken()
            val type = Type.generateNode(sp)
            val varNames = listVarNames(sp)
            Terminal(Symbol.SEMI_COL).assert(sp.nextToken())
            return when(decType) {
                Keyword.STATIC -> Node.ClassVarDec.StaticClassVarDec(type, varNames)
                Keyword.FIELD -> Node.ClassVarDec.FieldClassVarDec(type, varNames)
                else -> throw RuntimeException("Shouldn't be else here")
            }
        }
    }


    object Type : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.TypeOrVoid.Type {
            val primitiveOrNull = Terminal(Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN).matchingOrNull(sp.tipToken)
                    ?: return Node.TypeOrVoid.Type.ClassType(ClassName.generateNode(sp))
            sp.nextToken()
            return when (primitiveOrNull){
                Keyword.INT -> Node.TypeOrVoid.Type.IntType
                Keyword.CHAR -> CharType
                Keyword.BOOLEAN -> Node.TypeOrVoid.Type.BooleanType
                else -> throw RuntimeException("Shouldn't be else here")
            }
        }
    }

    fun listSubroutineDecs(sp: SyntacticParser): List<Node.SubroutineDec> {
        val subroutineDecsList = mutableListOf<Node.SubroutineDec>()
        var subroutineDec = SubroutineDec.generateNode(sp)
        while (subroutineDec != null){
            subroutineDecsList.add(subroutineDec)
            subroutineDec = SubroutineDec.generateNode(sp)
        }
        return subroutineDecsList
    }

    object SubroutineDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.SubroutineDec? {
            val subroutineType = Terminal(Keyword.CTOR, Keyword.FUNCTION, Keyword.METHOD).matchingOrNull(sp.tipToken)
                    ?: return null
            sp.nextToken()
            val typeOrVoid = if (Terminal(Keyword.VOID).check(sp.tipToken)) {
                sp.nextToken(); Node.TypeOrVoid.Void} else Type.generateNode(sp)
            val subroutineName = SubroutineName.generateNode(sp)
            val parametersList = mutableListOf<Node.ParameterDec>()
            Terminal(Symbol.L_PAR_RND).assert(sp.nextToken())
            var curToken = Terminal(Symbol.R_PAR_RND).matchingOrNull(sp.tipToken)
            if (curToken == Symbol.R_PAR_RND) sp.nextToken()
            while (curToken != Symbol.R_PAR_RND) {
                parametersList.add(ParameterDec.generateNode(sp))
                curToken = Terminal(Symbol.COMMA, Symbol.R_PAR_RND).matchingOrThrow(sp.nextToken())
            }
            val subroutineBody = SubroutineBody.generateNode(sp)
            return when(subroutineType){
                Keyword.CTOR -> Node.SubroutineDec.ConstructorDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.FUNCTION -> Node.SubroutineDec.FunctionDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.METHOD -> Node.SubroutineDec.MethodDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                else -> throw RuntimeException("Shouldn't be else here")
            }

        }
    }

    object ParameterDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ParameterDec {
            val type = Type.generateNode(sp)
            val varName = VarName.generateNode(sp)
            return Node.ParameterDec(type, varName)
        }
    }

    object SubroutineBody : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.SubroutineBody {
            Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
            val varDecs = listVarDecs(sp)
            val statements = listStatements(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            return Node.SubroutineBody(varDecs, statements)
        }
    }

    fun listVarDecs(sp: SyntacticParser) : List<Node.VarDec>{
        val varDecsList = mutableListOf<Node.VarDec>()
        while (Terminal(Keyword.VAR).check(sp.tipToken)){
            varDecsList.add(VarDec.generateNode(sp))
        }
        return varDecsList
    }

    object VarDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.VarDec {
            Terminal(Keyword.VAR).assert(sp.nextToken())
            val type = Type.generateNode(sp)
            val varNames = listVarNames(sp)
            Terminal(Symbol.SEMI_COL).assert(sp.nextToken())
            return Node.VarDec(type, varNames)
        }
    }

    object ClassName : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ClassName {
            val codeLocation = sp.tipToken.codeLocation
            return Node.ClassName(TerminalType<Identifier>().returnSameTypeOrThrow(sp.nextToken()).idName, codeLocation)
        }
    }

    object SubroutineName : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.SubroutineName {
            val codeLocation = sp.tipToken.codeLocation
            return Node.SubroutineName(TerminalType<Identifier>().returnSameTypeOrThrow(sp.nextToken()).idName, codeLocation)
        }
    }

    fun listVarNames(sp: SyntacticParser) : List<Node.VarName>{
        val varNamesList = mutableListOf(VarName.generateNode(sp))
        while (Terminal(Symbol.COMMA).check(sp.tipToken)){
            sp.nextToken()
            varNamesList.add(VarName.generateNode(sp))
        }
        return varNamesList
    }

    object VarName : ProgramStructureVar(){
        override fun generateNode(sp: SyntacticParser): Node.VarName {
            val codeLocation = sp.tipToken.codeLocation
            return Node.VarName(TerminalType<Identifier>().returnSameTypeOrThrow(sp.nextToken()).idName, codeLocation)
        }
    }
}