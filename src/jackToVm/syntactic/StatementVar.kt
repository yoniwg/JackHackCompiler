package jackToVm.syntactic

import jackToVm.compilerElements.Keyword
import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Symbol
import jackToVm.compilerElements.Token

sealed class StatementVar : Variable(){
    object Statements : StatementVar() {
        override fun generateNode(): Node.Statements {
            val curToken =
                    Terminal(Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN).matchingOrNull(tipToken)
            val statement = when (curToken){
                Keyword.LET -> LetStatement.generateNode()
                Keyword.IF -> IfStatement.generateNode()
                Keyword.WHILE -> WhileStatement.generateNode()
                Keyword.DO -> DoStatement.generateNode()
                Keyword.RETURN -> ReturnStatement.generateNode()
                else -> return Node.Statements(null,null)
            }
            return Node.Statements(statement, Statements.generateNode())
        }
    }

    object LetStatement : StatementVar() {
        override fun generateNode(): Node.LetStatement {
            Terminal(Keyword.LET).assert(nextToken)
            val varName = ProgramStructureVar.VarName.generateNode()
            val arrayOffsetExp = ArrayExpression.generateNode()
            Terminal(Symbol.ASSIGN).assert(nextToken)
            val intializerExp = ExpressionVar.Expression.generateNode()
            return Node.LetStatement(varName,arrayOffsetExp,intializerExp)
        }
    }

    object ArrayExpression : StatementVar() {
        override fun generateNode(): Node.Expression? {
            if (!Terminal(Symbol.L_PAR_SQR).check(tipToken)) return null
            nextToken
            val expression = ExpressionVar.Expression.generateNode()
            Terminal(Symbol.R_PAR_SQR).assert(nextToken)
            return expression
        }
    }

    object IfStatement : StatementVar() {
        override fun generateNode(): Node.IfStatement {
            Terminal(Keyword.IF).assert(nextToken)
            Terminal(Symbol.L_PAR_RND).assert(nextToken)
            val conditionExp = ExpressionVar.Expression.generateNode()
            Terminal(Symbol.R_PAR_RND).assert(nextToken)
            Terminal(Symbol.L_PAR_CRL).assert(nextToken)
            val trueStatements = Statements.generateNode()
            Terminal(Symbol.R_PAR_CRL).assert(nextToken)
            val falseStatements =
            if (Terminal(Keyword.ELSE).check(tipToken)){
                nextToken
                Terminal(Symbol.L_PAR_CRL).assert(nextToken)
                val falseStatements = Statements.generateNode()
                Terminal(Symbol.R_PAR_CRL).assert(nextToken)
                falseStatements
            } else null
            return Node.IfStatement(conditionExp,trueStatements,falseStatements)
        }
    }

    object WhileStatement : StatementVar() {
        override fun generateNode(): Node.WhileStatement {
            Terminal(Keyword.WHILE).assert(nextToken)
            Terminal(Symbol.L_PAR_RND).assert(nextToken)
            val conditionExp = ExpressionVar.Expression.generateNode()
            Terminal(Symbol.R_PAR_RND).assert(nextToken)
            Terminal(Symbol.L_PAR_CRL).assert(nextToken)
            val statements = Statements.generateNode()
            Terminal(Symbol.R_PAR_CRL).assert(nextToken)
            return Node.WhileStatement(conditionExp,statements)
        }
    }

    object DoStatement : StatementVar() {
        override fun generateNode(): Node.DoStatement {
            Terminal(Keyword.DO).assert(nextToken)
            return Node.DoStatement(ExpressionVar.SubroutineCall.generateNode())
        }
    }

    object ReturnStatement : StatementVar() {
        override fun generateNode(): Node.ReturnStatement {
            Terminal(Keyword.RETURN).assert(nextToken)
            val expression =
                    if (Terminal(Symbol.SEMI_COL).check(tipToken)) null
                    else ExpressionVar.Expression.generateNode()
            return Node.ReturnStatement(expression)
        }
    }
}
