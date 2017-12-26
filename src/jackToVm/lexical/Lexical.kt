package jackToVm.lexical

import jackToVm.CodeLocation
import jackToVm.EOF
import jackToVm.compilerElements.Token
import jackToVm.compilerElements.Token.Companion.parseToken
import java.io.File

open class FileToken(val codeLocation: CodeLocation, val token : Token)

object EOFTok : FileToken(EOF, Token.EOF)

class LexicalParser(private val file: File){
    var commentedOut = false
    var i = 0

    fun parseTokens()  = file.bufferedReader().lineSequence().flatMap { parseOneLine(++i, it) }

    fun parseOneLine(lineNumber : Int, line: String): Sequence<FileToken> {
        var lineToParse = line.replace(Regex("/\\*.*\\*/"),"")
        if (lineToParse.contains("*/")) commentedOut = false
        if (commentedOut) return emptySequence()
        if (lineToParse.contains("/*")) commentedOut = true
        lineToParse = lineToParse.substringBefore("//").substringBefore("/*").substringAfter("*/")
        return Regex("""[^"\W]+|[^\w\s"]|"[^"]*"""").findAll(lineToParse)
                .map { FileToken(CodeLocation(file, lineNumber), parseToken(it.value)) }
    }
}
