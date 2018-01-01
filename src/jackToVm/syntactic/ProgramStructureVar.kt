package jackToVm.syntactic

import jackToVm.compilerElements.*

sealed class ProgramStructureVar : Variable(){

    object Class : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.Class {
            Terminal(Keyword.CLASS).assert(sp.nextToken())
            val className = ClassName.generateNode(sp)
            Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
            val classVarDecs = ClassVarDecs.generateNode(sp)
            val subroutineDecs = SubroutineDecs.generateNode(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            return Node.Class(className, classVarDecs, subroutineDecs)
        }
    }

    object ClassVarDecs : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ClassVarDecs? {
            val classVarDec = ClassVarDec.generateNode(sp) ?: return null
            return Node.ClassVarDecs(classVarDec, ClassVarDecs.generateNode(sp))
        }
    }

    object ClassVarDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ClassVarDec? {
            val decType = Terminal(Keyword.STATIC, Keyword.FIELD).matchingOrNull(sp.tipToken) ?:
                    return null
            sp.nextToken()
            val type = Type.generateNode(sp)
            val varNames = VarNames.generateNode(sp)
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

    object SubroutineDecs : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.SubroutineDecs? {
            val subroutineDec = SubroutineDec.generateNode(sp) ?: return null
            return Node.SubroutineDecs(subroutineDec, SubroutineDecs.generateNode(sp))
        }
    }

    object SubroutineDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.SubroutineDec? {
            val subroutineType = Terminal(Keyword.CTOR, Keyword.FUNCTION, Keyword.METHOD).matchingOrNull(sp.tipToken)
                    ?: return null
            sp.nextToken()
            val typeOrVoid = if (Terminal(Keyword.VOID).check(sp.tipToken)) {
                sp.nextToken(); Node.TypeOrVoid.Void} else Type.generateNode(sp)
            val subroutineName = SubroutineName.generateNode(sp)
            Terminal(Symbol.L_PAR_RND).assert(sp.nextToken())
            val parametersList = ParametersList.generateNode(sp)
            Terminal(Symbol.R_PAR_RND).assert(sp.nextToken())
            val subroutineBody = SubroutineBody.generateNode(sp)
            return when(subroutineType){
                Keyword.CTOR -> Node.SubroutineDec.ConstructorDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.FUNCTION -> Node.SubroutineDec.FunctionDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.METHOD -> Node.SubroutineDec.MethodDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                else -> throw RuntimeException("Shouldn't be else here")
            }

        }
    }

    object ParametersList : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.ParametersList? {
            if (Terminal(Symbol.R_PAR_RND).check(sp.tipToken)) return null
            val parameterDec = ParameterDec.generateNode(sp)
            if (Terminal(Symbol.COMMA).check(sp.tipToken)){
                sp.nextToken()
                return Node.ParametersList(parameterDec,ParametersList.generateNode(sp))
            }
            return Node.ParametersList(parameterDec,null)
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
            val varDecs = VarDecs.generateNode(sp)
            val statements = StatementVar.Statements.generateNode(sp)
            val returnStatements = StatementVar.ReturnStatement.generateNode(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            return Node.SubroutineBody(varDecs, statements, returnStatements)
        }
    }

    object VarDecs : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.VarDecs? {
            if (!Terminal(Keyword.VAR).check(sp.tipToken)) return null
            return Node.VarDecs(VarDec.generateNode(sp),VarDecs.generateNode(sp))
        }
    }

    object VarDec : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.VarDec {
            Terminal(Keyword.VAR).assert(sp.nextToken())
            val type = Type.generateNode(sp)
            val varNames = VarNames.generateNode(sp)
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

    object VarNames : ProgramStructureVar() {
        override fun generateNode(sp: SyntacticParser): Node.VarNames {
            val varName = VarName.generateNode(sp)
            if (Terminal(Symbol.COMMA).check(sp.tipToken)){
                sp.nextToken()
                return Node.VarNames(varName,VarNames.generateNode(sp))
            }
            return Node.VarNames(varName,null)
        }
    }

    object VarName : ProgramStructureVar(){
        override fun generateNode(sp: SyntacticParser): Node.VarName {
            val codeLocation = sp.tipToken.codeLocation
            return Node.VarName(TerminalType<Identifier>().returnSameTypeOrThrow(sp.nextToken()).idName, codeLocation)
        }
    }
}