import SwiftUI
import shared

struct ContentView: UIViewControllerRepresentable {
	  func makeUIViewController(context: Context) -> UIViewController {
            MainViewControllerKt.MainViewController() //引入我们的iOS入口
        }

	 func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler

    }
}

//struct ContentView_Previews: PreviewProvider {
//	static var previews: some View {
//		ContentView()
//	}
//}