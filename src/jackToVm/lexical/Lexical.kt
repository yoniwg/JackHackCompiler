package jackToVm.lexical

import jackToVm.compilerElements.Token
import jackToVm.compilerElements.Token.Companion.parseToken
import java.io.File

val delimsRegex = """[^A-Za-z0-9_]"""
var commentedOut = false
fun parseTokens(file: File)  = file.bufferedReader().lineSequence().flatMap { parseOneLine(it) }

fun parseOneLine(it: String): Sequence<Token> {
    var lineToParse = it.replace(Regex("/\\*.*\\*/"),"")
    if (lineToParse.contains("*/")) commentedOut = false
    if (commentedOut) return emptySequence()
    if (lineToParse.contains("/*")) commentedOut = true
    lineToParse = lineToParse.substringBefore("//").substringBefore("/*").substringAfter("*/")
    return lineToParse.split(Regex("[\\s]+"))
            .flatMap { it.split(Regex("(?<=${delimsRegex})|(?=${delimsRegex})")) }
            .filter { it.isNotBlank() }
            .map { parseToken(it) }
            .asSequence()
}
