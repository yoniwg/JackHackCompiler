package jackToVm.compilerElements

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class NodePrint{

    fun Node.printTo(appendable: Appendable) {
        appendable.apply { appendNode(this@printTo) }
    }

    private fun Appendable.appendNode(
            node: Node,
            prefix: String = "",
            isRoot: Boolean = true,
            lastInPeers: Boolean = true
    ) {

        val memberProperties = node::class.memberProperties
        val props = mutableListOf<Pair<String, String>>()
        val children = mutableListOf<Node>()
        memberProperties.forEach { p ->
            @Suppress("UNCHECKED_CAST")
            p as KProperty1<Node, Any?>
            val name = p.name
            val value = p.get(node)
            if (p.name != "codeLocation") {
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    is Node -> children += value
                    is List<*> -> children.addAll(value as List<Node>)
                    else -> props += name to value.toString()
                }
            }
        }


        append(prefix)
        if (!isRoot) {
            append(if (lastInPeers) "└── " else "├── ")
        } else {
            append(" ── ")
        }
        appendTitle(node.javaClass.simpleName, props)
        append('\n')

        val indentation = if (isRoot) "    " else "    "
        children.forEachIndexed { index, child ->
            if (index != children.lastIndex) {
                appendNode(child, prefix + if (lastInPeers) indentation else "│   ", false, false)
            } else {
                appendNode(child, prefix + if (lastInPeers) indentation else "│   ", false, true)
            }
        }
    }

    private fun Appendable.appendTitle(title: String, props: Iterable<Pair<String, String>>) {
        append(title)
        append("(")
        props.joinTo(this, separator = "; ") { (name, value) ->
            append(name)
            append(" = ")
            append(value)
            ""
        }
        append(")")
    }
}