package vmToHack.vm

import vmToHack.asm.*

enum class CompSign { GT, LT, EQ }

sealed class VmCommand {

    class Push(val vmSegment: VmSegment) : VmCommand()

    class Pop(val vmSegment: VmSegment) : VmCommand()

    abstract class BinaryCommand(val binaryOpCode: BinaryOpCode) : VmCommand()

    class Add : BinaryCommand(BinaryOpCode.ADD)

    class Sub : BinaryCommand(BinaryOpCode.SUB)

    class And : BinaryCommand(BinaryOpCode.AND)

    class Or : BinaryCommand(BinaryOpCode.OR)

    abstract class UnaryCommand(val unaryOpCode: UnaryOpCode) : VmCommand()

    class Neg : UnaryCommand(UnaryOpCode.NEGATIVE)

    class Not : UnaryCommand(UnaryOpCode.NOT)

    abstract class ComparisionCommand(val compSign : CompSign) : VmCommand()

    class Eq : ComparisionCommand(CompSign.EQ)

    class Gt : ComparisionCommand(CompSign.GT)

    class Lt : ComparisionCommand(CompSign.LT)

    class Label(val labelName : String) : VmCommand()

    class Goto(val labelName : String) : VmCommand()

    class IfGoto(val labelName : String) : VmCommand()

    class Function(val funcName: String, val nVars: Int) : VmCommand()

    class Call(val funcName: String, val nArgs: Int) : VmCommand()

    class Return : VmCommand()

}

