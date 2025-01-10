//
//  WebView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 11/19/24.
//

import SwiftUI
import WebKit

struct WebView: UIViewRepresentable {
    let url: URL

    class Coordinator: NSObject, WKNavigationDelegate {
        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            print("Web content process terminated. Cleaning up.")
            webView.reload()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context _: Context) {
        let request = URLRequest(url: url)
        uiView.load(request)
    }

    static func dismantleUIView(_ uiView: WKWebView, coordinator _: Coordinator) {
        uiView.stopLoading()
        uiView.navigationDelegate = nil
        print("WebView dismantled and cleaned up.")
    }
}

struct FullScreenWebView: View {
    let url: URL
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            WebView(url: url)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") {
                            dismiss()
                        }
                    }
                }
        }
        .ignoresSafeArea(edges: .all)
    }
}
