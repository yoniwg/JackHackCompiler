package vmToHack.vm

import vmToHack.asm.*


sealed class VmSegment {

    class Constant(val value: Int) : VmSegment()

    sealed class DynamicSeg(val segment: RamConstant, val offset: Int) : VmSegment() {

        class Local(offset: Int) : DynamicSeg(RamConstant.LCL, offset)

        class Arg(offset: Int) : DynamicSeg(RamConstant.ARG, offset)

        class This(offset: Int) : DynamicSeg(RamConstant.THIS, offset)

        class That(offset: Int) : DynamicSeg(RamConstant.THAT, offset)
    }

    sealed class StaticSeg(val staticLocation: StaticLocation) : VmSegment() {

        class Pointer(offset: Int) : StaticSeg(PointerRam(offset))

        class Temp(offset: Int) : StaticSeg(TempRam(offset))

        class Static(fileName: String, offset: Int) : StaticSeg(StaticRam(fileName, offset))
    }
}