package jackToVm.syntactic

import jackToVm.CodeLocation
import jackToVm.compilerElements.*


sealed class ExpressionVar : Variable(){

    val opTerminal = Terminal(Symbol.PLUS, Symbol.MINUS, Symbol.ASTRX, Symbol.SLASH,
            Symbol.AMPRSND, Symbol.PIPE, Symbol.L_PAR_ANGL, Symbol.R_PAR_ANGL, Symbol.ASSIGN)

    object Expression : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term.Expression {
            val term = Term.generateNode(sp)
            val opTerm =
            if (opTerminal.check(sp.tipToken)){
                OpTerm.generateNode(sp)
            } else null
            return Node.Term.Expression(term, opTerm)
        }
    }

    object Term : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term {
            return ConstOrNull.generateNode(sp) ?:
                    IdentifierTermOrNull.generateNode(sp) ?:
                    ParenthesizedExpressionOrNull.generateNode(sp) ?:
                    UnaryOpTerm.generateNode(sp)
        }
    }

    object OpTerm : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.OpTerm {
            val opTok = opTerminal.matchingOrThrow(sp.nextToken())
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
            return Node.OpTerm(op, Expression.generateNode(sp))
        }
    }

    object ConstOrNull : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term.Const? {
            val codeLocation = sp.tipToken.codeLocation
            val token = TerminalType<IntConstant>().returnSameTypeOrNull(sp.tipToken)?.uintVal ?:
                    TerminalType<StringConstant>().returnSameTypeOrNull(sp.tipToken)?.string ?:
                    Terminal(Keyword.TRUE,Keyword.FALSE, Keyword.NULL, Keyword.THIS).matchingOrNull(sp.tipToken) ?: return null
            sp.nextToken()
            return when (token){
                is Int -> Node.Term.Const.IntConst(token, codeLocation)
                is String -> Node.Term.Const.StringConst(token.drop(1).dropLast(1), codeLocation)
                Keyword.TRUE, Keyword.FALSE -> Node.Term.Const.BooleanConst(token == Keyword.TRUE,codeLocation)
                Keyword.NULL -> Node.Term.Const.Null(codeLocation)
                Keyword.THIS -> Node.Term.Const.This(codeLocation)
                else -> throw throw RuntimeException("Shouldn't be else here")
            }
        }
    }

    object IdentifierTermOrNull : ExpressionVar(){ // this require LL(1), thus I made some workarounds
        override fun generateNode(sp: SyntacticParser): Node.Term.IdentifierTerm? {
            val codeLocation = sp.tipToken.codeLocation
            val idTok = TerminalType<Identifier>().returnSameTypeOrNull(sp.tipToken) ?: return null
            sp.nextToken()
            val afterIdTok = Terminal(Symbol.L_PAR_SQR, Symbol.L_PAR_RND, Symbol.PERIOD).matchingOrNull(sp.tipToken)
            return when(afterIdTok){
                Symbol.L_PAR_SQR -> VarArray(idTok, codeLocation).generateNode(sp)
                Symbol.L_PAR_RND, Symbol.PERIOD -> SubroutineCall(idTok, codeLocation).generateNode(sp)
                else -> Node.Term.IdentifierTerm.VarTerm(Node.VarName(idTok.idName, codeLocation))
            }
        }

    }

    class VarArray(val idTok: Identifier? = null, val codeLocation: CodeLocation?) : ExpressionVar(){
        override fun generateNode(sp: SyntacticParser): Node.Term.IdentifierTerm.VarArrayTerm {
            val codeLocation = codeLocation ?: sp.tipToken.codeLocation
            val idTok = idTok ?: TerminalType<Identifier>().returnSameTypeOrThrow(sp.tipToken)
            Terminal(Symbol.L_PAR_SQR).assert(sp.nextToken())
            val arrayOffset = Expression.generateNode(sp)
            Terminal(Symbol.R_PAR_SQR).assert(sp.nextToken())
            return Node.Term.IdentifierTerm.VarArrayTerm(Node.VarName(idTok.idName, codeLocation), arrayOffset)
        }
    }

    class SubroutineCall(val idTok: Identifier? = null, val codeLocation: CodeLocation?) : ExpressionVar(){
        override fun generateNode(sp: SyntacticParser): Node.Term.IdentifierTerm.SubroutineCall {
            val codeLocation = codeLocation ?: sp.tipToken.codeLocation
            val idTok = idTok ?: TerminalType<Identifier>().returnSameTypeOrThrow(sp.nextToken())
            val periodOrPar = Terminal(Symbol.PERIOD, Symbol.L_PAR_RND).matchingOrThrow(sp.nextToken())
            var subroutineSource : Node.SubroutineSource? = null
            var subroutineName = Node.SubroutineName(idTok.idName, codeLocation)
            if (periodOrPar == Symbol.PERIOD){
                subroutineSource = Node.SubroutineSource(Node.VarOrClassName(idTok.idName,codeLocation))
                subroutineName = ProgramStructureVar.SubroutineName.generateNode(sp)
                Terminal(Symbol.L_PAR_RND).matchingOrThrow(sp.nextToken())
            }
            val expressionsList : Node.ExpressionsList? =
            if (!Terminal(Symbol.R_PAR_RND).check(sp.tipToken)){
                ExpressionsList.generateNode(sp)

            } else null
            Terminal(Symbol.R_PAR_RND).assert(sp.nextToken())
            return Node.Term.IdentifierTerm.SubroutineCall(subroutineSource,subroutineName,expressionsList)
        }
    }

    object ExpressionsList : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.ExpressionsList {
            val expression = Expression.generateNode(sp)
            if (Terminal(Symbol.COMMA).check(sp.tipToken)){
                sp.nextToken()
                return Node.ExpressionsList(expression, ExpressionsList.generateNode(sp))
            }
            return Node.ExpressionsList(expression, null)
        }
    }

    object ParenthesizedExpressionOrNull : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term.Expression? {
            if (Terminal(Symbol.L_PAR_RND).check(sp.tipToken)){
                sp.nextToken()
                val expression = Expression.generateNode(sp)
                Terminal(Symbol.R_PAR_RND).assert(sp.nextToken())
                return expression
            }
            return null
        }
    }

    object UnaryOpTerm : ExpressionVar() {
        override fun generateNode(sp: SyntacticParser): Node.Term.UnaryOp {
            val opToken = Terminal(Symbol.MINUS, Symbol.TILDE).matchingOrThrow(sp.nextToken())
            val unaryOp = when (opToken){
                Symbol.MINUS -> UnaryOp.NEGATIVE
                Symbol.TILDE -> UnaryOp.NOT
                else -> throw RuntimeException("Shouldn't be else here")
            }
            val term = Term.generateNode(sp)
            return Node.Term.UnaryOp(unaryOp, term)
        }
    }

}
