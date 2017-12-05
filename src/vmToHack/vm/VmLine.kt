package vmToHack.vm

class VmLine (private val fileName : String, private val line : String, private val lineNumber : Int ) {

    private val lineElements = line.split(Regex("\\s"))

    private val command = lineElements[0]

    fun parseCommand(): VmCommand {
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
                "label" -> VmCommand.Label(lineElements[1])
                "goto" -> VmCommand.Goto(lineElements[1])
                "if-goto" -> VmCommand.IfGoto(lineElements[1])
                "function" -> VmCommand.Function(lineElements[1], lineElements[2].toInt())
                "call" -> VmCommand.Call(lineElements[1], lineElements[2].toInt())
                "return" -> VmCommand.Return()
                else -> throw IllegalArgumentException(command + " is not a legal vm command")
            }
        } catch (e : Exception){
            throw IllegalArgumentException("line $lineNumber: $line, is illegal vm order: ${e.message}")
        }
    }

    private fun parseSegment(segmentStr: String, numberStr: String): VmSegment {
        val number = numberStr.toInt()
        return when (segmentStr){
            "constant" -> VmSegment.Constant(number)
            "local" -> VmSegment.Local(number)
            "argument" -> VmSegment.Arg(number)
            "this" -> VmSegment.This(number)
            "that" -> VmSegment.That(number)
            "pointer" -> VmSegment.Pointer(number)
            "temp" -> VmSegment.Temp(number)
            "static" -> VmSegment.Static(fileName, number)
            else -> throw IllegalArgumentException("'$segmentStr' is not a legal vm segment")
        }
    }

}