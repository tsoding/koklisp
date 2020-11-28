enum class TokenType {
    OpenParen,
    CloseParen,
    Dot,
    Quote,
    Symbol
}

data class Token(val type: TokenType, val text: String)

fun String.tokenize(): List<Token> {
    val s = this
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    return buildList {
        var input = s.trimStart()
        while (input.isNotEmpty()) {
            val token: Token = when (input[0]) {
                '(' -> Token(TokenType.OpenParen, "(")
                ')' -> Token(TokenType.CloseParen, ")")
                '.' -> Token(TokenType.Dot, ".")
                '\'' -> Token(TokenType.Quote, "'")
                else -> {
                    fun Char.isSpecial(): Boolean {
                        return "().'".indexOf(this) >= 0
                    }
                    fun Char.isSymbol(): Boolean {
                        return !this.isSpecial() && !this.isWhitespace()
                    }
                    Token(TokenType.Symbol, input.takeWhile(Char::isSymbol))
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
    return if (first == null) {
        Nil
    } else {
        Cons(first, sexprList(items.drop(1)))
    }
}

fun parseList(tokens: List<Token>): Pair<Sexpr, List<Token>> {
    var input = tokens.drop(1)  // TODO: This is hackish
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    val items = buildList() {
        // TODO: no support for dots in the list syntax (1 2 . 3)
        while (input.isNotEmpty() && input.first().type != TokenType.CloseParen) {
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
            TokenType.Quote -> {
                val result = parseSexpr(tokens.drop(1))
                Pair(Cons(Symbol("quote"), Cons(result.first, Nil)), result.second)
            }
            else -> {
                throw Exception("Unexpected token $first")
            }
        }
    } else {
        throw Exception("EOF")
    }
}

sealed class Sexpr

object Nil : Sexpr() {
    override fun toString() = "nil"
}

data class Cons(val first: Sexpr, val second: Sexpr) : Sexpr() {
    override fun toString(): String {
        val result = StringBuilder()

        result.append("(")
        var current: Sexpr = this
        var first = true
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
            result.append(" . ")
            result.append(current.toString())
            result.append(")")
        }

        return result.toString()
    }
}

data class Symbol(val name: String) : Sexpr() {
    override fun toString() = name
}

class Context {
    val names: MutableMap<String, Sexpr> = mutableMapOf()
    var quit = false

    fun evalMonoidFold(base: Int, mappend: (Int, Int) -> Int, sexpr: Sexpr): Sexpr {
        var result = base
        var args = sexpr
        while (args is Cons) {
            val x = eval(args.first)
            if (x is Symbol) {
                result = mappend(result, x.name.toInt())
            } else {
                throw Exception("Expected symbol but got $x")
            }
            args = args.second
        }
        return Symbol(result.toString())
    }

    fun eval(sexpr: Sexpr): Sexpr {
        return if (sexpr is Cons) {
            when (sexpr.first) {
                Symbol("+") -> evalMonoidFold(0, Int::plus, sexpr.second)
                Symbol("*") -> evalMonoidFold(1, Int::times, sexpr.second)
                Symbol("min") -> evalMonoidFold(Int.MAX_VALUE, Math::min, sexpr.second)
                Symbol("max") -> evalMonoidFold(Int.MIN_VALUE, Math::max, sexpr.second)
                Symbol("-") -> {
                    if (sexpr.second is Cons) {
                        val x = eval(sexpr.second.first)
                        if (x is Symbol) {
                            evalMonoidFold(x.name.toInt(), Int::minus, sexpr.second.second)
                        } else {
                            throw Exception("Expected symbol but got ${sexpr.second.first}")
                        }
                    } else {
                        throw Exception("Wrong arity of minus. Expected at least one argument")
                    }
                }
                Symbol("def") -> {
                    val args = sexpr.second
                    if (args !is Cons) {
                        throw Exception("Wrong arity of def. Expected at least one argument")
                    }

                    if (args.first !is Symbol) {
                        throw Exception("Expected symbol but got ${sexpr.first}")
                    }

                    val name = args.first.name
                    if (args.second !is Cons) {
                        throw Exception("Value is not provided")
                    }

                    val value = eval(args.second.first)
                    names[name] = value
                    Nil
                }
                Symbol("quote") -> {
                    if (sexpr.second is Cons) {
                        sexpr.second.first
                    } else {
                        throw Exception("Wrong arity of quote. Expected at least one argument")
                    }
                }
                Symbol("names") -> {
                    println(names)
                    Nil
                }
                Symbol("quit") -> {
                    quit = true
                    Nil
                }
                else -> throw Exception("Cannot call ${sexpr.first} as a function")
            }
        } else {
            sexpr
        }
    }
}

// TODO: global variables defined with `def` are not evaluated properly

fun main() {
    val context = Context()
    while (!context.quit) {
        print("∆ ")
        val line = readLine() ?: throw Exception("Line did a fucky-wucky")

        try {
            println(context.eval(parseSexpr(line.tokenize()).first))
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }
    }
}
