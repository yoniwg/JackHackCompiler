package vmToHack.vm

import vmToHack.asm.BinaryOpCode
import vmToHack.asm.UnaryOpCode

enum class CompSign { GT, LT, EQ }

sealed class VmCommand {

    class Push(val vmSegment: VmSegment) : VmCommand()

    class Pop(val vmSegment: VmSegment) : VmCommand()

    sealed class BinaryCommand(val binaryOpCode: BinaryOpCode) : VmCommand() {

        object Add : BinaryCommand(BinaryOpCode.ADD)

        object Sub : BinaryCommand(BinaryOpCode.SUB)

        object And : BinaryCommand(BinaryOpCode.AND)

        object Or : BinaryCommand(BinaryOpCode.OR)
    }

    sealed class UnaryCommand(val unaryOpCode: UnaryOpCode) : VmCommand() {

        object Neg : UnaryCommand(UnaryOpCode.NEGATIVE)

        object Not : UnaryCommand(UnaryOpCode.NOT)
    }

    sealed class ComparisionCommand(val compSign : CompSign) : VmCommand() {

        object Eq : ComparisionCommand(CompSign.EQ)

        object Gt : ComparisionCommand(CompSign.GT)

        object Lt : ComparisionCommand(CompSign.LT)
    }

    class Label(val labelName : String) : VmCommand()

    class Goto(val labelName : String) : VmCommand()

    class IfGoto(val labelName : String) : VmCommand()

    class Function(val funcName: String, val nVars: Int) : VmCommand()

    class Call(val funcName: String, val nArgs: Int) : VmCommand()

    object Return : VmCommand()

    class Comment(val comment : String) : VmCommand()

}

