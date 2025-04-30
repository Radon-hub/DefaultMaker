package com.radon.defaultmaker

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import java.util.concurrent.atomic.AtomicReference

class DefaultMaker<T : Any>(
    private val kClass: KClass<T>,
) {

    private var customGenerators: Map<KClass<*>, () -> Any?> = emptyMap()
    private var collectionStrategies: Map<KClass<*>, () -> Int> = emptyMap()
    private var depthStack: ThreadLocal<MutableSet<KClass<*>>> = ThreadLocal.withInitial { mutableSetOf() }

    private constructor(
        kClass: KClass<T>,
        customGenerators: Map<KClass<*>, () -> Any?> = emptyMap(),
        collectionStrategies: Map<KClass<*>, () -> Int> = emptyMap(),
        depthStack: ThreadLocal<MutableSet<KClass<*>>> = ThreadLocal.withInitial { mutableSetOf() }
    ) : this(kClass) {
        this.customGenerators = customGenerators
        this.collectionStrategies = collectionStrategies
        this.depthStack = depthStack
    }

    // Default values for common types
    private val defaultPrimitives: Map<KClass<*>, Any?> = mapOf(
        String::class to "",
        Int::class to 0,
        Boolean::class to false,
        Long::class to 0L,
        Float::class to 0f,
        Double::class to 0.0
    )

    companion object {
        // Global configuration storage (thread-safe)
        private val globalCustomGenerators = AtomicReference(mutableMapOf<KClass<*>, () -> Any?>())
        private val globalCollectionStrategies = AtomicReference(
            mutableMapOf(
                List::class to { 1 },
                Map::class to { 1 },
                Set::class to { 1 }
            )
        )

        // Circular reference configuration
        var allowCircularReferences: Boolean = false

        // Register global custom generators (thread-safe)
        fun <T : Any> registerGenerator(type: KClass<T>, generator: () -> T) {
            globalCustomGenerators.getAndUpdate { it.apply { put(type, generator) } }
        }

        // Configure collection strategies (thread-safe)
        fun configureCollectionStrategy(
            collectionType: KClass<*>,
            strategy: () -> Int
        ) {
            globalCollectionStrategies.updateAndGet { it.apply { put(collectionType, strategy) } }
        }

        // Main entry point with reified type
        inline fun <reified T : Any> create(): DefaultMaker<T> {
            val kClass = T::class
            require(kClass.isData) { "${kClass.simpleName} must be a data class" }
            return DefaultMaker(kClass)
        }
    }

    // Thread-safe builder pattern
    fun withCustomGenerator(type: KClass<*>, generator: () -> Any?): DefaultMaker<T> {
        return DefaultMaker(
            kClass,
            customGenerators + (type to generator),
            collectionStrategies,
            ThreadLocal.withInitial { depthStack.get().toMutableSet() }
        )
    }

    fun withCollectionStrategy(collectionType: KClass<*>, strategy: () -> Int): DefaultMaker<T> {
        return DefaultMaker(
            kClass,
            customGenerators,
            collectionStrategies + (collectionType to strategy),
            ThreadLocal.withInitial { depthStack.get().toMutableSet() }
        )
    }

    // Core functions
    fun create(): T {
        return createWithDepthTracking()
    }

    fun createList(size: Int): List<T> = List(size) { create() }

    private fun createWithDepthTracking(): T {
        val currentDepth = depthStack.get()
        try {
            checkCircularReference()
            currentDepth.add(kClass)  // Work with local reference
            return when {
                kClass.isData -> createDataClassInstance()
                else -> throw IllegalArgumentException("${kClass.simpleName} is not a data class")
            }
        } finally {
            currentDepth.remove(kClass)
        }
    }

//    private fun createWithDepthTracking(): T {
//        return try {
//            checkCircularReference()
//            depthStack.get().add(kClass)
//            when {
//                kClass.isData -> createDataClassInstance()
//                else -> throw IllegalArgumentException("${kClass.simpleName} is not a data class")
//            }
//        } finally {
//            depthStack.get().remove(kClass)
//        }
//    }

    private fun checkCircularReference() {
        if (!allowCircularReferences && depthStack.get().contains(kClass)) {
            throw CircularReferenceException(
                "Circular reference detected for ${kClass.simpleName}. " +
                        "Consider making one side nullable or enable allowCircularReferences"
            )
        }
    }

    private fun createDataClassInstance(): T {
        val constructor = requireNotNull(kClass.primaryConstructor) {
            "Data class ${kClass.simpleName} does not have a primary constructor"
        }
        val args = constructor.parameters.associateWith { param ->
            generateDefaultValue(param.type)
        }
        return constructor.callBy(args)
    }

    private fun generateDefaultValue(type: KType): Any? {
        val classifier = type.jvmErasure

        // Handle nullables first
        if (type.isMarkedNullable) return null

        // Check custom generators (instance-specific first)
        customGenerators[classifier]?.invoke()?.let { return it }
        globalCustomGenerators.get()[classifier]?.invoke()?.let { return it }

        // Check primitive defaults
        defaultPrimitives[classifier]?.let { return it }

        // Handle collections and complex types
        return when {
            classifier.isSubclassOf(List::class) -> handleList(type)
            classifier.isSubclassOf(Map::class) -> handleMap(type)
            classifier.isSubclassOf(Set::class) -> handleSet(type)
            classifier.isSubclassOf(Enum::class) -> handleEnum(classifier)
            classifier.isData -> handleDataClass(classifier)
            else -> throw IllegalArgumentException("Unsupported type: ${classifier.simpleName}")
        }
    }

    private fun handleList(type: KType): List<*> {
        val elementType = type.arguments.first().type!!
        val size = collectionStrategies[List::class]
            ?.invoke()
            ?: globalCollectionStrategies.get()[List::class]?.invoke()
            ?: 1

        return List(size) { generateDefaultValue(elementType) }
    }

    private fun handleMap(type: KType): Map<*, *> {
        val keyType = type.arguments[0].type!!
        val valueType = type.arguments[1].type!!
        val size = collectionStrategies[Map::class]
            ?.invoke()
            ?: globalCollectionStrategies.get()[Map::class]?.invoke()
            ?: 1

        return (1..size).associate {
            generateDefaultValue(keyType) to generateDefaultValue(valueType)
        }
    }

    private fun handleSet(type: KType): Set<*> {
        val elementType = type.arguments.first().type!!
        val size = collectionStrategies[Set::class]
            ?.invoke()
            ?: globalCollectionStrategies.get()[Set::class]?.invoke()
            ?: 1

        return (1..size).map { generateDefaultValue(elementType) }.toSet()
    }

    private fun handleEnum(classifier: KClass<*>): Any? {
        val constants = classifier.java.enumConstants
        return when {
            constants != null -> constants.first()
            allowCircularReferences -> null
            else -> throw IllegalArgumentException("Empty enum: ${classifier.simpleName}")
        }
    }

    private fun handleDataClass(classifier: KClass<*>): Any {
        return DefaultMaker(
            classifier as KClass<Any>,
            customGenerators,
            collectionStrategies,
            ThreadLocal.withInitial { depthStack.get().toMutableSet() }
        ).create()
    }
}

class CircularReferenceException(message: String) : RuntimeException(message)

// Top-level helper functions
inline fun <reified T : Any> defaultMaker(): DefaultMaker<T> = DefaultMaker.create()
inline fun <reified T : Any> createDefault(): T = defaultMaker<T>().create()
inline fun <reified T : Any> createDefaultList(size: Int): List<T> = defaultMaker<T>().createList(size)