package vmToHack.vm

import vmToHack.asm.*

class VmToAsmCompiler private constructor() {

    private var lc: Int = 0

    companion object {

        private const val STACK_START = 256 - 1 //Start stack at the previous location of start position

        fun compile(commands: Sequence<VmCommand>): Sequence<AsmLine> = with(VmToAsmCompiler()) {
            return initSP.asSequence() + commands.flatMap { it.createAsmLines().asSequence() } + testWorkaround
        }

        private fun setATo(value: Int) = AsmLine.ACommand(value)
        private fun setATo(value: RamLocation) = AsmLine.ACommand(value.location)
        private val copyAToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.A))
        private val copyMToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
        private val copyMToA = AsmLine.CCommand(AMReg.A, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
        private val copyDToM = AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(D))

        private fun setAToRamLocationValue(ramLocation: RamLocation) = arrayOf(setATo(ramLocation), copyMToA)

        private val setAToSP = setATo(RamConstant.SP)
        private val setAToSpValue = arrayOf(setAToSP, copyMToA)
        private val insertDToTopStack = setAToSpValue + copyDToM
        private val insertAToTopStack = arrayOf<AsmLine>(copyAToD) + insertDToTopStack
        private val getTopStackToD = setAToSpValue + copyMToD
        private val getTopStackToA = setAToSpValue + copyMToA
        private val increaseSpBy1 = increaseRamLocBy1(RamConstant.SP)
        private val decreaseSpBy1 = decreaseRamLocBy1(RamConstant.SP)

        private fun increaseRamLocBy1(ramLocation: RamLocation) =
                arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, true))
        private fun decreaseRamLocBy1(ramLocation: RamLocation) =
                arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, false))

        private val initSP = arrayOf(setATo(STACK_START), copyAToD, setAToSP, copyDToM)

        private val testWorkaround = increaseSpBy1
    }

    private fun VmSegment.push() : Array<AsmLine> = when (this) {
        is VmSegment.Constant -> increaseSpBy1 + setATo(value) + insertAToTopStack
        is VmSegment.DynamicSeg ->
            increaseSpBy1 + setAToRamLocationValue(segment) + copyAToD + setATo(offset) +
                    AsmLine.CCommand(AMReg.A, Comp.DOpReg(BinaryOpCode.ADD, AMReg.A)) +
                    copyMToA + insertAToTopStack
        is VmSegment.StaticSeg -> arrayOf(setATo(staticLocation), copyMToA)
    }
    private fun VmSegment.pop() : Array<AsmLine> = when (this) {
        is VmSegment.Constant -> throw NotImplementedError("pop is not allowed on constant")
        is VmSegment.DynamicSeg -> TODO()
        is VmSegment.StaticSeg -> arrayOf(copyAToD, setATo(staticLocation), copyDToM)
    }


    private fun VmCommand.createAsmLines() : Array<AsmLine> = when (this) {
        is VmCommand.Push -> vmSegment.push()

        is VmCommand.Pop -> vmSegment.pop()

        is VmCommand.BinaryCommand ->
            getTopStackToD +
                    setATo(RamConstant.R13) +
                    copyDToM +
                    decreaseSpBy1 +
                    getTopStackToD +
                    setATo(RamConstant.R13) +
                    AsmLine.CCommand(D, Comp.DOpReg(binaryOpCode, AMReg.M)) +
                    insertDToTopStack

        is VmCommand.UnaryCommand ->
            setAToSpValue + AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(unaryOpCode, AMReg.M))

        is VmCommand.ComparisionCommand ->  {
            val jumpCode = when (compSign){
                CompSign.GT -> JOpCode.JGT
                CompSign.LT -> JOpCode.JLT
                CompSign.EQ -> JOpCode.JEQ
            }
            lc++
            val ifTrueLbl = "IF_TRUE_$lc"
            val endLbl = "END_$lc"
            getTopStackToD +
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
                    AsmLine.CCommand(null, Comp.Zero, JOpCode.JMP) +           // jump to end
                    AsmLine.CreateLabel(ifTrueLbl) +                               // (if_true)
                    setAToSpValue +
                    AsmLine.CCommand(AMReg.M, Comp.One(false)) +          // set M=-1
                    AsmLine.CreateLabel(endLbl)                                   // (end)
        }
    }


}