package jackToVm

import jackToVm.codeGen.ApiDefining
import jackToVm.codeGen.CodeGeneration
import jackToVm.compilerElements.NodePrint
import jackToVm.lexical.LexicalParser
import jackToVm.syntactic.ProgramStructureVar
import jackToVm.syntactic.SyntacticParser
import jackToVm.syntactic.Variable
import vmToHack.vm.VmCommand
import vmToHack.vm.VmSegment
import java.io.File

val VmSegment.name get() = when(this){

    is VmSegment.Constant -> "constant"
    is VmSegment.DynamicSeg -> when (this){
        is VmSegment.DynamicSeg.Local -> "local"
        is VmSegment.DynamicSeg.Arg -> "argument"
        is VmSegment.DynamicSeg.This -> "this"
        is VmSegment.DynamicSeg.That -> "that"
    }
    is VmSegment.StaticSeg -> when (this){
        is VmSegment.StaticSeg.Pointer -> "pointer"
        is VmSegment.StaticSeg.Temp -> "temp"
        is VmSegment.StaticSeg.Static -> "static"
    }
}

fun VmCommand.generateStringCode() = when (this){

    is VmCommand.Push -> "push ${this.vmSegment.name} ${vmSegment.offset}"
    is VmCommand.Pop -> "pop ${this.vmSegment.name} ${vmSegment.offset}"
    VmCommand.BinaryCommand.Add -> "add"
    VmCommand.BinaryCommand.Sub -> "sub"
    VmCommand.BinaryCommand.And -> "and"
    VmCommand.BinaryCommand.Or -> "or"
    VmCommand.UnaryCommand.Neg -> "neg"
    VmCommand.UnaryCommand.Not -> "not"
    VmCommand.ComparisionCommand.Eq -> "eq"
    VmCommand.ComparisionCommand.Gt -> "gt"
    VmCommand.ComparisionCommand.Lt -> "lt"
    is VmCommand.Label -> "label $labelName"
    is VmCommand.Goto -> "goto $labelName"
    is VmCommand.IfGoto -> "if-goto $labelName"
    is VmCommand.Function -> "function $funcName $nVars"
    is VmCommand.Call -> "call $funcName $nArgs"
    VmCommand.Return -> "return"
    is VmCommand.Comment -> "// $comment"
}

class JackToVmCompiler(private val jackFiles: List<File>) {

    fun compile(): Map<String, List<VmCommand>> {
            val nodes = jackFiles.map { file ->
                Pair(file.nameWithoutExtension,
                        LexicalParser(file).parseTokens().iterator()
                                .let { SyntacticParser(it.iterator()) })
            }.toMap()

            nodes.forEach{ (_, sp)->
                ApiDefining(sp.classNode).defineApi()
            }
            with(NodePrint()){nodes.forEach{ (_, sp)->sp.classNode.printTo(System.out)}}

            return nodes.mapValues { CodeGeneration(it.key, it.value.classNode).generateCode() }
    }

}