//
//  ContentViewController.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 01/20/25.
//

import SwiftUI
import WebKit

class ContentViewController: ObservableObject {
    @Published var contentResource: ContentResource

    private var videoDelegate: WebNavigationDelegate?
    private var webDelegate: WebNavigationDelegate?

    init(url: URL) {
        contentResource = ContentResource(url: url)
    }

    // Web navigation delegate methods
    class WebNavigationDelegate: NSObject, WKNavigationDelegate {
        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            print("Web content process terminated. Reloading...")
            webView.reload()
        }

        func webView(_ webView: WKWebView, didFinish _: WKNavigation!) {
            if webView.tag == 100 { // Tag 100 indicates video player
                injectVideoPlayerStyles(into: webView)
            }
        }

        private func injectVideoPlayerStyles(into webView: WKWebView) {
            let css = """
                body > *:not(video):not(.video-container):not(.player) { display: none !important; }
                body { background: black !important; margin: 0 !important; }
                video, .video-container, .player {
                    position: fixed !important;
                    top: 0 !important;
                    left: 0 !important;
                    width: 100vw !important;
                    height: 100vh !important;
                    object-fit: contain !important;
                }
            """

            let javascript = """
                var style = document.createElement('style');
                style.innerHTML = `\(css)`;
                document.head.appendChild(style);

                var videos = document.getElementsByTagName('video');
                for(var i = 0; i < videos.length; i++) {
                    videos[i].style.width = '100%';
                    videos[i].style.height = '100%';
                    videos[i].play();
                }
            """

            webView.evaluateJavaScript(javascript, completionHandler: nil)
        }
    }

    func createWebView(for type: ContentType) -> WKWebView {
        switch type {
        case .youtubeVideo, .otherVideo:
            createVideoPlayerWebView()
        case .webpage:
            createRegularWebView()
        }
    }

    private func createVideoPlayerWebView() -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.tag = 100 // Tag to identify video player

        // Create and store a strong reference to the delegate
        videoDelegate = WebNavigationDelegate()
        webView.navigationDelegate = videoDelegate

        webView.scrollView.isScrollEnabled = false
        webView.backgroundColor = .black

        return webView
    }

    private func createRegularWebView() -> WKWebView {
        let webView = WKWebView()

        // Create and store a strong reference to the delegate
        webDelegate = WebNavigationDelegate()
        webView.navigationDelegate = webDelegate

        return webView
    }

    func loadContent(in webView: WKWebView) {
        let request = URLRequest(url: contentResource.embedURL)
        webView.load(request)
    }

    func cleanup(webView: WKWebView) {
        webView.stopLoading()
        webView.navigationDelegate = nil
        // Clear delegate references
        videoDelegate = nil
        webDelegate = nil
    }
}
