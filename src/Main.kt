import vmToHack.vm.TestWorkAround
import vmToHack.vm.VmInit
import vmToHack.vm.VmLine
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
    var i = 1
    val asmBW = asmFile.bufferedWriter()
    initAsm(asmBW)
    vmFile.forEachLine {
        parseVmLine(it, i++, asmBW)
    }
    testWorkaround(asmBW)
    asmBW.close()
}

fun initAsm(asmBW: BufferedWriter) {
    VmInit().getAssembly().forEach {
        asmBW.write(it.getAsString())
        asmBW.newLine()
    }
}

fun parseVmLine(lineStr: String, index: Int, asmBW: BufferedWriter) {
    if (lineStr.startsWith("//") || lineStr.trim().isEmpty()) return
    VmLine(lineStr, index).generateAssembly().forEach {
        asmBW.write(it.getAsString())
        asmBW.newLine()
    }
}


fun testWorkaround(asmBW: BufferedWriter) {
    TestWorkAround().getAssembly().forEach {
        asmBW.write(it.getAsString())
        asmBW.newLine()
    }
}