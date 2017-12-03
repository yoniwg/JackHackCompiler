package vmToHack.vm

import vmToHack.asm.*
private var lc : Int = 0


enum class CompSign {GT,LT,EQ}

val setAToSP = setATo(RamConstant.SP)
val setAToSpValue = arrayOf(setAToSP, copyMToA)
val insertDToTopStack = setAToSpValue + copyDToM
val insertAToTopStack = arrayOf<AsmLine>(copyAToD) + insertDToTopStack
val getTopStackToD = setAToSpValue + copyMToD
val getTopStackToA = setAToSpValue + copyMToA
val increaseSpBy1 = increaseRamLocBy1(RamConstant.SP)
val decreaseSpBy1 = decreaseRamLocBy1(RamConstant.SP)

sealed class VmCommand {
    abstract fun createAsmLines() : Array<AsmLine>

    class Push(private val vmSegment: VmSegment) : VmCommand() {
        override fun createAsmLines() = vmSegment.push()
    }

    class Pop(private val vmSegment: VmSegment) : VmCommand() {
        override fun createAsmLines() = vmSegment.pop()
    }

    abstract class BinaryCommand(private val binaryOpCode: BinaryOpCode) : VmCommand(){
        override fun createAsmLines(): Array<AsmLine> {
            return getTopStackToD +
                    setATo(RamConstant.R13) +
                    copyDToM +
                    decreaseSpBy1 +
                    getTopStackToD +
                    setATo(RamConstant.R13) +
                    AsmLine.CCommand(D, Comp.DOpReg(binaryOpCode, AMReg.M)) +
                    insertDToTopStack
        }
    }

    class Add : BinaryCommand(BinaryOpCode.ADD)

    class Sub : BinaryCommand(BinaryOpCode.SUB)

    class And : BinaryCommand(BinaryOpCode.AND)

    class Or : BinaryCommand(BinaryOpCode.OR)

    abstract class UnaryCommand(private val unaryOpCode: UnaryOpCode) : VmCommand(){
        override fun createAsmLines(): Array<AsmLine> {
            return setAToSpValue + AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(unaryOpCode, AMReg.M))
        }
    }

    class Neg : UnaryCommand(UnaryOpCode.NEGATIVE)

    class Not : UnaryCommand(UnaryOpCode.NOT)

    abstract class ComparisionCommand(private val compSign : CompSign) : VmCommand() {
        override fun createAsmLines(): Array<AsmLine> {
            val jumpCode = when (compSign){
                CompSign.GT -> JOpCode.JGT
                CompSign.LT -> JOpCode.JLT
                CompSign.EQ -> JOpCode.JEQ
            }
            lc++
            val ifTrueLbl = "IF_TRUE_$lc"
            val endLbl = "END_$lc"
            return getTopStackToD +
                    setATo(RamConstant.R13) +
                    copyDToM +
                    decreaseSpBy1 +
                    getTopStackToD +
                    setATo(RamConstant.R13) +
                    AsmLine.CCommand(D, Comp.DOpReg(BinaryOpCode.SUB, AMReg.M)) + // set D to subtraction
                    AsmLine.ACommand(ifTrueLbl) +                                 // set A to if-true label
                    AsmLine.CCommand(null, Comp.UnaryOpReg(D), jumpCode) +   // jump according to D
                    setAToSpValue +
                    AsmLine.CCommand(AMReg.M, Comp.Zero) +                        // if false set M=0
                    AsmLine.ACommand(endLbl) +                                    // set A to end label
                    AsmLine.CCommand(null,Comp.Zero,JOpCode.JMP) +           // jump to end
                    AsmLine.CreateLabel(ifTrueLbl)+                               // (if_true)
                    setAToSpValue +
                    AsmLine.CCommand(AMReg.M, Comp.One(false)) +          // set M=-1
                    AsmLine.CreateLabel(endLbl)                                   // (end)
        }
    }

    class Eq : ComparisionCommand(CompSign.EQ)

    class Gt : ComparisionCommand(CompSign.GT)

    class Lt : ComparisionCommand(CompSign.LT)

}

