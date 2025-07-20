# Shoply - E-Commerce Android App

**Shoply** is a fully-featured e-commerce Android application built with modern tools like **Jetpack Compose**, **Room**, **Firebase Authentication**, and **Firestore**. It provides a rich shopping experience including onboarding, authentication (Email + Google), product browsing with pagination, cart and checkout system, order history, and user profile settings.

---

## 🧱 Architecture Used

Shoply follows the **MVVM (Model-View-ViewModel)** architecture with a clean separation of concerns. It uses a repository pattern to manage data from Room and Firebase.

### 🔹 Model Layer
- `Product`, `User`, `CartItem`, and `Order` data classes
- Room with DAO and TypeConverters (e.g., `CartItemListConverter`)
- Firestore for cloud data storage
- Firebase Authentication for login and session management

### 🔹 View Layer
- UI built using **Jetpack Compose**
- Screens:
    - Onboarding
    - Authentication 
    - Home
    - Product Details
    - Cart → Checkout
    - Order History 
    - Profile
    - Settings (using DataStore)

### 🔹 ViewModel Layer
- State handled using `mutableStateOf`, `derivedStateOf`, `viewModelScope`
- ViewModels interact with Repositories for business logic

### 🔹 Data Sync Flow
- Cart and Order data are stored locally using Room
- Firestore is used to sync order history in real-time
- On user sign-in or device change, app checks for missing order data in Firestore and syncs it locally

---

## 📁 Project Folder Structure

Shoply/
├── database/ # Room database and DAO setup
├── models/ # Product, User, CartItem, Order
├── repositories/ # CartRepository, CheckoutRepository, etc.
├── screens/ # All UI screens (Compose)
│ ├── OnboardingScreen.kt
│ ├── AuthenticationScreen.kt
│ ├── HomeScreen.kt
│ ├── ProductDetailsScreen.kt
│ ├── CartScreen.kt → CheckoutScreen.kt
│ ├── SettingsScreen.kt → ProfileScreen.kt, OrderHistoryScreen.kt
├── sharedPref/ # SettingsDataStore.kt for theme & onboarding flags
├── ui/ # Theming and reusable components
├── viewmodels/ # ViewModels + Factory
│ ├── AuthViewModel.kt
│ ├── HomeViewModel.kt
│ ├── CartCheckoutViewModel.kt
│ ├── OrderHistoryViewModel.kt
│ └── ProfileViewModel.kt
└── MainActivity.kt # Entry point

---

## ▶️ How to Run the App

### ✅ Prerequisites

- **Android Studio**: Flamingo (2023.1.1) or later
- **JDK**: 17 or above
- **Min SDK**: 29
- **Target SDK**: 36
- **Firebase Project** with Firestore and Authentication

### 📥 Steps

1. **Clone the repository**

```bash
git clone https://github.com/amaankhan420/Shoply.git
cd shoply
```

Open in Android Studio

Open the project via File > Open in Android Studio.

Set up Firebase

Create a Firebase project

Register the Android app with package name: com.example.shoply

Download google-services.json and place it in the app/ directory

Sync the Project

Ensure Gradle sync completes successfully

Dependencies are managed via Version Catalog (libs.versions.toml)

Build and Run

Connect an emulator or device

Run the project. The onboarding screen appears on first launch, followed by Auth/Home based on login status.

🔥 Firebase Setup Instructions
1️⃣ Firebase Console
Go to Firebase Console

Create a new project

2️⃣ Add Your Android App
Package name: com.example.shoply

Download google-services.json and put it in app/

3️⃣ Enable Firestore
Create Firestore database in test mode (for development)

Create products and orders collections

Sample products document:
```
{
  "id": "product_001",
  "name": "Example Shirt",
  "price": 1200,
  "image": "https://example.com/shirt.jpg",
  "brand": "Shoply"
}
```

4️⃣ Enable Authentication
Enable Email/Password

Enable Google Sign-In

5️⃣ Add SDK
In libs.versions.toml and Gradle files:

firebase-auth

firebase-firestore

play-services-auth

credentials-play-services-auth

googleid

App-level build.gradle must apply:
```
plugins {
    alias(libs.plugins.google.gms.google.services)
}
```

✅ Firestore Rules

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null;
    }

    match /orders/{orderId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

🛠 Troubleshooting
Problem	Solution
Firebase errors	Make sure google-services.json is in place
Room issues	Verify AppDatabase is initialized properly
Build fails	JDK 17 must be selected in Project Structure > SDK

🚀 Future Improvements
🗂 Sync Firestore orders to Room when user signs in from a new device

🔐 Use Hilt for dependency injection

🔔 Add push notifications for order status

🌐 Add product search suggestions using Firestore queries

📦 Enhance product upload flow for admin


👨‍💻 Contributions
Pull requests and feature ideas are welcome. Open an issue or submit a PR if you’d like to help improve Shoply!

