package jackToVm.syntactic

import jackToVm.compilerElements.Node

sealed class ExpressionVar : Variable(){
    object Expression : ExpressionVar() {
        override fun generateNode(): Node.Expression {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    object SubroutineCall : Variable(){
        override fun generateNode(): Node.SubroutineCall {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}
