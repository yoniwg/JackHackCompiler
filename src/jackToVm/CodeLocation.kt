package jackToVm

import java.io.File

data class CodeLocation(val file : File, val lineNumber : Int)

val EOF = CodeLocation(File(""), -1)