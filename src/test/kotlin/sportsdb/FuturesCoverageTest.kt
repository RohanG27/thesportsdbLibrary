package sportsdb

import kotlin.coroutines.Continuation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fails if V1Api/V2Api gain a method that SportsDbFutures lacks: re-run tools/gen-futures.py. */
class FuturesCoverageTest {
    private fun suspendMethods(c: Class<*>) = c.declaredMethods
        .filter { !it.isSynthetic && '$' !in it.name && java.lang.reflect.Modifier.isPublic(it.modifiers) }
        .filter { it.parameterTypes.lastOrNull() == Continuation::class.java }
        .map { it.name + it.parameterTypes.dropLast(1).joinToString(",", "(", ")") { p -> p.simpleName } }
        .toSet()

    private fun futureMethods(c: Class<*>) = c.declaredMethods
        .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) && it.returnType == java.util.concurrent.CompletableFuture::class.java }
        .map { it.name + it.parameterTypes.joinToString(",", "(", ")") { p -> p.simpleName } }
        .toSet()

    @Test fun everyMethodIsWrapped() {
        var checked = 0
        for ((api, prefix) in listOf(V1Api::class.java to "V1", V2Api::class.java to "V2")) {
            for (group in api.declaredClasses) {
                val facade = Class.forName("sportsdb.SportsDbFutures\$$prefix${group.simpleName}")
                val missing = suspendMethods(group) - futureMethods(facade)
                assertEquals(emptySet(), missing, "SportsDbFutures.$prefix${group.simpleName} is missing methods")
                checked += suspendMethods(group).size
            }
        }
        assertTrue(checked > 80, "only found $checked methods")
    }
}
