package vmToHack.vm

import vmToHack.asm.*


sealed class VmSegment(val offset: Int) {

    class Constant(val value: Int) : VmSegment(value)

    sealed class DynamicSeg(val segment: RamConstant, offset: Int) : VmSegment(offset) {

        class Local(offset: Int) : DynamicSeg(RamConstant.LCL, offset)

        class Arg(offset: Int) : DynamicSeg(RamConstant.ARG, offset)

        class This(offset: Int) : DynamicSeg(RamConstant.THIS, offset)

        class That(offset: Int) : DynamicSeg(RamConstant.THAT, offset)
    }

    sealed class StaticSeg(val staticLocation: StaticLocation, offset: Int) : VmSegment(offset) {

        class Pointer(offset: Int) : StaticSeg(PointerRam(offset), offset)

        class Temp(offset: Int) : StaticSeg(TempRam(offset), offset)

        class Static(fileName: String, offset: Int) : StaticSeg(StaticRam(fileName, offset), offset)
    }
}