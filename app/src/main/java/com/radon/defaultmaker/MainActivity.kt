package com.radon.defaultmaker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.radon.defaultmaker.ui.theme.DefaultMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure everything
        DefaultMaker.registerGenerator(String::class) { "John Doe" }
        DefaultMaker.configureCollectionStrategy(List::class) { 3 }
        DefaultMaker.configureCollectionStrategy(Map::class) { 2 }

        enableEdgeToEdge()
        setContent {
            DefaultMakerTheme {

                Log.e("defaultUser", defaultUser.toString())
                Log.e("defaultUser", users.toString())
                Log.e("defaultUser", customUser.toString())
                Log.e("defaultUser", order.toString())
                Log.e("defaultUser", order2.toString())
                Log.e("defaultUser", parent.toString())
                Log.e("defaultUser", config.toString())


            }
        }
    }
}

data class User(val name: String, val age: Int)

val defaultUser = defaultMaker<User>().create()
val users = createDefaultList<User>(5)


// Instance-specific override
val userMaker = defaultMaker<User>()
    .withCustomGenerator(Int::class) { (18..60).random() }

val customUser = userMaker.create()


data class Order(val items: List<String>)
val order = createDefault<Order>()

data class Parent(val child: Child)
data class Child(val parent: Parent?)
val parent = createDefault<Parent>()

data class Config(val settings: Map<String, Boolean>)

// Configure default Map size

val config = createDefault<Config>()


data class Order2(
    val id: String,
    val items: List<OrderItem>,
    val customer: Customer
)

data class OrderItem(val sku: String, val quantity: Int)
data class Customer(val name: String, val tier: CustomerTier)

enum class CustomerTier { BASIC, PREMIUM }


val order2 = createDefault<Order2>()


