package jackToVm.codeGen

import jackToVm.CodeLocation
import jackToVm.codeGen.CodeGeneration.SubroutineKind.*
import jackToVm.compilerElements.CharType
import jackToVm.compilerElements.Node
import jackToVm.compilerElements.Node.TypeOrVoid.Type.*
import jackToVm.compilerElements.Node.TypeOrVoid.Void
import java.io.File

fun String.vmClassName() = Node.ClassName(this, CodeLocation(File("$this.vm"),0))


val functions : MutableMap<String, MutableMap<String, TypedSubroutine>> = mutableMapOf(
        Pair(
                "Math", mutableMapOf(
                Pair("abs",TypedSubroutine(FUNCTION, "Math", IntType,listOf(IntType))),
                Pair("multiply",TypedSubroutine(FUNCTION, "Math", IntType, listOf(IntType, IntType))),
                Pair("divide",TypedSubroutine(FUNCTION, "Math", IntType, listOf(IntType, IntType))),
                Pair("min",TypedSubroutine(FUNCTION, "Math", IntType, listOf(IntType, IntType))),
                Pair("max",TypedSubroutine(FUNCTION, "Math", IntType, listOf(IntType, IntType)))
        )),
        Pair(
                "String", mutableMapOf(
                Pair("new",TypedSubroutine(CTOR, "String", ClassType("String".vmClassName()), listOf(IntType))),
                Pair("backSpace",TypedSubroutine(FUNCTION, "String", CharType, listOf())),
                Pair("doubleQuotes",TypedSubroutine(FUNCTION, "String", CharType, listOf())),
                Pair("newLine",TypedSubroutine(FUNCTION, "String", CharType, listOf()))
        )),
        Pair(
                "Keyboard", mutableMapOf(
                Pair("keyPressed",TypedSubroutine(FUNCTION, "Keyboard", CharType, listOf())),
                Pair("readLine",TypedSubroutine(FUNCTION, "Keyboard", ClassType("String".vmClassName()), listOf(ClassType("String".vmClassName())))),
                Pair("readInt",TypedSubroutine(FUNCTION, "Keyboard", IntType, listOf(ClassType("String".vmClassName())))),
                Pair("readChar",TypedSubroutine(FUNCTION, "Keyboard", CharType, listOf()))
        )),
        Pair(
                "Output", mutableMapOf(
                Pair("moveCursor",TypedSubroutine(FUNCTION, "Output", Void, listOf(IntType, IntType))),
                Pair("printString",TypedSubroutine(FUNCTION, "Output", Void, listOf(ClassType("String".vmClassName())))),
                Pair("println",TypedSubroutine(FUNCTION, "Output", Void, listOf())),
                Pair("printInt",TypedSubroutine(FUNCTION, "Output", Void, listOf(IntType))),
                Pair("printChar",TypedSubroutine(FUNCTION, "Output", Void, listOf(CharType))),
                Pair("backSpace",TypedSubroutine(FUNCTION, "Output", Void, listOf()))
        )),
        Pair(
                "Memory", mutableMapOf(
                Pair("poke", TypedSubroutine(FUNCTION, "Memory", IntType, listOf(IntType))),
                Pair("peek", TypedSubroutine(FUNCTION, "Memory", Void, listOf(IntType, IntType))),
                Pair("alloc", TypedSubroutine(FUNCTION, "Memory", ClassType("Array".vmClassName()), listOf(IntType))),
                Pair("deAlloc", TypedSubroutine(FUNCTION, "Memory", Void, listOf(ClassType("Array".vmClassName()))))
        )),
        Pair(
                "Screen", mutableMapOf(
                Pair("clearScreen", TypedSubroutine(FUNCTION, "Screen", Void, listOf())),
                Pair("setColor", TypedSubroutine(FUNCTION, "Screen", Void, listOf(BooleanType))),
                Pair("drawPixel", TypedSubroutine(FUNCTION, "Screen", Void, listOf(IntType,IntType))),
                Pair("drawLine", TypedSubroutine(FUNCTION, "Screen", Void, listOf(IntType,IntType,IntType,IntType))),
                Pair("drawRectangle", TypedSubroutine(FUNCTION, "Screen", Void, listOf(IntType,IntType,IntType,IntType))),
                Pair("drawCircle", TypedSubroutine(FUNCTION, "Screen", Void, listOf(IntType,IntType,IntType)))
        )),
        Pair(
                "Sys", mutableMapOf(
                Pair("halt", TypedSubroutine(FUNCTION, "Sys", Void, listOf())),
                Pair("error", TypedSubroutine(FUNCTION, "Sys", Void, listOf(IntType))),
                Pair("wait", TypedSubroutine(FUNCTION, "Sys", Void, listOf(IntType)))
        )),
        Pair(
                "Array", mutableMapOf(
                Pair("new",TypedSubroutine(CTOR, "Array", ClassType("Array".vmClassName()), listOf(IntType)))
        ))
)

val methods : MutableMap<String, MutableMap<String, TypedSubroutine>> = mutableMapOf(
        Pair(
                "String", mutableMapOf(
                Pair("dispose",TypedSubroutine(FUNCTION, "String", Void, listOf())),
                Pair("length",TypedSubroutine(METHOD, "String", IntType, listOf())),
                Pair("charAt",TypedSubroutine(METHOD, "String", CharType, listOf(IntType))),
                Pair("setCharAt",TypedSubroutine(METHOD, "String", Void, listOf(IntType, CharType))),
                Pair("appendChar",TypedSubroutine(METHOD, "String", ClassType("String".vmClassName()), listOf(CharType))),
                Pair("eraseLastChar",TypedSubroutine(METHOD, "String", Void, listOf())),
                Pair("intValue",TypedSubroutine(METHOD, "String", IntType, listOf())),
                Pair("setInt",TypedSubroutine(METHOD, "String", Void, listOf(IntType)))
        )),
        Pair(
                "Array", mutableMapOf(
                Pair("dispose",TypedSubroutine(FUNCTION, "Array", Void, listOf()))
        ))

)

val classes = mutableListOf("Math","Keyboard","Output","String","Array","Memory", "Screen","Sys")

