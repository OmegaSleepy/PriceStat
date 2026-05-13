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

#[unsafe(no_mangle)]
pub extern "C" fn read_and_print(c_buf: *const c_char) {
    let file_name_c = unsafe { CStr::from_ptr(c_buf) };

    if let Ok(file_name) = file_name_c.to_str() {
        let result = fs::read(Path::new(file_name));

        match result {
            Ok(file_contents) => {
                let msg = String::from_utf8(file_contents).unwrap();
                println!("{}", msg);
            }
            Err(e) => eprintln!("Error when reading file {} \n {} ", file_name, e)
        }
    }
}
