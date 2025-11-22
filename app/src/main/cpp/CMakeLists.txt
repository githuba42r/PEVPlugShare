cmake_minimum_required(VERSION 3.22.1)

project("pevplugshare")

# OAuth secrets are passed as compile definitions from Gradle
# These are read from secrets.properties (not in git)
# Example: -DOAUTH_DEV_CALLBACK_URL="https://..."

# Create native library
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-secrets.cpp)

# Link against Android log library
find_library(log-lib log)
target_link_libraries(${CMAKE_PROJECT_NAME} ${log-lib})

# Strip debug symbols in release builds for additional security
if(CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -s")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -s")
endif()
