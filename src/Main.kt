import jackToVm.JackToVmCompiler
import java.io.File
import java.io.IOException
import java.nio.file.FileSystem

fun main(args : Array<String>) {
    if (args.getOrNull(0)?.matches(Regex("""[/-]\?|--help""")) == true) {
        println("Usage: JackHack <dirName1> [dirName2 ...] [options]")
        println("options:")
        println("-asm [asm-out-filename (default=Main.asm)]")
        return
    }

    val pair = parseArguments(args)
    val dirs = pair.first
    val asmFile = pair.second

    Compiler(dirs, asmFile).compile()
}

private fun parseArguments(args: Array<String>): Pair<List<File>, File?> {
    val indexOfOutFile = args.indexOf("-asm")
    val firstOptArg = args.indexOfFirst { it.startsWith("-") }
    val dirsStr = (if (firstOptArg <= 0) listOf(System.getProperty("user.dir")) else listOf()) +
        if (firstOptArg == -1) args.toList() else args.take(firstOptArg)
    val dirs = dirsStr.map(::File)
    dirs.forEach { if (!it.isDirectory) throw IOException("No such directory: " + it) }

    val asmFile = if (indexOfOutFile != -1) if (args.lastIndex >= indexOfOutFile + 1) File(args[indexOfOutFile + 1]) else File("Main.asm") else null
    return Pair(dirs, asmFile)
}


