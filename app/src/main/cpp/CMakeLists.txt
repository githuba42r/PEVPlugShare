cmake_minimum_required(VERSION 3.22.1)

project("pevplugshare")

# OAuth secrets are passed as compile definitions from Gradle
# These are read from secrets.properties (not in git)
# Example: -DOAUTH_DEV_CALLBACK_URL="https://..."

# Create native library
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-secrets.cpp)

# Pass OAuth secrets as compile definitions to the C++ code
if(DEFINED OAUTH_DEV_CALLBACK_URL)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_DEV_CALLBACK_URL="${OAUTH_DEV_CALLBACK_URL}"
    )
endif()

if(DEFINED OAUTH_DEV_SHARED_SECRET)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_DEV_SHARED_SECRET="${OAUTH_DEV_SHARED_SECRET}"
    )
endif()

if(DEFINED OAUTH_DEV_DEEP_LINK_SCHEME)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_DEV_DEEP_LINK_SCHEME="${OAUTH_DEV_DEEP_LINK_SCHEME}"
    )
endif()

if(DEFINED OAUTH_PROD_CALLBACK_URL)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_PROD_CALLBACK_URL="${OAUTH_PROD_CALLBACK_URL}"
    )
endif()

if(DEFINED OAUTH_PROD_SHARED_SECRET)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_PROD_SHARED_SECRET="${OAUTH_PROD_SHARED_SECRET}"
    )
endif()

if(DEFINED OAUTH_PROD_DEEP_LINK_SCHEME)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        OAUTH_PROD_DEEP_LINK_SCHEME="${OAUTH_PROD_DEEP_LINK_SCHEME}"
    )
endif()

# Link against Android log library
find_library(log-lib log)
target_link_libraries(${CMAKE_PROJECT_NAME} ${log-lib})

# 16 KB page-size support: NDK r26 and older link shared libraries with 4 KB
# LOAD alignment, which Google Play flags as an app-compatibility warning for
# apps targeting SDK 35+. (androidx.camera ships its own libs aligned already.)
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE "-Wl,-z,max-page-size=16384")

# Strip debug symbols in release builds for additional security
if(CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -s")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -s")
endif()
