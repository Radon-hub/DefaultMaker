# DefaultMaker 🏗️

[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

### Kotlin's Smart Default Generator 🚀
**Generate default instances of any Kotlin class with ease!** Perfect for testing, prototyping, and reducing boilerplate code.

The smartest way to generate default instances of any Kotlin class. Perfect for:
- Unit testing 🧪
- Prototyping 🚀
- Reducing boilerplate code ✂️

## Features ✨

✔ **Zero-config defaults** for data classes  
✔ **Custom value generators** for any type  
✔ **Circular reference detection** (with escape hatch)  
✔ **Thread-safe** implementation  
✔ **Collection strategies** (List/Map/Set sizes)  
✔ **JSON integration** (Gson/Moshi ready)  

## Installation 📦

## Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.radon:defaultmaker:1.0.0")
    implementation(kotlin("reflect"))
}
```
## Maven
```xml
<dependency>
    <groupId>com.radon</groupId>
    <artifactId>defaultmaker</artifactId>
    <version>1.0.0</version>
</dependency>
```
## Basic Usage 🚀
```kotlin
data class User(val name: String, val age: Int)
// Create single instance
val user = createDefault<User>() 
// User(name="", age=0)

// Create list of defaults
val users = createDefaultList<User>(3)
// [User(name="", age=0), User(name="", age=0), User(name="", age=0)]
```
## Advanced Features 🔥
### 1. Custom Generators
```kotlin
// Global registration
DefaultMaker.registerGenerator(String::class) { "User_${(1000..9999).random()}" }

// Instance-specific
val specialUser = defaultMaker<User>()
    .withCustomGenerator(Int::class) { 42 }
    .create()
// User(name="User_1234", age=42)
```
### 2. Collection Strategies
```kotlin
// Set default sizes
DefaultMaker.configureCollectionStrategy(List::class) { 2 }
DefaultMaker.configureCollectionStrategy(Map::class) { 1 }

data class Order(
    val items: List<String>,
    val metadata: Map<String, Int>
)

val order = createDefault<Order>()
// Order(items=["", ""], metadata={"": 0})
```
### 3. Map & Set Generation
```kotlin
data class Config(
    val properties: Map<String, Int>,
    val featureFlags: Map<String, Boolean>
)

// Set default Map size to 2
DefaultMaker.configureCollectionStrategy(Map::class) { 2 }

val config = createDefault<Config>()
println(config)
// Config(
//   properties = {"": 0, "": 0}, 
//   featureFlags = {"": false, "": false}
// )

// Custom key/value generators
val advancedConfig = defaultMaker<Config>()
    .withCustomGenerator(String::class) { "key_${(1..100).random()}" }
    .withCustomGenerator(Int::class) { (1..10).random() }
    .create()
// Config(
//   properties = {"key_42": 3, "key_87": 7},
//   featureFlags = {"key_15": false, "key_92": false}
// )
```

```kotlin
data class ShoppingCart(
    val items: Set<String>,
    val couponCodes: Set<Int>
)

// Set default Set size to 3
DefaultMaker.configureCollectionStrategy(Set::class) { 3 }

val cart = createDefault<ShoppingCart>()
println(cart)
// ShoppingCart(
//   items = ["", "", ""],
//   couponCodes = [0, 0, 0]
// )

// With custom values
val premiumCart = defaultMaker<ShoppingCart>()
    .withCustomGenerator(Int::class) { (1000..9999).random() }
    .create()
// ShoppingCart(
//   items = ["", "", ""],
//   couponCodes = [4821, 7395, 1562]
// )
```
### 4. Nested Collections
```kotlin
data class ServerCluster(
    val nodes: Map<String, List<Int>> // Hostname -> Ports
)

// Custom configuration
val clusterMaker = defaultMaker<ServerCluster>()
    .withCollectionStrategy(List::class) { 2 } // 2 ports per host
    .withCollectionStrategy(Map::class) { 1 }  // 1 host
    .withCustomGenerator(String::class) { "host_${('A'..'Z').random()}" }
    .withCustomGenerator(Int::class) { (8000..9000).random() }

val cluster = clusterMaker.create()
println(cluster)
// ServerCluster(
//   nodes = {"host_X": [8321, 8456]}
// )
```
### 5. Circular References
```kotlin
data class Parent(val child: Child)
data class Child(val parent: Parent?)

// Option 1: Enable circular references (nullable fields)
DefaultMaker.allowCircularReferences = true
val parent = createDefault<Parent>() // Works! (child.parent = null)

// Option 2: Keep detection (throws exception)
DefaultMaker.allowCircularReferences = false
try {
    createDefault<Parent>() // throws CircularReferenceException
} catch (e: Exception) {
    println("Use nullable fields for circular refs!")
}
```
## Best Practices 📚
✔ **For circular references:** Use nullable types (**Node?**)

✔ **Register global generators** in your application startup

✔ **Prefer** ```kotlin createDefault() ```  for simple cases

✔ **Use builder pattern** when you need customization:

```kotlin
defaultMaker<MyClass>()
    .withCustomGenerator(...)
    .withCollectionStrategy(...)
    .create()
```
## JSON Integration 🗄️
### Gson Example
```kotlin
val gson = Gson()
DefaultMaker.registerGenerator(User::class) {
    gson.fromJson("""{"name":"GsonUser"}""", User::class.java)
}
```
### Moshi Example
```kotlin
val moshi = Moshi.Builder().build()
val adapter = moshi.adapter(User::class.java)
DefaultMaker.registerGenerator(User::class) {
    adapter.fromJson("""{"name":"MoshiUser"}""")!!
}
```

Made with ❤️ for Kotlin developers.
Star ⭐ the repo if you find it useful!

