package jackToVm.compilerElements

fun String.tryIntConstant() = if (this.matches(Regex("[0-9]+"))) IntConstant(this) else null
fun String.tryStringConstant() = if (this.startsWith("\"") && this.endsWith("\"")) StringConstant(this) else null
fun String.tryBooleanConstant() = if (this == "true" || this == "false") BooleanConstant(this) else null
fun String.tryNullConstant() = if (this == "null") NullConstant else null
fun String.tryIdentifier() = if (this.matches(Regex("[A-Za-z_]+[A-Za-z0-9_]*"))) Identifier(this) else null

interface Token {
    companion object {
        fun parseToken(token : String): Token =
                        Symbol.values().firstOrNull { it.char == token } as Token?
                        ?: Keyword.values().firstOrNull { it.keyword == token }
                        ?: token.tryIntConstant()
                        ?: token.tryStringConstant()
                        ?: token.tryBooleanConstant()
                        ?: token.tryNullConstant()
                        ?: token.tryIdentifier()
                        ?: throw Exception("'$token' is not a legal Jack token")
    }

    object EOF : Token
}

enum class Symbol(val char: String) : Token {
    L_PAR_RND("("), R_PAR_RND(")"),
    L_PAR_SQR("["), R_PAR_SQR("]"),
    L_PAR_CRL("{"), R_PAR_CRL("}"),
    L_PAR_ANGL("<"), R_PAR_ANGL(">"),
    COMMA(","), SEMI_COL(";"), ASSIGN("="),
    PERIOD("."), PLUS("+"), MINUS("-"),
    ASTRX("*"), SLASH("/"), AMPRSND("&"),
    PIPE("|"), TILDE("~")
}


enum class Keyword(keyword : String? = null) : Token {
    CLASS("class"), CTOR("constructor"), METHOD,
    FUNCTION, INT, BOOLEAN, CHAR, VOID, VAR("var"), STATIC,
    FIELD, LET, DO("do"), IF("if"), ELSE("else"),
    WHILE("while"), RETURN("return"), TRUE("true"),
    FALSE("false"), NULL("null"), THIS("this");
    val keyword = keyword ?: name.toLowerCase()
}

data class IntConstant(val uintVal: Int) : Token {
    constructor(uintTok : String) : this(uintTok.toInt())
    override fun toString() = "'$uintVal'"
}

data class StringConstant(val string : String) : Token {
    override fun toString() = "'${string.substring(0,20)}...'"

}

data class BooleanConstant(val boolVal : Boolean) : Token {
    constructor(boolTok: String) : this(boolTok.toBoolean())
    override fun toString() = "'$boolVal'".toUpperCase()
}

object NullConstant : Token {
    override fun toString() = "'NULL'"
}

data class Identifier(val idName: String) : Token {
    override fun toString() = "ID_'$idName'"
}
