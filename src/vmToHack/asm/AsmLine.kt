package vmToHack.asm

interface Register
enum class AMReg : Register {A, M}
object D : Register {
    override fun toString(): String {
        return "D"
    }
}

enum class UnaryOpCode(private val sign : String){
    NONE(""), NEGATIVE("-"), NOT("!");
    override fun toString() = sign
}
enum class BinaryOpCode (private val sign : String){
    ADD("+"), SUB("-"),OR("|"), AND("&");
    override fun toString() = sign
}

enum class JOpCode{JGT , JEQ , JGE , JLT , JNE , JLE , JMP}

sealed class Comp(private val compExp : String){

    override fun toString() = compExp

    // D+A, D-A, D&A, D|A, D+M, D-M, D&M, D|M
    class DOpReg(opCode: BinaryOpCode, rReg: AMReg) : Comp("D$opCode$rReg")
    // A-D, M-D
    class RegSubD(lReg: AMReg) : Comp("$lReg-D")
    // D+1, A+1, D-1, A-1, M+1, M-1
    class RegOpOne(lreg: Register, isAddition: Boolean) : Comp("$lreg${if (isAddition) '+' else '-'}1")
    // A, D, M, !A, !D, !M, -A, -D, -M
    class UnaryOpReg(opCode: UnaryOpCode, reg: Register) : Comp("$opCode$reg"){
        constructor(reg: Register) : this(UnaryOpCode.NONE, reg)
    }
    // 1, -1
    class One(isPositive : Boolean) : Comp(if (isPositive) "1" else "-1")
    // 0
    object Zero : Comp("0")

}

abstract class AsmLine {
    abstract fun getAsString() : String
    class ACommand(private val value : String) : AsmLine(){
        constructor(value: Int) : this(value.toString())
        override fun getAsString() : String{
            return "@$value"
        }
    }

    class CCommand private constructor(private val dest: Array<Register>? = null,
                                       private val comp : Comp,
                                       private val jump : JOpCode? = null) : AsmLine(){
        constructor(dest: Register? = null, comp: Comp, jump: JOpCode? = null) :
                this(if (dest == null) null else arrayOf(dest), comp, jump)

        override fun getAsString() : String{
            return (if (dest != null && !dest.isEmpty()) {
                val amdString = dest.sortedBy {
                    when (it) {
                        AMReg.A -> 1
                        AMReg.M -> 2
                        D -> 3
                        else -> throw RuntimeException("Unexpected Register")
                    }
                }.joinToString("")
                "$amdString="
            } else "") +
                    comp.toString() +
                    (if (jump != null) ";$jump" else "")
        }
    }

    class CreateLabel (private val label : String) : AsmLine(){

        override fun getAsString(): String {
            return "($label)"
        }

    }

}
