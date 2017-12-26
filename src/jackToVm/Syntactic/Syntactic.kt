package jackToVm.syntactic

import jackToVm.codeGen.VmCodeGenerationException
import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Token
import jackToVm.lexical.EOFTok
import jackToVm.lexical.FileToken

private fun mismatchException(token : FileToken) =
        VmCodeGenerationException(token.codeLocation, "Token '${token.token}' was unexpected in this context")

class Terminal(private vararg val tokens: Token) {

    fun check(token: FileToken) = tokens.any { it == token.token }
    fun assert(token: FileToken) {
            if (!check(token)) throw mismatchException(token)
    }
    fun matchingOrNull(token: FileToken) = tokens.firstOrNull { it == token.token }
    fun matchingOrThrow(token: FileToken) = matchingOrNull(token) ?: throw mismatchException(token)
}

class TerminalType<out T : Token>(private val tokenType: Class<T>) {
    companion object {
        inline operator fun <reified T : Token> invoke() = TerminalType(T::class.java)
    }

    fun returnSameTypeOrThrow(token: FileToken): T {
        if (token.token.javaClass != tokenType) throw mismatchException(token)
        @Suppress("UNCHECKED_CAST")
        return token.token as T
    }

    fun returnSameTypeOrNull(token: FileToken): T? {
        if (token.token.javaClass != tokenType) return null
        @Suppress("UNCHECKED_CAST")
        return token.token as T
    }
}

abstract class Variable {
    companion object {
        private lateinit var iterator : Iterator<FileToken>
        lateinit var tipToken : FileToken private set
        fun initIterator(iterator: Iterator<FileToken>){
            this.iterator = iterator
            tipToken = iterator.next()
        }

        fun nextToken(): FileToken {
            print("${tipToken.token} ")
            val currentToken = tipToken
            tipToken = if (iterator.hasNext()) iterator.next() else EOFTok
            return currentToken
        }


    }
    abstract fun generateNode(): Node?

}