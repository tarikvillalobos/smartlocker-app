import SwiftUI
import Security
import SmartLocker

@main
struct SmartLockerApp: App {
    var body: some Scene {
        WindowGroup { LockerView().ignoresSafeArea(.keyboard) }
    }
}

struct LockerView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            readSecret: { key in Keychain.read(key) },
            writeSecret: { key, value in KotlinBoolean(bool: Keychain.write(key, value)) }
        )
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

enum Keychain {
    private static func query(_ key: String) -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: "app.smartlocker.session",
         kSecAttrAccount as String: key]
    }
    static func read(_ key: String) -> String? {
        var request = query(key)
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    static func write(_ key: String, _ value: String?) -> Bool {
        let request = query(key)
        guard let value else {
            let result = SecItemDelete(request as CFDictionary)
            return result == errSecSuccess || result == errSecItemNotFound
        }
        let attributes = [kSecValueData as String: Data(value.utf8)]
        let result = SecItemUpdate(request as CFDictionary, attributes as CFDictionary)
        if result == errSecSuccess { return true }
        guard result == errSecItemNotFound else { return false }
        var addition = request.merging(attributes) { _, new in new }
        addition[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        return SecItemAdd(addition as CFDictionary, nil) == errSecSuccess
    }
}
