package app.trail.launcher.util

import kotlin.math.abs
import kotlin.math.floor

/** Tiny arithmetic evaluator for the search box: + - * / and brackets. */
object Calc {
    fun eval(input: String): Double? {
        val s = input.replace("×", "*").replace("x", "*").replace("X", "*").replace("÷", "/")
            .replace(",", ".").filter { !it.isWhitespace() }
        if (s.isEmpty()) return null
        val first = s[0]
        if (!(first.isDigit() || first == '(' || first == '-' || first == '.')) return null
        if (s.none { it == '+' || it == '-' || it == '*' || it == '/' }) return null
        return try {
            val p = Parser(s)
            val v = p.expr()
            if (p.i != s.length || v.isNaN() || v.isInfinite()) null else v
        } catch (_: Exception) {
            null
        }
    }

    fun format(v: Double): String =
        if (v == floor(v) && abs(v) < 1e15) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')

    private class Parser(val s: String) {
        var i = 0

        fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i]
                i++
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun term(): Double {
            var v = factor()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i]
                i++
                val r = factor()
                v = if (op == '*') v * r else v / r
            }
            return v
        }

        fun factor(): Double {
            if (i < s.length && s[i] == '-') {
                i++
                return -factor()
            }
            if (i < s.length && s[i] == '(') {
                i++
                val v = expr()
                if (i >= s.length || s[i] != ')') throw IllegalArgumentException()
                i++
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (start == i) throw IllegalArgumentException()
            return s.substring(start, i).toDouble()
        }
    }
}
