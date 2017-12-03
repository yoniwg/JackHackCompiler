package vmToHack.asm

interface RamLocation{
    val location : String
}

enum class RamConstant : RamLocation {SP, LCL, ARG, THIS, THAT, R13, R14, R15;
    override val location : String = name
}
interface StaticLocation : RamLocation
class TempRam(offset : Int) : StaticLocation {
    override val location = "R${5 + offset}"
}

class StaticRam(offset : Int) : StaticLocation {
    override val location = "${16 + offset}"
}
