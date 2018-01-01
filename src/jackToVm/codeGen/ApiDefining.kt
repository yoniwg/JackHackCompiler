package jackToVm.codeGen

import jackToVm.codeGen.CodeGeneration.VarKind.FIELD
import jackToVm.codeGen.CodeGeneration.VarKind.STATIC
import jackToVm.compilerElements.Node


class ApiDefining(val classNode: Node.Class){
    lateinit var className : Node.ClassName

    fun defineApi(){
        classNode.addToSymbolsTableHeap()
    }

    private fun Node.Class.addToSymbolsTableHeap(){
        this@ApiDefining.className = this.className
        Classes.addClass(className)
        classVarDecs?.addToSymbolsTableHeap()
        subroutineDecs?.addToSymbolsTableHeap()
    }

    private fun Node.ClassVarDecs.addToSymbolsTableHeap(){
        classVarDec.addToSymbolsTableHeap()
        classVarDecs?.addToSymbolsTableHeap()
    }

    private fun Node.ClassVarDec.addToSymbolsTableHeap(){
        when (this){
            is Node.ClassVarDec.StaticClassVarDec -> varNames.addToSymbolsTableHeap(STATIC,type)
            is Node.ClassVarDec.FieldClassVarDec -> varNames.addToSymbolsTableHeap(FIELD,type)
        }
    }

    private fun Node.VarNames.addToSymbolsTableHeap(varKind: CodeGeneration.VarKind, type: Node.TypeOrVoid){
        varName.addToSymbolsTableHeap(varKind, type)
        varNames?.addToSymbolsTableHeap(varKind,type)
    }

    private fun Node.VarName.addToSymbolsTableHeap(varKind: CodeGeneration.VarKind, type: Node.TypeOrVoid){
        when (varKind) {
            FIELD, STATIC -> {
                HeapMember.addVariable(varKind, className, this, type)
            }
            else -> {
                throw RuntimeException("$varKind Should be added to Symbols Table at Code Generation phase")
            }
        }
    }

    private fun Node.SubroutineDecs.addToSymbolsTableHeap(){
        subroutineDec.addToSymbolsTableHeap()
        subroutineDecs?.addToSymbolsTableHeap()
    }

    private fun Node.SubroutineDec.addToSymbolsTableHeap(){
        val parametersList =  parametersList?.aggregateDecs().orEmpty()
        val subroutineKind = when (this) {
            is Node.SubroutineDec.ConstructorDec -> CodeGeneration.SubroutineKind.CTOR
            is Node.SubroutineDec.FunctionDec -> CodeGeneration.SubroutineKind.FUNCTION
            is Node.SubroutineDec.MethodDec -> CodeGeneration.SubroutineKind.METHOD
        }
        HeapMember.addSubroutine(subroutineKind, className, subroutineName, retType, parametersList)
    }

    private fun Node.ParametersList.aggregateDecs() : List<Node.ParameterDec>{
        return listOf(parameterDec) + parametersList?.aggregateDecs().orEmpty()
    }

}