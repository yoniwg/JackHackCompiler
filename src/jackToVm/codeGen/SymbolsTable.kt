package jackToVm.codeGen

import jackToVm.codeGen.CodeGeneration.SubroutineKind
import jackToVm.codeGen.CodeGeneration.SubroutineKind.*
import jackToVm.codeGen.CodeGeneration.VarKind
import jackToVm.codeGen.CodeGeneration.VarKind.*
import jackToVm.compilerElements.Node
import java.lang.UnsupportedOperationException


data class IndexedVar(val index : Int, val varKind: VarKind, val type: Node.TypeOrVoid)

data class TypedSubroutine(val subroutineKind: SubroutineKind, val ownerClassName: String, val returnedType: Node.TypeOrVoid, val parametersList: List<Node.TypeOrVoid>)

object SymbolsTable {

    private fun Node.VarOrClassName.toVarName() = Node.VarName(varOrClassName, codeLocation)

    private fun Node.VarOrClassName.toClassName() = Node.ClassName(varOrClassName, codeLocation)

    fun getVarOrFieldOrStatic(varOrClassNameKnownAsVar: Node.VarOrClassName, scope: Node.ClassName) =
            getVarOrFieldOrStatic(varOrClassNameKnownAsVar.toVarName(), scope)
    fun getVarOrFieldOrStatic(varName: Node.VarName, scope: Node.ClassName) =
            StackMember.getVariable(varName) ?:
                    HeapMember.getIndexedVar(FIELD, scope, varName) ?:
                    HeapMember.getIndexedVar(STATIC,scope, varName) ?:
                    throw VmCodeGenerationException(varName.codeLocation, "'${varName.varName}' declaration was not found in '${scope.className}' class")

    fun getMethodOrFunction(subroutineName : Node.SubroutineName, source : Node.SubroutineSource?, scope: Node.ClassName) : TypedSubroutine{
        return if (source != null && Classes.isClass(source.varOrClassName)) {
                HeapMember.getTypedSubroutine(FUNCTION, source.varOrClassName.toClassName(), subroutineName)
            } else {
                val varType = when {
                    source != null -> getVarOrFieldOrStatic(source.varOrClassName.toVarName(), scope).type
                    else -> StackMember.thisArg?.type
                }
                if (varType != null) {
                    HeapMember.getTypedSubroutine(METHOD,
                            if (varType is Node.TypeOrVoid.Type.ClassType) varType.className
                            else Node.ClassName(varType.javaClass.simpleName, subroutineName.codeLocation),
                            subroutineName)
                } else {
                    HeapMember.getTypedSubroutine(FUNCTION, scope, subroutineName)
                }
            }
                    ?:
                    throw VmCodeGenerationException(subroutineName.codeLocation,
                            "'${source?.varOrClassName?.varOrClassName ?: scope.className}' doesn't contain '${subroutineName.subroutineName}' method")
}
fun getThis() = StackMember.thisArg
}


object Classes {

    private val classesList : MutableList<String> = classes

    fun addClass(className: Node.ClassName) {
        classesList.add(className.className)
    }

    fun isClass(varOrClassName: Node.VarOrClassName?) = classesList.contains(varOrClassName?.varOrClassName)
}

object HeapMember {

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
    private val methodsMap: MutableMap<String, MutableMap<String, TypedSubroutine>> = methods

    /**
     * Container Class -> Method Name -> [TypedSubroutine]
     */
    private val functionsMap: MutableMap<String, MutableMap<String, TypedSubroutine>> = functions

    private fun getMapOfVarKind(varKind: VarKind)=
            when (varKind) {
                FIELD -> fieldsMap
                STATIC -> staticsMap
                else -> throw UnsupportedOperationException("$varKind is not a heap member")
            }

    private fun getMapOfSubroutineKind(subroutineKind: SubroutineKind) =
            when (subroutineKind) {
                FUNCTION, CTOR -> functionsMap
                METHOD -> methodsMap
            }

    fun addVariable(varKind : VarKind, className: Node.ClassName, varName: Node.VarName, type: Node.TypeOrVoid) : IndexedVar{
        val kindMap = getMapOfVarKind(varKind)
        val classMap = kindMap.getOrPut(className.className, {HashMap()})
        if (fieldsMap[className.className]?.containsKey(varName.varName) == true ||
                staticsMap[className.className]?.containsKey(varName.varName) == true) {
            throw VmCodeGenerationException(varName.codeLocation, "${varKind.name.toLowerCase()} '$varName': Name was used before in the context of $className")
        }
        val varsNumber = classMap.size
        val indexedVar = IndexedVar(varsNumber, varKind, type)
        classMap.put(varName.varName, indexedVar)
        return indexedVar
    }

    fun addSubroutine(subroutineKind: SubroutineKind, className: Node.ClassName, subroutineName: Node.SubroutineName, returnedType: Node.TypeOrVoid, parametersList: List<Node.ParameterDec>){
        if (subroutineKind == CTOR){
            verifyConstructorConstrains(className, subroutineName, returnedType)
        }
        val kindMap = getMapOfSubroutineKind(subroutineKind)
        val classMap = kindMap.getOrPut(className.className, {HashMap()})
        if (functionsMap[className.className]?.containsKey(subroutineName.subroutineName) == true ||
                methodsMap[className.className]?.containsKey(subroutineName.subroutineName) == true) {
            throw VmCodeGenerationException(subroutineName.codeLocation,
                    "${subroutineKind.name.toLowerCase()} '$subroutineName' has already been declared before in the context of $className")
        }
        classMap.put(subroutineName.subroutineName, TypedSubroutine(subroutineKind ,className.className, returnedType, parametersList.map { it.type }))
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

    fun getTypedSubroutine(subroutineKind: SubroutineKind, className: Node.ClassName, subroutineName: Node.SubroutineName) =
            getMapOfSubroutineKind(subroutineKind)[className.className]?.get(subroutineName.subroutineName)
}


object StackMember {

    private val varsMap : MutableMap<String, IndexedVar> = HashMap()

    private val argsMap : MutableMap<String, IndexedVar> = HashMap()

    var thisArg : IndexedVar? = null
        private set

    private fun getMapOfVarKind(varKind: VarKind) =
            when (varKind) {
                VAR -> varsMap
                ARG -> argsMap
                else -> throw UnsupportedOperationException("$varKind is not a stack member")
            }

    fun initStack() {
        varsMap.clear()
        argsMap.clear()
        thisArg = null
    }


    fun addVariable(varKind: VarKind, varName: Node.VarName, type: Node.TypeOrVoid): IndexedVar {
        if (varKind == THIS){
            thisArg = IndexedVar(0, THIS, type)
            return thisArg!!
        }
        val kindMap = getMapOfVarKind(varKind)
        if (kindMap.containsKey(varName.varName)) {
            throw VmCodeGenerationException(varName.codeLocation, "$varKind '$varName' was declared before")
        }
        val varsNumber = kindMap.size
        val indexedVar = IndexedVar(varsNumber, varKind, type)
        kindMap.put(varName.varName, indexedVar)
        return indexedVar
    }

    fun getVariable(varName: Node.VarName) = argsMap[varName.varName] ?: varsMap[varName.varName]

}




