package vmToHack.asm

interface RamLocation{
    val location : String
}

enum class RamConstant : RamLocation {SP, LCL, ARG, THIS, THAT, R13, R14, R15;
    override val location : String = name
}
interface StaticLocation : RamLocation
class TempRam(private val offset : Int) : StaticLocation {
    override val location = "R${5 + offset}"
}

class StaticRam(private val offset : Int) : StaticLocation {
    override val location = "${16 + offset}"
}

fun setATo(value: Int) = AsmLine.ACommand(value)
fun setATo(value: RamLocation) = AsmLine.ACommand(value.location)
val copyAToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.A))
val copyMToD = AsmLine.CCommand(D, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
val copyMToA = AsmLine.CCommand(AMReg.A, Comp.UnaryOpReg(UnaryOpCode.NONE, AMReg.M))
val copyDToM = AsmLine.CCommand(AMReg.M, Comp.UnaryOpReg(D))

fun setAToRamLocationValue(ramLocation: RamLocation) = arrayOf(setATo(ramLocation), copyMToA)
fun increaseRamLocBy1(ramLocation: RamLocation) =
        arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, true))
fun decreaseRamLocBy1(ramLocation: RamLocation) =
        arrayOf<AsmLine>(setATo(ramLocation)) + AsmLine.CCommand(AMReg.M, Comp.RegOpOne(AMReg.M, false))
