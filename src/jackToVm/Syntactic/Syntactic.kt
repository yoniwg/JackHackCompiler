package jackToVm.syntactic

import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Token

private fun mismatchException(token : Token) = Exception("Token '$token' was unexpected in this context")
class Terminal(private vararg val tokens: Token){

    fun check(token: Token) = tokens.map { it == token }.reduce(Boolean::or)
    fun assert(token: Token) {
            if (!check(token)) throw mismatchException(token)
    }
    fun matchingOrNull(token: Token) = tokens.firstOrNull { it == token }
    fun matchingOrThrow(token: Token) = matchingOrNull(token) ?: throw mismatchException(token)
}

class TerminalType<out T : Token>(private val tokenType: Class<T>) {
    companion object {
        inline operator fun <reified T : Token> invoke() = TerminalType(T::class.java)
    }

    fun returnSameTypeOrThrow(token: Token): T {
        if (token.javaClass != tokenType) throw mismatchException(token)
        @Suppress("UNCHECKED_CAST")
        return token as T

    }
}

abstract class Variable {
    companion object {
        private lateinit var iterator : Iterator<Token>
        lateinit var tipToken : Token private set
        fun initIterator(iterator: Iterator<Token>){
            this.iterator = iterator
            tipToken = iterator.next()
        }

        val nextToken: Token
            get() {
                val currentToken = tipToken
                tipToken = if (iterator.hasNext()) iterator.next() else Token.EOF
                return currentToken
            }


    }
    abstract fun generateNode(): Node?

}