import vmToHack.vm.*
import java.io.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlin.system.measureTimeMillis

fun main(args : Array<String>) {
    if (args.isEmpty() || args[0].matches(Regex("""[/-]\?|--help"""))) {
        println("Usage: JackHack <filename1.vm> [filename2.vm filename3.vm ...] [-o out-filename: default=filename.asm]")
        return
    }
        val indexOfOutFile = args.indexOf("-o")
        if (indexOfOutFile == 0) throw IllegalArgumentException("No vm file is provided")
        val vmFiles = (if (indexOfOutFile == -1) args.toList() else args.filterIndexed { i, _ -> (i < indexOfOutFile) }).map { File(it) }

        var asmFile = vmFiles[0].absoluteFile.parentFile.resolve(vmFiles[0].nameWithoutExtension + ".asm")
        if (indexOfOutFile > 0) {
            asmFile = File(args[indexOfOutFile + 1])
        }

        vmFiles.forEach { if (!it.exists()) throw IOException("No such file: " + it) }

        asmFile.delete()
        asmFile.createNewFile()

    val millis = measureTimeMillis {
        asmFile.bufferedWriter().use { asmBW ->
            val vmCommands = vmFiles.asSequence().flatMap {
                it.bufferedReader().lineSequence().mapIndexedNotNull { i, line ->
                    parseVmLine(it.nameWithoutExtension, line, i)
                }
            }
            val asmLines = VmToAsmCompiler.compile(vmCommands)
            asmLines.forEach {
                asmBW.write(it.getAsString())
                asmBW.newLine()
            }
        }
    }

    println("$asmFile was created successfully in: $millis ms")
}

fun parseVmLine(fileName : String, lineStr: String, index: Int): VmCommand? {
    if (lineStr.startsWith("//") || lineStr.trim().isEmpty()) return null
    return VmLine(fileName, lineStr, index).parseCommand()
}
