import vmToHack.vm.*
import java.io.*

var vmFile = File("in.vm")
var asmFile = File("out.asm")

fun main(args : Array<String>){
    if (args.isEmpty() || args[0].matches(Regex("""[/-]\?|--help"""))) {
        println("Usage: JackHack <filename> [out filename]")
        return
    }
    vmFile = File(args[0])

    if (args.size > 1) {
        asmFile = File(args[1])
    }else{
        asmFile = vmFile.absoluteFile.parentFile.resolve(vmFile.nameWithoutExtension + ".asm")
    }

    if (!vmFile.exists()){
        throw IOException("No such file: " + vmFile)
    }
    asmFile.delete()
    asmFile.createNewFile()

    val asmBW = asmFile.bufferedWriter()
    val vmCommands =
            vmFile.bufferedReader()
            .lineSequence()
            .mapIndexedNotNull { i, it -> parseVmLine(it, i) }
    val asmLines = VmToAsmCompiler.compile(vmCommands)
    asmLines.forEach {
        asmBW.write(it.getAsString())
        asmBW.newLine()
    }
    asmBW.close()
}

fun parseVmLine(lineStr: String, index: Int): VmCommand? {
    if (lineStr.startsWith("//") || lineStr.trim().isEmpty()) return null
    return VmLine(lineStr, index).parseCommand()
}
