import java.io.File

enum class TokenType {
    OpenParen,
    CloseParen,
    Dot,
    Symbol
}

data class Token(val type: TokenType, val text: String)

fun String.tokenize(): List<Token> {
    val s = this
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    return buildList() {
        var input = s.trimStart()
        while (!input.isEmpty()) {
            val token: Token = when (input[0]) {
                '(' -> Token(TokenType.OpenParen, "(")
                ')' -> Token(TokenType.CloseParen, ")")
                '.' -> Token(TokenType.Dot, ".")
                else -> {
                    fun Char.isSpecial(): Boolean {
                        return "().".indexOf(this) >= 0
                    }
                    fun Char.isSymbol(): Boolean {
                        return !this.isSpecial() && !this.isWhitespace()
                    }
                    Token(TokenType.Symbol, input.takeWhile({ x -> x.isSymbol() }))
                }
            }
            add(token)
            input = input.drop(token.text.length).trimStart()
        }
    }
}

// TODO: sexprList is probably slow and requires something like TCO (I don't know if kotlin supports that)
fun sexprList(items: List<Sexpr>): Sexpr {
    val first = items.firstOrNull()
    if (first == null) {
        return Nil
    } else {
        return Cons(first, sexprList(items.drop(1)))
    }
}

fun parseList(tokens: List<Token>): Pair<Sexpr, List<Token>> {
    var input = tokens.drop(1)  // TODO: This is hackish
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    val items = buildList() {
        // TODO: no support for dots in the list syntax (1 2 . 3)
        while (!input.isEmpty() && input.first().type != TokenType.CloseParen) {
            val result = parseSexpr(input)
            input = result.second
            add(result.first)
        }
        input = input.drop(1)
    }
    return Pair(sexprList(items), input)
}

fun parseSexpr(tokens: List<Token>): Pair<Sexpr, List<Token>> {
    val first = tokens.firstOrNull()
    if (first != null) {
        return when (first.type) {
            TokenType.OpenParen -> parseList(tokens)
            TokenType.Symbol -> Pair(Symbol(first.text), tokens.drop(1))
            else -> {
                throw Exception("Unexpected token ${first}")
            }
        }
    } else {
        throw Exception("EOF")
    }
}

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

fun eval(sexpr: Sexpr): Sexpr {
    return if (sexpr is Cons) {
        when (sexpr.first) {
            Symbol("+") -> {
                var result = 0
                var args = sexpr.second
                while (args is Cons) {
                    val x = eval(args.first)
                    if (x is Symbol) {
                        result = result + x.name.toInt()
                    } else {
                        throw Exception("Expected symbol but got $x")
                    }
                    args = args.second
                }
                Symbol(result.toString())
            }

            Symbol("*") -> {
                var result = 1
                var args = sexpr.second
                while (args is Cons) {
                    val x = eval(args.first)
                    if (x is Symbol) {
                        result = result * x.name.toInt()
                    } else {
                        throw Exception("Expected symbol but got $x")
                    }
                    args = args.second
                }
                Symbol(result.toString())
            }

            else -> {
                throw Exception("Cannot call ${sexpr.first} as a function")
            }
        }
    } else {
        sexpr
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("ERROR: Not enough arguments provided");
        System.exit(1);
    }

    println("Result: ${eval(parseSexpr(File(args[0]).readText().tokenize()).first)}");
}
