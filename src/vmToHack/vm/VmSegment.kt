package vmToHack.vm

import com.sun.deploy.trace.TraceLevel.TEMP
import vmToHack.asm.*


sealed class VmSegment {

    class Constant(val value: Int): VmSegment()

    abstract class DynamicSeg(val segment: RamConstant, val offset: Int) : VmSegment()

    abstract class StaticSeg(val staticLocation: StaticLocation) : VmSegment()

    class Local(offset: Int) : DynamicSeg(RamConstant.LCL, offset)

    class Arg(offset: Int) : DynamicSeg(RamConstant.ARG, offset)

    class This(offset: Int) : DynamicSeg(RamConstant.THIS, offset)

    class That(offset: Int) : DynamicSeg(RamConstant.THAT, offset)

    class Temp(offset: Int) : StaticSeg(TempRam(offset))

    class Static(offset: Int) : StaticSeg(StaticRam(offset))
}