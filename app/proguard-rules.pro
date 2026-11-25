# Keep Room entities
-keep class **TransactionEntity { *; }
-keep class **BudgetPlanEntity { *; }
-keep class **SavingsGoalEntity { *; }
-keep class **CategoryEntity { *; }

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep class * implements com.squareup.moshi.JsonAdapter

# Moshi models: the runtime builder uses KotlinJsonAdapterFactory
# (reflection), so keep the DTOs even though @JsonClass codegen exists
-keep class ru.moneyapp.data.remote.** { *; }
-keep class ru.moneyapp.data.auth.** { *; }

# Domain models: serialized reflectively by Moshi for the JSON backup
-keep class ru.moneyapp.domain.model.** { *; }

# Keep Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# XmlBeans (POI OOXML runtime) and log4j-api (POI's logger) rely on
# reflection over their own classes (e.g. StatusLogger instantiates
# DefaultFlowMessageFactory via getDeclaredConstructor()), so both
# must stay unobfuscated
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

# XmlBeans generated schema classes (poi-ooxml-lite): the generated
# Factory classes register them into the type system and POI casts
# parse()/newInstance() results to these interfaces — renaming them
# breaks the casts (ClassCastException in XSSFWorkbook init).
# dontwarn: the lite jar is a subset, kept members may reference
# schema types that are not shipped
-dontwarn org.openxmlformats.schemas.**
-keep class org.openxmlformats.schemas.** { *; }

# Optional POI/xmlbeans/log4j dependencies and compile-time annotations:
# XSLT (Saxon), OSGi service loading, AWT, StAX — never used at runtime by
# this app, so R8 missing-class warnings for them are suppressed.
-dontwarn aQute.bnd.**
-dontwarn com.google.errorprone.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**

# Room
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
