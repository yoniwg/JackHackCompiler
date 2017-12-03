package vmToHack.vm

import com.sun.deploy.trace.TraceLevel.TEMP
import vmToHack.asm.*


sealed class VmSegment {

    abstract fun push() : Array<AsmLine>
    abstract fun pop() : Array<AsmLine>

    class Constant(private val value: Int): VmSegment(){

        override fun push() = increaseSpBy1 + setATo(value) + insertAToTopStack

        override fun pop(): Array<AsmLine> {
            throw NotImplementedError("pop is not allowed on constant")
        }
    }

    abstract class DynamicSeg(private val segment: RamConstant, private val offset: Int) : VmSegment(){
        override fun push(): Array<AsmLine> {
            return increaseSpBy1 + setAToRamLocationValue(segment) + copyAToD + setATo(offset) +
                    AsmLine.CCommand(AMReg.A, Comp.DOpReg(BinaryOpCode.ADD, AMReg.A)) +
                    copyMToA + insertAToTopStack
        }

        override fun pop(): Array<AsmLine> {
            TODO()
        }
    }

    abstract class StaticSeg(private val staticLocation: StaticLocation) : VmSegment(){
        override fun push(): Array<AsmLine> {
            return arrayOf(setATo(staticLocation), copyMToA)
        }

        override fun pop(): Array<AsmLine> {
            return arrayOf(copyAToD, setATo(staticLocation), copyDToM)
        }
    }

    class Local(offset: Int) : DynamicSeg(RamConstant.LCL, offset)

    class Arg(offset: Int) : DynamicSeg(RamConstant.ARG, offset)

    class This(offset: Int) : DynamicSeg(RamConstant.THIS, offset)

    class That(offset: Int) : DynamicSeg(RamConstant.THAT, offset)

    class Temp(offset: Int) : StaticSeg(TempRam(offset))

    class Static(offset: Int) : StaticSeg(StaticRam(offset))
}