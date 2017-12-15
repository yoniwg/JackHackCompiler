package vmToHack.vm

import vmToHack.asm.*

private var lc: Int = 0

class VmToAsmCompiler private constructor() {

    private fun init(): Array<AsmLine> = arrayOf(setATo(STACK_START), copyAToD, setAToSP, copyDToM) +
            VmCommand.Call(SYS_INIT_FUNC,0).createAsmLines()

    companion object {

        private const val STACK_START = 256
        private const val SYS_INIT_FUNC = "Sys.init"


        fun compile(commands: Sequence<VmCommand>): Sequence<AsmLine> = with(VmToAsmCompiler()) {
            return init().asSequence() + commands.flatMap { it.createAsmLines().asSequence() }
        }

        private fun setATo(value: Int) = AsmLine.ACommand(value)
        private fun setATo(value: RamLocation) = AsmLine.ACommand(value.location)
        private fun setATo(labelName: String) = AsmLine.ACommand(labelName)
        private val copyAToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.A))
        private val copyMToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
        private val copyMToA = AsmLine.CCommand(AMReg.A, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
        private val copyDToM = AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(D))
        private val copyDToA = AsmLine.CCommand(AMReg.A, Comp.UnaryOpReg(D))
        private val decreaseMBy1 = AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M,false))
        private val decreaseABy1 = AsmLine.CCommand(AMReg.A, Comp.RegOpOne(AMReg.A,false))
        private fun setAToRamLocationValue(ramLocation: RamLocation) = arrayOf(setATo(ramLocation), copyMToA)
        private fun setDToRamLocationValue(ramLocation: RamLocation) = arrayOf(setATo(ramLocation), copyMToD)

        private val setAToSP = setATo(RamConstant.SP)
        private val setAToSpValue = arrayOf(setAToSP, copyMToA)
        private val setDToSpValue = arrayOf(setAToSP, copyMToD)
        private val insertDToTopStack = setAToSpValue + copyDToM
        private val insertAToTopStack = arrayOf<AsmLine>(copyAToD) + insertDToTopStack
        private val getTopStackToD = setAToSpValue + copyMToD
        private val increaseSpBy1 = increaseRamLocBy1(RamConstant.SP)
        private val decreaseSpBy1 = decreaseRamLocBy1(RamConstant.SP)
        private val popStackToD = decreaseSpBy1 + copyMToA + copyMToD
        private fun pushValue(value: Int) = arrayOf<AsmLine>(setATo(value)) + insertAToTopStack + increaseSpBy1
        private fun pushValue(label: String) = arrayOf<AsmLine>(setATo(label)) + insertAToTopStack + increaseSpBy1
        private fun pushRamLocationValue(ramLocation: RamLocation) =
                setAToRamLocationValue(ramLocation) + insertAToTopStack + increaseSpBy1

        private fun increaseRamLocBy1(ramLocation: RamLocation) =
                arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, true))
        private fun decreaseRamLocBy1(ramLocation: RamLocation) =
                arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, false))


    }

    private var currentFunction = ""


    private fun VmSegment.push() : Array<AsmLine> = when (this) {
        is VmSegment.Constant -> pushValue(value)
        is VmSegment.DynamicSeg ->
            setDToRamLocationValue(segment) + setATo(offset) +
                    AsmLine.CCommand(AMReg.A, Comp.DOpReg(BinaryOpCode.ADD, AMReg.A)) +
                    copyMToD + insertDToTopStack + increaseSpBy1
        is VmSegment.StaticSeg -> arrayOf(setATo(staticLocation), copyMToD) + setAToSpValue + copyDToM + increaseSpBy1
    }


    private fun VmSegment.pop() : Array<AsmLine> = when (this) {
        is VmSegment.Constant -> throw NotImplementedError("pop is not allowed on constant")
        is VmSegment.DynamicSeg ->
            arrayOf(setATo(offset), copyAToD) +                                 // store offset in D
                    setAToRamLocationValue(segment) +                               // store first segment address in A
                    AsmLine.CCommand(D,Comp.DOpReg(BinaryOpCode.ADD, AMReg.A))+     // set D = D + A (segment address + offset)
                    setATo(RamConstant.R13) + copyDToM +                            // save in R13
                    popStackToD +
                    setAToRamLocationValue(RamConstant.R13) + copyDToM             // set A to the stored address and set its value to D


        is VmSegment.StaticSeg -> popStackToD + setATo(staticLocation) + copyDToM
    }


    private fun VmCommand.createAsmLines() : Array<AsmLine> = when (this) {
        is VmCommand.Push -> vmSegment.push()

        is VmCommand.Pop -> vmSegment.pop()

        is VmCommand.BinaryCommand ->
            popStackToD + setATo(RamConstant.R13) + copyDToM +
                    popStackToD +
                    setATo(RamConstant.R13) +
                    AsmLine.CCommand(D, Comp.DOpReg(binaryOpCode, AMReg.M)) +
                    insertDToTopStack + increaseSpBy1

        is VmCommand.UnaryCommand ->
            setAToSpValue + decreaseABy1 + AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(unaryOpCode, AMReg.M))

        is VmCommand.ComparisionCommand ->  {
            val jumpCode = when (compSign){
                CompSign.GT -> JOpCode.JGT
                CompSign.LT -> JOpCode.JLT
                CompSign.EQ -> JOpCode.JEQ
            }
            lc++
            val ifTrueLbl = "IF_TRUE\$$lc"
            val endLbl = "END\$$lc"
                popStackToD +
                    setATo(RamConstant.R13) +
                    copyDToM +
                    popStackToD +
                    setATo(RamConstant.R13) +
                    AsmLine.CCommand(D, Comp.DOpReg(BinaryOpCode.SUB, AMReg.M)) + // set D to subtraction
                    AsmLine.ACommand(ifTrueLbl) +                                 // set A to if-true label
                    AsmLine.CCommand(null, Comp.UnaryOpReg(D), jumpCode) +   // jump according to D
                    setAToSpValue +
                    AsmLine.CCommand(AMReg.M, Comp.Zero) +                        // if false set M=0
                    AsmLine.ACommand(endLbl) +                                    // set A to end label
                    AsmLine.CCommand(null, Comp.Zero, JOpCode.JMP) +         // jump to end
                    AsmLine.CreateLabel(ifTrueLbl) +                             // (if_true)
                    setAToSpValue +
                    AsmLine.CCommand(AMReg.M, Comp.One(false)) +          // set M=-1
                    AsmLine.CreateLabel(endLbl) +                                 // (end)
                    increaseSpBy1
        }


        is VmCommand.Label -> arrayOf(AsmLine.CreateLabel("$currentFunction\$$labelName"))

        is VmCommand.Goto -> arrayOf(setATo("$currentFunction\$$labelName"), AsmLine.CCommand(null,Comp.Zero,JOpCode.JMP))

        is VmCommand.IfGoto -> popStackToD + setATo("$currentFunction\$$labelName") + AsmLine.CCommand(null,Comp.UnaryOpReg(D),JOpCode.JNE)

        is VmCommand.Function -> {
            currentFunction = funcName
            arrayOf(AsmLine.CreateLabel(funcName) as AsmLine) +
                    Array(nVars,{ arrayOf(setAToSP, AsmLine.CCommand(AMReg.M,Comp.Zero)) + increaseSpBy1}).flatten().toTypedArray()
        }


        is VmCommand.Call -> {
            lc++
            val retAddress = "$funcName\$return$$lc"
            pushValue(retAddress) +
                    pushRamLocationValue(RamConstant.LCL) +
                    pushRamLocationValue(RamConstant.ARG) +
                    pushRamLocationValue(RamConstant.THIS) +
                    pushRamLocationValue(RamConstant.THAT) +
                    setDToSpValue + setATo(nArgs + 5) +                                                 // calculate SP - nArgs - 5
                    AsmLine.CCommand(D, Comp.DOpReg(BinaryOpCode.SUB, AMReg.A)) +                       // store in D
                    setATo(RamConstant.ARG) + copyDToM +                                                // set to ARG segment
                    setDToSpValue + setATo(RamConstant.LCL) + copyDToM +                                // set LCL = SP
                    setATo(funcName) + AsmLine.CCommand(null,Comp.Zero,JOpCode.JMP) +               // goto function labal
                    AsmLine.CreateLabel(retAddress)                                                     // create return address label

        }

        is VmCommand.Return -> {
            arrayOf(setATo(RamConstant.LCL), copyMToD) +                                                        // D = frame = LCL
                    setATo(5) + AsmLine.CCommand(AMReg.A, Comp.DOpReg(BinaryOpCode.SUB, AMReg.A)) +             // A = frame - 5
                    copyMToD + setATo(RamConstant.R13) + copyDToM +                                             // R13 = retAddress = M = *(frame - 5)
                    popStackToD + setAToRamLocationValue(RamConstant.ARG) + copyDToM +                          // *ARG = pop
                    setATo(RamConstant.ARG) + copyMToD + AsmLine.CCommand(D, Comp.RegOpOne(D,true)) +   // D = ARG + 1
                    setAToSP + copyDToM +                                                                       // SP = D = ARG + 1
                    decreaseLclBy1AndCopyValueTo(RamConstant.THAT) +                                            // THAT = --LCL = frame - 1
                    decreaseLclBy1AndCopyValueTo(RamConstant.THIS) +                                            // THIS = --LCL = frame - 2
                    decreaseLclBy1AndCopyValueTo(RamConstant.ARG) +                                             // ARG = --LCL = frame - 3
                    decreaseLclBy1AndCopyValueTo(RamConstant.LCL) +                                             // LCL = --LCL = frame - 4
                    setAToRamLocationValue(RamConstant.R13) + AsmLine.CCommand(null, Comp.Zero, JOpCode.JMP) // goto retAddress (R13)
        }


    }

    private fun decreaseLclBy1AndCopyValueTo(ramConstant: RamConstant) =
            arrayOf(setATo(RamConstant.LCL), decreaseMBy1) +
                    setAToRamLocationValue(RamConstant.LCL) + copyMToD + setATo(ramConstant) + copyDToM

}