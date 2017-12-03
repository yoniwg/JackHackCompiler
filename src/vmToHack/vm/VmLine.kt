package vmToHack.vm

import vmToHack.asm.AsmLine

class VmLine (private val line : String, private val lineNumber : Int ) {
    val lineElements = line.split(" ")
    val command = lineElements[0]

    fun generateAssembly() : Array<AsmLine>{
        return parseCommand().createAsmLines()
    }

    private fun parseCommand(): VmCommand {
        try {
            return when (command){
                "push" -> VmCommand.Push(parseSegment(lineElements[1], lineElements[2]))
                "pop" -> VmCommand.Pop(parseSegment(lineElements[1], lineElements[2]))
                "add" -> VmCommand.Add()
                "sub" -> VmCommand.Sub()
                "and" -> VmCommand.And()
                "or" -> VmCommand.Or()
                "neg" -> VmCommand.Neg()
                "not" -> VmCommand.Not()
                "eq" -> VmCommand.Eq()
                "gt" -> VmCommand.Gt()
                "lt" -> VmCommand.Lt()
                else -> throw IllegalArgumentException(command + " is not a legal vm command")
            }
        }catch (e : Exception){
            throw IllegalArgumentException("line $lineNumber: $line, is illegal vm order: ${e.message}")
        }
    }

    private fun parseSegment(segmentStr: String, numberStr: String): VmSegment {
        val number = numberStr.toInt()
        return when (segmentStr){
            "constant" -> VmSegment.Constant(number)
            "local" -> VmSegment.Local(number)
            "arg" -> VmSegment.Arg(number)
            "this" -> VmSegment.This(number)
            "that" -> VmSegment.That(number)
            "temp" -> VmSegment.Temp(number)
            "static" -> VmSegment.Static(number)
            else -> throw IllegalArgumentException("'$segmentStr' is not a legal vm segment")
        }
    }
}