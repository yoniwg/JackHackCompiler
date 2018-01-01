import jackToVm.JackToVmCompiler
import jackToVm.generateStringCode
import jackToVm.syntactic.ProgramStructureVar
import vmToHack.vm.VmToAsmCompiler
import java.io.File
import kotlin.system.measureTimeMillis

class Compiler(private val dirs : List<File>, private val asmFile: File?){
    fun compile(){
        jackToVm()
        if (asmFile != null) vmToHack()
    }

    private fun jackToVm(){
        val millis = measureTimeMillis {
            val jackFiles = dirs.map { Pair(it,it.listFiles{ fn -> fn.extension == "jack"}.toList()) }.toMap()
            jackFiles.forEach { (dir, jackFiles) ->
                jackFiles.let{JackToVmCompiler(it).compile()}.forEach{ (className, vmCommands) ->
                    dir.resolve("$className.vm").printWriter().use { writer ->
                        vmCommands.forEach {
                            writer.println(it.generateStringCode())
                        }
                    }
                }
            }
        }

        println("vm files were created successfully in: $millis ms")
    }

   private fun vmToHack(){
        val millis = measureTimeMillis {
            val vmFiles = dirs.flatMap { it.listFiles{fn -> fn.extension == "vm"}.toList() }
            asmFile!!.bufferedWriter().use { asmBW ->
                VmToAsmCompiler.compileAll(vmFiles).forEach {
                    asmBW.write(it.getAsString())
                    asmBW.newLine()
                }
            }
        }

        println("$asmFile was created successfully in: $millis ms")
    }
}
