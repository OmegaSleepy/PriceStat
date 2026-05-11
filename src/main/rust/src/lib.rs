use std::ffi::CStr;
use std::os::raw::c_char;

#[unsafe(no_mangle)]
pub extern "C" fn add_numbers(a: i32, b: i32) -> i32 {
    a + b
}
#[unsafe(no_mangle)]
pub extern "C" fn print_message(c_buf: *const c_char) {
    let c_str = unsafe { CStr::from_ptr(c_buf) };
    if let Ok(s) = c_str.to_str() {
        println!("Rust received: {}", s);
    }
}