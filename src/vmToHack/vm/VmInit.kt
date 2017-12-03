package vmToHack.vm

import vmToHack.asm.*

private const val STACK_START = 256 - 1 //Start stack at the previous location of start position

class VmInit {

    fun getAssembly() : Array<AsmLine>{
        return initSP()
    }


    private fun initSP(): Array<AsmLine> {
        return arrayOf(setATo(STACK_START), copyAToD, setAToSP, copyDToM)
    }
}