# Keep Model classes
-keep class com.example.dictionary.data.model.** { *; }

# Keep Fragment classes
-keep class com.example.dictionary.ui.home.HomeFragment { *; }
-keep class com.example.dictionary.ui.chat.ChatFragment { *; }
-keep class com.example.dictionary.ui.history.HistoryFragment { *; }
-keep class com.example.dictionary.ui.favorite.FavoriteFragment { *; }

# Keep Activity classes
-keep class com.example.dictionary.ui.MainActivity { *; }
-keep class com.example.dictionary.ui.scan.ScanActivity { *; }

# Keep all Fragment and Activity subclasses
-keep public class * extends androidx.fragment.app.Fragment { *; }
-keep public class * extends androidx.appcompat.app.AppCompatActivity { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep interface com.squareup.okhttp.** { *; }

# Gemini SDK
-keep class com.google.ai.client.** { *; }

# MLKit
-keep class com.google.mlkit.** { *; }
