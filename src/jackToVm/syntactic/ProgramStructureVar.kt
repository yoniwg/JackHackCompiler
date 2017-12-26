package jackToVm.syntactic

import jackToVm.compilerElements.*

sealed class ProgramStructureVar : Variable(){

    object Class : ProgramStructureVar() {
        override fun generateNode(): Node.Class {
            Terminal(Keyword.CLASS).assert(nextToken())
            val className = ClassName.generateNode()
            Terminal(Symbol.L_PAR_CRL).assert(nextToken())
            val classVarDecs = ClassVarDecs.generateNode()
            val subroutineDecs = SubroutineDecs.generateNode()
            Terminal(Symbol.R_PAR_CRL).assert(nextToken())
            return Node.Class(className, classVarDecs, subroutineDecs)
        }
    }

    object ClassVarDecs : ProgramStructureVar() {
        override fun generateNode(): Node.ClassVarDecs? {
            val classVarDec = ClassVarDec.generateNode() ?: return null
            return Node.ClassVarDecs(classVarDec, ClassVarDecs.generateNode())
        }
    }

    object ClassVarDec : ProgramStructureVar() {
        override fun generateNode(): Node.ClassVarDec? {
            val decType = Terminal(Keyword.STATIC, Keyword.FIELD).matchingOrNull(tipToken) ?:
                    return null
            nextToken()
            val type = Type.generateNode()
            val varNames = VarNames.generateNode()
            Terminal(Symbol.SEMI_COL).assert(nextToken())
            return when(decType) {
                Keyword.STATIC -> Node.ClassVarDec.StaticClassVarDec(type, varNames)
                Keyword.FIELD -> Node.ClassVarDec.FieldClassVarDec(type, varNames)
                else -> throw RuntimeException("Shouldn't be else here")
            }
        }
    }


    object Type : ProgramStructureVar() {
        override fun generateNode(): Node.TypeOrVoid.Type {
            val primitiveOrNull = Terminal(Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN).matchingOrNull(tipToken)
                    ?: return Node.TypeOrVoid.Type.ClassType(ClassName.generateNode())
            nextToken()
            return when (primitiveOrNull){
                Keyword.INT -> Node.TypeOrVoid.Type.IntType
                Keyword.CHAR -> Node.TypeOrVoid.Type.CharType
                Keyword.BOOLEAN -> Node.TypeOrVoid.Type.BooleanType
                else -> throw RuntimeException("Shouldn't be else here")
            }
        }
    }

    object SubroutineDecs : ProgramStructureVar() {
        override fun generateNode(): Node.SubroutineDecs? {
            val subroutineDec = SubroutineDec.generateNode() ?: return null
            return Node.SubroutineDecs(subroutineDec, SubroutineDecs.generateNode())
        }
    }

    object SubroutineDec : ProgramStructureVar() {
        override fun generateNode(): Node.SubroutineDec? {
            val subroutineType = Terminal(Keyword.CTOR, Keyword.FUNCTION, Keyword.METHOD).matchingOrNull(tipToken)
                    ?: return null
            nextToken()
            val typeOrVoid = if (Terminal(Keyword.VOID).check(tipToken)) {
                nextToken(); Node.TypeOrVoid.Void} else Type.generateNode()
            val subroutineName = SubroutineName.generateNode()
            Terminal(Symbol.L_PAR_RND).assert(nextToken())
            val parametersList = ParametersList.generateNode()
            Terminal(Symbol.R_PAR_RND).assert(nextToken())
            val subroutineBody = SubroutineBody.generateNode()
            return when(subroutineType){
                Keyword.CTOR -> Node.SubroutineDec.ConstructorDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.FUNCTION -> Node.SubroutineDec.FunctionDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                Keyword.METHOD -> Node.SubroutineDec.MethodDec(typeOrVoid, subroutineName, parametersList, subroutineBody)
                else -> throw RuntimeException("Shouldn't be else here")
            }

        }
    }

    object ParametersList : ProgramStructureVar() {
        override fun generateNode(): Node.ParametersList? {
            if (Terminal(Symbol.R_PAR_RND).check(tipToken)) return null
            val parameterDec = ParameterDec.generateNode()
            if (Terminal(Symbol.COMMA).check(tipToken)){
                nextToken()
                return Node.ParametersList(parameterDec,ParametersList.generateNode())
            }
            return Node.ParametersList(parameterDec,null)
        }
    }

    object ParameterDec : ProgramStructureVar() {
        override fun generateNode(): Node.ParameterDec {
            val type = Type.generateNode()
            val varName = VarName.generateNode()
            return Node.ParameterDec(type, varName)
        }
    }

    object SubroutineBody : ProgramStructureVar() {
        override fun generateNode(): Node.SubroutineBody {
            Terminal(Symbol.L_PAR_CRL).assert(nextToken())
            val varDecs = VarDecs.generateNode()
            val statements = StatementVar.Statements.generateNode()
            Terminal(Symbol.R_PAR_CRL).assert(nextToken())
            return Node.SubroutineBody(varDecs, statements)
        }
    }

    object VarDecs : ProgramStructureVar() {
        override fun generateNode(): Node.VarDecs? {
            if (!Terminal(Keyword.VAR).check(tipToken)) return null
            return Node.VarDecs(VarDec.generateNode(),VarDecs.generateNode())
        }
    }

    object VarDec : ProgramStructureVar() {
        override fun generateNode(): Node.VarDec {
            Terminal(Keyword.VAR).assert(nextToken())
            val type = Type.generateNode()
            val varNames = VarNames.generateNode()
            Terminal(Symbol.SEMI_COL).assert(nextToken())
            return Node.VarDec(type, varNames)
        }
    }

    object ClassName : ProgramStructureVar() {
        override fun generateNode(): Node.ClassName {
            return Node.ClassName(TerminalType<Identifier>().returnSameTypeOrThrow(nextToken()).idName)
        }
    }

    object SubroutineName : ProgramStructureVar() {
        override fun generateNode(): Node.SubroutineName {
            val codeLocation = tipToken.codeLocation
            return Node.SubroutineName(TerminalType<Identifier>().returnSameTypeOrThrow(nextToken()).idName, codeLocation)
        }
    }

    object VarNames : ProgramStructureVar() {
        override fun generateNode(): Node.VarNames {
            val varName = VarName.generateNode()
            if (Terminal(Symbol.COMMA).check(tipToken)){
                nextToken()
                return Node.VarNames(varName,VarNames.generateNode())
            }
            return Node.VarNames(varName,null)
        }
    }

    object VarName : ProgramStructureVar(){
        override fun generateNode(): Node.VarName {
            val codeLocation = tipToken.codeLocation
            return Node.VarName(TerminalType<Identifier>().returnSameTypeOrThrow(nextToken()).idName, codeLocation)
        }
    }
}