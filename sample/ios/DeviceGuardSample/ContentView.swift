import SwiftUI
import UIKit
import SampleShared

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

/// Bridges the Kotlin/Compose UIViewController into SwiftUI's view tree. The factory
/// itself lives in `sample/shared/src/iosMain/kotlin/.../Platform.ios.kt` as
/// `MainViewController()`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Platform_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No-op; the Compose side owns its own state.
    }
}
