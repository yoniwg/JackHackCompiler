package jackToVm.syntactic

import jackToVm.CodeLocation
import jackToVm.compilerElements.*


sealed class ExpressionVar : Variable(){

    val opTerminal = Terminal(Symbol.PLUS, Symbol.MINUS, Symbol.ASTRX, Symbol.SLASH,
            Symbol.AMPRSND, Symbol.PIPE, Symbol.L_PAR_ANGL, Symbol.R_PAR_ANGL, Symbol.ASSIGN)

    object Expression : ExpressionVar() {
        override fun generateNode(): Node.Term.Expression {
            val term = Term.generateNode()
            val opTerm =
            if (opTerminal.check(tipToken)){
                OpTerm.generateNode()
            } else null
            return Node.Term.Expression(term, opTerm)
        }
    }

    object Term : ExpressionVar() {
        override fun generateNode(): Node.Term {
            return ConstOrNull.generateNode() ?:
                    IdentifierTermOrNull.generateNode() ?:
                    ParenthesizedExpressionOrNull.generateNode() ?:
                    UnaryOpTerm.generateNode()
        }
    }

    object OpTerm : ExpressionVar() {
        override fun generateNode(): Node.OpTerm {
            val opTok = opTerminal.matchingOrThrow(nextToken())
            val op = when (opTok){
                Symbol.PLUS -> Op.ADD
                Symbol.MINUS -> Op.SUB
                Symbol.ASTRX -> Op.MULT
                Symbol.SLASH -> Op.DIV
                Symbol.AMPRSND -> Op.AND
                Symbol.PIPE -> Op.OR
                Symbol.L_PAR_ANGL -> Op.LT
                Symbol.R_PAR_ANGL -> Op.GT
                Symbol.ASSIGN -> Op.EQUAL
                else -> throw RuntimeException("Shouldn't be else here")
            }
            return Node.OpTerm(op, Term.generateNode())
        }
    }

    object ConstOrNull : ExpressionVar() {
        override fun generateNode(): Node.Term.Const? {
            val token = TerminalType<IntConstant>().returnSameTypeOrNull(tipToken)?.uintVal ?:
                    TerminalType<StringConstant>().returnSameTypeOrNull(tipToken)?.string ?:
                    Terminal(Keyword.TRUE,Keyword.FALSE, Keyword.NULL, Keyword.THIS).matchingOrNull(tipToken) ?: return null
            nextToken()
            return when (token){
                is Int -> Node.Term.Const.IntConst(token)
                is String -> Node.Term.Const.StringConst(token)
                Keyword.TRUE, Keyword.FALSE -> Node.Term.Const.BooleanConst(token == Keyword.TRUE)
                Keyword.NULL -> Node.Term.Const.Null
                Keyword.THIS -> Node.Term.Const.This
                else -> throw throw RuntimeException("Shouldn't be else here")
            }
        }
    }

    object IdentifierTermOrNull : ExpressionVar(){ // this require LL(1), thus I made some workarounds
        override fun generateNode(): Node.Term.IdentifierTerm? {
            val codeLocation = tipToken.codeLocation
            val idTok = TerminalType<Identifier>().returnSameTypeOrNull(tipToken) ?: return null
            nextToken()
            val afterIdTok = Terminal(Symbol.L_PAR_SQR, Symbol.L_PAR_RND, Symbol.PERIOD).matchingOrNull(tipToken)
            return when(afterIdTok){
                Symbol.L_PAR_SQR -> VarArray(idTok, codeLocation).generateNode()
                Symbol.L_PAR_RND, Symbol.PERIOD -> SubroutineCall(idTok, codeLocation).generateNode()
                else -> Node.Term.IdentifierTerm.VarTerm(Node.VarName(idTok.idName, codeLocation))
            }
        }

    }

    class VarArray(val idTok: Identifier? = null, val codeLocation: CodeLocation?) : ExpressionVar(){
        override fun generateNode(): Node.Term.IdentifierTerm.VarArrayTerm {
            val codeLocation = codeLocation ?: tipToken.codeLocation
            val idTok = idTok ?: TerminalType<Identifier>().returnSameTypeOrThrow(tipToken)
            Terminal(Symbol.L_PAR_SQR).assert(nextToken())
            val arrayOffset = Expression.generateNode()
            Terminal(Symbol.R_PAR_SQR).assert(nextToken())
            return Node.Term.IdentifierTerm.VarArrayTerm(Node.VarName(idTok.idName, codeLocation), arrayOffset)
        }
    }

    class SubroutineCall(val idTok: Identifier? = null, val codeLocation: CodeLocation?) : ExpressionVar(){
        override fun generateNode(): Node.Term.IdentifierTerm.SubroutineCall {
            val codeLocation = codeLocation ?: tipToken.codeLocation
            val idTok = idTok ?: TerminalType<Identifier>().returnSameTypeOrThrow(nextToken())
            val periodOrPar = Terminal(Symbol.PERIOD, Symbol.L_PAR_RND).matchingOrThrow(nextToken())
            var subroutineSource : Node.SubroutineSource? = null
            var subroutineName = Node.SubroutineName(idTok.idName, codeLocation)
            if (periodOrPar == Symbol.PERIOD){
                subroutineSource = Node.SubroutineSource(Node.VarOrClassName(idTok.idName))
                subroutineName = ProgramStructureVar.SubroutineName.generateNode()
                Terminal(Symbol.L_PAR_RND).matchingOrThrow(nextToken())
            }
            val expressionsList : Node.ExpressionsList? =
            if (!Terminal(Symbol.R_PAR_RND).check(tipToken)){
                ExpressionsList.generateNode()

            } else null
            Terminal(Symbol.R_PAR_RND).assert(nextToken())
            return Node.Term.IdentifierTerm.SubroutineCall(subroutineSource,subroutineName,expressionsList)
        }
    }

    object ExpressionsList : ExpressionVar() {
        override fun generateNode(): Node.ExpressionsList {
            val expression = Expression.generateNode()
            if (Terminal(Symbol.COMMA).check(tipToken)){
                nextToken()
                return Node.ExpressionsList(expression, ExpressionsList.generateNode())
            }
            return Node.ExpressionsList(expression, null)
        }
    }

    object ParenthesizedExpressionOrNull : ExpressionVar() {
        override fun generateNode(): Node.Term.Expression? {
            if (Terminal(Symbol.L_PAR_RND).check(tipToken)){
                nextToken()
                val expression = Expression.generateNode()
                Terminal(Symbol.R_PAR_RND).assert(nextToken())
                return expression
            }
            return null
        }
    }

    object UnaryOpTerm : ExpressionVar() {
        override fun generateNode(): Node.Term.UnaryOp {
            val opToken = Terminal(Symbol.MINUS, Symbol.TILDE).matchingOrThrow(nextToken())
            val unaryOp = when (opToken){
                Symbol.MINUS -> UnaryOp.NEGATIVE
                Symbol.TILDE -> UnaryOp.NOT
                else -> throw RuntimeException("Shouldn't be else here")
            }
            val term = Term.generateNode()
            return Node.Term.UnaryOp(unaryOp, term)
        }
    }

}
