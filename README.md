# OpenAPI Generator

### Description

Generate modern API Clients in Kotlin from OpenAPI specifications. Supports OpenAPI >= 3.0.0.

A generated example client can be found here: [openapi-kgen-generated](https://github.com/kroegerama/openapi-kgen-generated)

**Features**

- Kotlin
- Coroutines
- Optimized Code (lazy initialization)
- Json De-/Serialization with [Moshi-Codegen](https://github.com/square/moshi#codegen)
- Supports security (no OAuth for now)
- Generates named primitives (create typedefs or inline classes)
- Allows injection of custom request interceptors
- Allows filtering of APIs to only generate a subset of the OpenAPI (e.g. Endpoints tagged with 'App')

## Gradle plugin

###### 1. Add plugin classpath to your root `build.gradle`

```groovy
buildscript {
    [...]
    dependencies {
        [...]
        classpath 'com.kroegerama.kgen:gradle-plugin:<version>'
    }
}
```

###### 2. Apply the plugin at the top of your `build.gradle` inside a module of your project

```groovy
apply plugin: "com.kroegerama.kgen.gradle-plugin"
```

###### 3. Configure the plugin at the end of your module's `build.gradle`

```groovy
kgen {
    //use local spec file
    specFile = file("openapi.yaml")
    //alternative: use uri to an OpenAPI file
    //specUri = "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore.yaml"
    
    //define the package name of the generated classes
    packageName = "com.kroegerama.test"
    
    //optional: limit generated Apis (via endpoint.tag)
    //limitApis = ["App"]
    
    //optional: use inline classes instead of typedef for named primitive types
    //useInlineClasses = true
}
```


## CLI program

```
NAME
        openapi-kgen generate - Generate code from the specified OpenAPI
        Spec.

SYNOPSIS
        openapi-kgen generate [(-d | --dry-run)]
                [(-l <limit apis> | --limit-apis <limit apis>)]
                (-o <output directory> | --output <output directory>)
                [(-p <package name> | --package-name <package name>)]
                [--use-inline-class] [(-v | --verbose)] [--] <spec file>

OPTIONS
        -d, --dry-run
            Do not create any files. Just parse and analyze.

        -l <limit apis>, --limit-apis <limit apis>
            If set, generate only these APIs (set via tag) and their models.
            Comma separated list. Example: "auth,app"

        -o <output directory>, --output <output directory>


        -p <package name>, --package-name <package name>


        --use-inline-class
            Use inline classes for named primitive types. Else use typealias.

        -v, --verbose


        --
            This option can be used to separate command-line options from the
            list of argument, (useful when arguments might be mistaken for
            command-line options

        <spec file>
            Spec file (yaml/json). Can be a file or url.
```

## Usage of the generated code

#### Metadata

A `Metadata` object is generated which contains information about the OpenAPI spec.

### APIs

Each API is generated with a `companion object` which delegates all methods to the actual client.
This allows for lazy (re-) creation of the OkHttpClient and Retrofit instances.
Calling an endpoint is as simple as:

```kotlin
AppApi.getCurrentUser()
``` 

### Inject code into the OkHttpClient / Retrofit builders. E.g. for custom interceptors

If you want to inject custom code in the OkHttp/Retrofit builders, you can create an `ApiDecorator`. Example:

```kotlin
class MyApiDecorator : ApiDecorator {
    override fun OkHttpClient.Builder.decorate() {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
        }
    }

    override fun Retrofit.Builder.decorate() {

    }

}
```

And let the `ApiHolder` object use it:
```kotlin
ApiHolder.decorator = MyApiDecorator()
```
 

### Security

Each security scheme will generate a set/clear method in the `ApiAuthInterceptor` object, which will automatically be used by the `ApiHolder`.

### Gradle module

**Currently the generator only generates source files and no complete gradle module. You can use the following gradle file for android:**

```gradle
apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
apply plugin: 'kotlin-kapt'

android {
    compileSdkVersion 29

    defaultConfig {
        minSdkVersion 23
        targetSdkVersion 29
        versionCode 1
        versionName "1.0"
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = ["-XXLanguage:+InlineClasses"]
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines"

    implementation "com.squareup.moshi:moshi:$moshi"
    implementation "com.squareup.moshi:moshi-adapters:$moshi"
    kapt "com.squareup.moshi:moshi-kotlin-codegen:$moshi"

    implementation 'com.squareup.okhttp3:logging-interceptor:4.3.1'

    implementation "com.squareup.retrofit2:retrofit:$retrofit"
    implementation "com.squareup.retrofit2:converter-moshi:$retrofit"
    implementation "com.squareup.retrofit2:converter-scalars:$retrofit"
}
```

## Used Libraries
### Generator

- [Kotlin Poet](https://github.com/square/kotlinpoet)
- [Koin](https://insert-koin.io/)
- [Airlift Airline](https://github.com/airlift/airline)
- [Swagger Parser](https://github.com/swagger-api/swagger-parser)

### Generated Code

- [Retrofit](https://square.github.io/retrofit/)
- [OkHttp](https://github.com/square/okhttp)
- [Moshi](https://github.com/square/moshi)

## License

```
Copyright 2020 kroegerama

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
