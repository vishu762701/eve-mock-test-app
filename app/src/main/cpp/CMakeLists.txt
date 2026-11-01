cmake_minimum_required(VERSION 3.22)
project(eve_qr_animation LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
if(ANDROID_ABI STREQUAL "arm64-v8a")
  set(RUST_TARGET aarch64-linux-android)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
  set(RUST_TARGET armv7-linux-androideabi)
elseif(ANDROID_ABI STREQUAL "x86_64")
  set(RUST_TARGET x86_64-linux-android)
elseif(ANDROID_ABI STREQUAL "x86")
  set(RUST_TARGET i686-linux-android)
else()
  message(FATAL_ERROR "Unsupported QR renderer ABI: ${ANDROID_ABI}")
endif()

find_program(CARGO cargo HINTS "$ENV{HOME}/.cargo/bin" REQUIRED)
set(RUST_OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/rust/${RUST_TARGET}/release/libtlottie.a")
file(GLOB_RECURSE RUST_SOURCES CONFIGURE_DEPENDS "vendor/tlottie/src/*.rs")
add_custom_command(OUTPUT "${RUST_OUTPUT}"
  COMMAND ${CMAKE_COMMAND} -E env
    "RUSTUP_TOOLCHAIN=1.99.1"
    "RUSTFLAGS=-C linker=${CMAKE_C_COMPILER} -C link-arg=-Wl,-z,max-page-size=16384"
    ${CARGO} rustc --manifest-path "${CMAKE_CURRENT_SOURCE_DIR}/vendor/tlottie/Cargo.toml"
      --locked --release --lib --features c-api --crate-type staticlib
      --target ${RUST_TARGET} --target-dir "${CMAKE_CURRENT_BINARY_DIR}/rust"
  DEPENDS ${RUST_SOURCES} vendor/tlottie/Cargo.toml vendor/tlottie/Cargo.lock
  VERBATIM)
add_custom_target(tlottie_build DEPENDS "${RUST_OUTPUT}")
add_library(tlottie STATIC IMPORTED)
set_target_properties(tlottie PROPERTIES IMPORTED_LOCATION "${RUST_OUTPUT}")
add_dependencies(tlottie tlottie_build)

add_library(eve_qr_animation SHARED telegram_lottie.cpp)
target_include_directories(eve_qr_animation PRIVATE vendor/tlottie/include)
target_link_options(eve_qr_animation PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries(eve_qr_animation PRIVATE tlottie jnigraphics z log android dl m)
