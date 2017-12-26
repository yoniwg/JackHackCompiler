import jackToVm.lexical.LexicalParser
import jackToVm.syntactic.ProgramStructureVar
import jackToVm.syntactic.Variable
import vmToHack.vm.VmCommand
import vmToHack.vm.VmLine
import java.io.File
import java.io.IOException

fun main(args : Array<String>) {
    if (args.isEmpty() || args[0].matches(Regex("""[/-]\?|--help"""))) {
        println("Usage: JackHack <filename1.vm | dirName1> [fileName2.vm | dirName2 ...] [-o out-filename: default=filename1.asm]")
        return
    }

    val pair = parseArguments(args)
    val vmFiles = pair.first
    val asmFile = pair.second
    LexicalParser(vmFiles[0]).parseTokens().forEach { print("${it.token} ") }
    Variable.initIterator(LexicalParser(vmFiles[0]).parseTokens().iterator())
    val a = ProgramStructureVar.Class.generateNode()
    println()
    a.printTo(System.out)

//    val millis = measureTimeMillis {
//        asmFile.bufferedWriter().use { asmBW ->
//            val vmCommands = vmFiles.asSequence().flatMap {
//                it.bufferedReader().lineSequence().mapIndexedNotNull { i, line ->
//                    parseVmLine(it.nameWithoutExtension, line, i)
//                }
//            }
//            val asmLines = VmToAsmCompiler.compile(vmCommands)
//            asmLines.forEach {
//                asmBW.write(it.getAsString())
//                asmBW.newLine()
//            }
//        }
//    }
//
//    println("$asmFile was created successfully in: $millis ms")
}

private fun parseArguments(args: Array<String>): Pair<List<File>, File> {
    val indexOfOutFile = args.indexOf("-o")
    if (indexOfOutFile == 0) throw IllegalArgumentException("No vm file is provided")
    val rawFiles = (if (indexOfOutFile == -1) args.toList() else args.filterIndexed { i, _ -> (i < indexOfOutFile) }).map { File(it) }

    rawFiles.forEach { if (!it.exists()) throw IOException("No such file or directory: " + it) }
    val vmFiles = rawFiles.flatMap {
        if (it.isDirectory) it.listFiles({ f -> f.extension == "vm" }).asList() else listOf(it)
    }.requireNoNulls()

    var asmFile = vmFiles[0].absoluteFile.parentFile.resolve(vmFiles[0].nameWithoutExtension + ".asm")
    if (indexOfOutFile > 0) {
        asmFile = File(args[indexOfOutFile + 1])
    }
    asmFile.delete()
    asmFile.createNewFile()
    return Pair(vmFiles, asmFile)
}

fun parseVmLine(fileName : String, lineStr: String, index: Int): VmCommand? {
    if (lineStr.startsWith("//") || lineStr.trim().isEmpty()) return null
    return VmLine(fileName, lineStr, index).parseCommand()
}
