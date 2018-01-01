package vmToHack.vm

import java.io.File

class VmLine (private val fileName : String, private val line : String, private val lineNumber : Int ) {

    companion object {
        fun parseFile(file : File) : Sequence<VmCommand>{
            return  file.bufferedReader().lineSequence().mapIndexedNotNull { i, line ->
                parseVmLine(file.nameWithoutExtension, line, i)
            }
        }

        fun parseVmLine(fileName : String, lineStr: String, index: Int): VmCommand? {
            if (lineStr.startsWith("//") || lineStr.trim().isEmpty()) return null
            return VmLine(fileName, lineStr, index).parseCommand()
        }
    }

    private val lineElements = line.split(Regex("\\s"))

    private val command = lineElements[0]

    fun parseCommand(): VmCommand {
        try {
            return when (command){
                "push" -> VmCommand.Push(parseSegment(lineElements[1], lineElements[2]))
                "pop" -> VmCommand.Pop(parseSegment(lineElements[1], lineElements[2]))
                "add" -> VmCommand.BinaryCommand.Add
                "sub" -> VmCommand.BinaryCommand.Sub
                "and" -> VmCommand.BinaryCommand.And
                "or" -> VmCommand.BinaryCommand.Or
                "neg" -> VmCommand.UnaryCommand.Neg
                "not" -> VmCommand.UnaryCommand.Not
                "eq" -> VmCommand.ComparisionCommand.Eq
                "gt" -> VmCommand.ComparisionCommand.Gt
                "lt" -> VmCommand.ComparisionCommand.Lt
                "label" -> VmCommand.Label(lineElements[1])
                "goto" -> VmCommand.Goto(lineElements[1])
                "if-goto" -> VmCommand.IfGoto(lineElements[1])
                "function" -> VmCommand.Function(lineElements[1], lineElements[2].toInt())
                "call" -> VmCommand.Call(lineElements[1], lineElements[2].toInt())
                "return" -> VmCommand.Return
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
            "local" -> VmSegment.DynamicSeg.Local(number)
            "argument" -> VmSegment.DynamicSeg.Arg(number)
            "this" -> VmSegment.DynamicSeg.This(number)
            "that" -> VmSegment.DynamicSeg.That(number)
            "pointer" -> VmSegment.StaticSeg.Pointer(number)
            "temp" -> VmSegment.StaticSeg.Temp(number)
            "static" -> VmSegment.StaticSeg.Static(fileName, number)
            else -> throw IllegalArgumentException("'$segmentStr' is not a legal vm segment")
        }
    }

}