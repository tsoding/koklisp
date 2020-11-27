import java.io.File

interface Sexpr

object Nil : Sexpr {
    override fun toString(): String {
        return "nil"
    }
}

data class Cons(val first: Sexpr, val second: Sexpr) : Sexpr {
    override fun toString(): String {
        val result = StringBuilder()

        result.append("(")
        var current: Sexpr = this
        var first = true;
        while (current is Cons) {
            if (first) {
                first = false
            } else {
                result.append(" ")
            }
            result.append(current.first.toString())
            if (current.second is Nil) {
                result.append(")")
            }
            current = current.second
        }

        if (current !is Nil) {
            result.append(" . ");
            result.append(current.toString())
            result.append(")");
        }

        return result.toString()
    }
}

data class Symbol(val name: String) : Sexpr {
    override fun toString(): String {
        return "$name"
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("ERROR: Not enough arguments provided");
        System.exit(1);
    }

    // println("Line count: ${File(args[0]).readText().lines().size}");

    println(Cons(Symbol("foo"), Cons(Symbol("bar"), Cons(Symbol("baz"), Nil))).toString())
}


