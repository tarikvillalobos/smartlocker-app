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
