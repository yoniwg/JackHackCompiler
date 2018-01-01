package jackToVm.syntactic

import jackToVm.compilerElements.Keyword
import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Symbol

sealed class StatementVar : Variable(){
    object Statements : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Statements {
            val curToken =
                    Terminal(Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN).matchingOrNull(sp.tipToken)
            val statement = when (curToken){
                Keyword.LET -> LetStatement.generateNode(sp)
                Keyword.IF -> IfStatement.generateNode(sp)
                Keyword.WHILE -> WhileStatement.generateNode(sp)
                Keyword.DO -> DoStatement.generateNode(sp)
                else -> return Node.Statements(null,null)
            }
            return Node.Statements(statement, Statements.generateNode(sp))
        }
    }

    object LetStatement : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Statement.LetStatement {
            val codeLocation = sp.tipToken.codeLocation
            Terminal(Keyword.LET).assert(sp.nextToken())
            val varName = ProgramStructureVar.VarName.generateNode(sp)
            val arrayOffsetExp = ArrayExpression.generateNode(sp)
            Terminal(Symbol.ASSIGN).assert(sp.nextToken())
            val intializerExp = ExpressionVar.Expression.generateNode(sp)
            Terminal(Symbol.SEMI_COL).assert(sp.nextToken())
            return Node.Statement.LetStatement(varName,arrayOffsetExp,intializerExp, codeLocation)
        }
    }

    object ArrayExpression : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term.Expression? {
            if (!Terminal(Symbol.L_PAR_SQR).check(sp.tipToken)) return null
            sp.nextToken()
            val expression = ExpressionVar.Expression.generateNode(sp)
            Terminal(Symbol.R_PAR_SQR).assert(sp.nextToken())
            return expression
        }
    }

    object IfStatement : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Statement.IfStatement {
            val codeLocation = sp.tipToken.codeLocation
            Terminal(Keyword.IF).assert(sp.nextToken())
            Terminal(Symbol.L_PAR_RND).assert(sp.nextToken())
            val conditionExp = ExpressionVar.Expression.generateNode(sp)
            Terminal(Symbol.R_PAR_RND).assert(sp.nextToken())
            Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
            val trueStatements = Statements.generateNode(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            val falseStatements =
            if (Terminal(Keyword.ELSE).check(sp.tipToken)){
                sp.nextToken()
                Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
                val falseStatements = Statements.generateNode(sp)
                Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
                falseStatements
            } else null
            return Node.Statement.IfStatement(conditionExp,trueStatements,falseStatements, codeLocation)
        }
    }

    object WhileStatement : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Statement.WhileStatement {
            val codeLocation = sp.tipToken.codeLocation
            Terminal(Keyword.WHILE).assert(sp.nextToken())
            Terminal(Symbol.L_PAR_RND).assert(sp.nextToken())
            val conditionExp = ExpressionVar.Expression.generateNode(sp)
            Terminal(Symbol.R_PAR_RND).assert(sp.nextToken())
            Terminal(Symbol.L_PAR_CRL).assert(sp.nextToken())
            val statements = Statements.generateNode(sp)
            Terminal(Symbol.R_PAR_CRL).assert(sp.nextToken())
            return Node.Statement.WhileStatement(conditionExp,statements, codeLocation)
        }
    }

    object DoStatement : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.Statement.DoStatement {
            val codeLocation = sp.tipToken.codeLocation
            Terminal(Keyword.DO).assert(sp.nextToken())
            val doNode = Node.Statement.DoStatement(ExpressionVar.SubroutineCall(codeLocation = codeLocation).generateNode(sp), codeLocation)
            Terminal(Symbol.SEMI_COL).assert(sp.nextToken())
            return doNode
        }
    }

    object ReturnStatement : StatementVar() {
        override fun generateNode(sp: SyntacticParser): Node.ReturnStatement {
            val codeLocation = sp.tipToken.codeLocation
            Terminal(Keyword.RETURN).assert(sp.nextToken())
            val expression =
                    if (Terminal(Symbol.SEMI_COL).check(sp.tipToken)) null
                    else ExpressionVar.Expression.generateNode(sp)
            val returnNode = Node.ReturnStatement(expression, codeLocation)
            Terminal(Symbol.SEMI_COL).assert(sp.nextToken())
            return returnNode
        }
    }
}
