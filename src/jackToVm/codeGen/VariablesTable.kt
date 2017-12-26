package jackToVm.codeGen

import jackToVm.CodeLocation
import jackToVm.codeGen.CodeGeneration.SubroutineKind
import jackToVm.codeGen.CodeGeneration.SubroutineKind.*
import jackToVm.codeGen.CodeGeneration.VarKind
import jackToVm.codeGen.CodeGeneration.VarKind.*
import jackToVm.compilerElements.Node
import java.lang.UnsupportedOperationException
import kotlin.collections.HashMap


data class IndexedVar(val index : Int, val type: Node.TypeOrVoid)
data class TypedSubroutine(val returnedType: Node.TypeOrVoid, val parametersList: List<Node.TypeOrVoid>)

class VariablesTable{
    companion object {
        fun getVarOrFieldOrStatic(varName: Node.VarName, scope: Node.ClassName) =
                StackMember.getVariable(varName) ?:
                    HeapMember.getIndexedVar(FIELD, scope, varName) ?:
                    HeapMember.getIndexedVar(STATIC,scope, varName) ?:
                        throw VmCodeGenerationException(varName.codeLocation, "$varName declaration was not found in $scope context")
    }
}

class HeapMember{
    companion object {

        /**
         * Container Class -> Var Name -> <Defining Number, Var Type>
         */
        private val fieldsMap: MutableMap<String, MutableMap<String, IndexedVar>> = HashMap()

        /**
         * Container Class -> Var Name -> [IndexedVar]
         */
        private val staticsMap: MutableMap<String, MutableMap<String, IndexedVar>> = HashMap()

        /**
         * Container Class -> Method Name -> [TypedSubroutine]
         */
        private val methodsMap: MutableMap<String, MutableMap<String, TypedSubroutine>> = HashMap()

        /**
         * Container Class -> Method Name -> [TypedSubroutine]
         */
        private val functionsMap: MutableMap<String, MutableMap<String, TypedSubroutine>> = HashMap()

        private fun getMapOfVarKind(varKind: VarKind)= when (varKind) {
            FIELD -> fieldsMap
            STATIC -> staticsMap
            else -> throw UnsupportedOperationException("$varKind is not a heap member")
        }

        private fun getMapOfSubroutineKind(subroutineKind: SubroutineKind)= when (subroutineKind) {
            FUNCTION, CTOR -> functionsMap
            METHOD -> methodsMap
        }

        fun addVariable(varKind : VarKind, className: Node.ClassName, varName: Node.VarName, type: Node.TypeOrVoid){
            val kindMap = getMapOfVarKind(varKind)
            val classMap = kindMap.getOrPut(className.className, {HashMap()})
            if (fieldsMap[className.className]?.containsKey(varName.varName) == true || staticsMap[className.className]?.containsKey(varName.varName) == true) {
                throw VmCodeGenerationException(varName.codeLocation, "${varKind.name.toLowerCase()} '$varName': Name was used before in the context of $className")
            }
            val varsNumber = classMap.size
            classMap.put(varName.varName, IndexedVar(varsNumber,type))
        }


        fun addSubroutine(subroutineKind: SubroutineKind, className: Node.ClassName, subroutineName: Node.SubroutineName, returnedType: Node.TypeOrVoid, parametersList: List<Node.ParameterDec>){
            if (subroutineKind == CTOR){
                verifyConstructorConstrains(className, subroutineName, returnedType)
            }
            val kindMap = getMapOfSubroutineKind(subroutineKind)
            val classMap = kindMap.getOrPut(className.className, {HashMap()})
            if (functionsMap[className.className]?.containsKey(subroutineName.subroutineName) == true || methodsMap[className.className]?.containsKey(subroutineName.subroutineName) == true) {
                throw VmCodeGenerationException(subroutineName.codeLocation, "${subroutineKind.name.toLowerCase()} '$subroutineName' has already been declared before in the context of $className")
            }
            classMap.put(subroutineName.subroutineName, TypedSubroutine(returnedType, parametersList.map { it.type }))
        }

        private fun verifyConstructorConstrains(className: Node.ClassName, ctorName: Node.SubroutineName, returnedType : Node.TypeOrVoid){
            if (ctorName.subroutineName != "new"){
                throw VmCodeGenerationException(ctorName.codeLocation, "constructor must named 'new'")
            }
            if (!(returnedType is Node.TypeOrVoid.Type.ClassType && returnedType.className == className)){
                throw VmCodeGenerationException(ctorName.codeLocation, "constructor must return its class as returned type")
            }
        }

        fun getIndexedVar(varKind: VarKind, className: Node.ClassName, varName: Node.VarName) =
                getMapOfVarKind(varKind)[className.className]?.get(varName.varName)

        fun getTypedSubroutine(className: Node.ClassName, subroutineName: Node.SubroutineName) =
                methodsMap[className.className]?.get(subroutineName.subroutineName)
    }

}

class StackMember{
    companion object {
        private val varsMap : MutableMap<String, IndexedVar> = HashMap()

        fun addVariable(varName: Node.VarName, type: Node.TypeOrVoid){
            if (varsMap.containsKey(varName.varName)) {
                throw VmCodeGenerationException(varName.codeLocation, "local var '$varName' was declared before")
            }
            val varsNumber = varsMap.size
            varsMap.put(varName.varName, IndexedVar(varsNumber,type))
        }

        fun getVariable(varName: Node.VarName) = varsMap[varName.varName]

    }
}




