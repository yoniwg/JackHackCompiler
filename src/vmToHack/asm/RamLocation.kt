package vmToHack.asm

interface RamLocation{
    val location : String
}

enum class RamConstant : RamLocation {SP, LCL, ARG, THIS, THAT, R13, R14, R15;
    override val location : String = name
}
interface StaticLocation : RamLocation

class PointerRam(offset : Int) : StaticLocation {
    override val location = "R${3 + offset}"
}

class TempRam(offset : Int) : StaticLocation {
    override val location = "R${5 + offset}"
}

class StaticRam(fileName : String, offset : Int) : StaticLocation {
    override val location = "$fileName.$offset"
}
