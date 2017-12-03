package vmToHack.vm

import vmToHack.asm.AsmLine

class TestWorkAround {

    fun getAssembly() : Array<AsmLine>{
        return increaseSP()
    }


    private fun increaseSP(): Array<AsmLine> {
        return increaseSpBy1
    }
}