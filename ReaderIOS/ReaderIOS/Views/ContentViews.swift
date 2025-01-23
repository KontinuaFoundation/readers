//
//  ContentViews.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 01/20/25.
//

import SwiftUI
import WebKit

struct VideoPlayerView: UIViewRepresentable {
    let url: URL
    let isYouTube: Bool

    init(resource: ContentResource) {
        url = resource.embedURL
        isYouTube = resource.type == .youtubeVideo
    }

    // Extract YouTube script configuration to a separate function
    private func createYouTubeScript() -> WKUserScript {
        let script = WKUserScript(
            source: """
                document.body.style.margin = '0';
                document.body.style.padding = '0';
                document.body.style.backgroundColor = 'black';

                function styleIframe() {
                    var iframe = document.querySelector('iframe');
                    if (iframe) {
                        iframe.style.position = 'fixed';
                        iframe.style.top = '0';
                        iframe.style.left = '0';
                        iframe.style.width = '100%';
                        iframe.style.height = '100%';
                        iframe.style.border = 'none';
                        iframe.style.margin = '0';
                        iframe.style.padding = '0';
                    }
                }

                styleIframe();

                var observer = new MutationObserver(function() {
                    styleIframe();
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                var style = document.createElement('style');
                style.textContent = `
                    .ytp-chrome-bottom,
                    .ytp-watermark,
                    .ytp-youtube-button,
                    .ytp-show-cards-title
                `;
                document.head.appendChild(style);
            """,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: false
        )
        return script
    }

    // Extract configuration setup to a separate function
    private func createWebViewConfiguration() -> WKWebViewConfiguration {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        if isYouTube {
            let script = createYouTubeScript()
            config.userContentController.addUserScript(script)
        }

        return config
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = createWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)

        webView.navigationDelegate = context.coordinator
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black

        return webView
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(isYouTube: isYouTube)
    }

    func updateUIView(_ webView: WKWebView, context _: Context) {
        if isYouTube {
            let urlString = url.absoluteString
            let updatedURLString =
                "\(urlString)?autoplay=1&rel=0&showinfo=0&modestbranding=1&playsinline=1&fs=0&iv_load_policy=3"
            if let finalURL = URL(string: updatedURLString) {
                let request = URLRequest(url: finalURL)
                webView.load(request)
            }
        } else {
            let request = URLRequest(url: url)
            webView.load(request)
        }
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        let isYouTube: Bool

        init(isYouTube: Bool) {
            self.isYouTube = isYouTube
            super.init()
        }

        func webView(_ webView: WKWebView, didFinish _: WKNavigation!) {
            if !isYouTube {
                // Only inject custom styles for non-YouTube videos
                let css = """
                    body > *:not(video):not(.video-container):not(.player) { display: none !important; }
                    body { background: black !important; margin: 0 !important; padding: 0 !important; }
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
    }
}

struct WebContentView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        return webView
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func updateUIView(_ webView: WKWebView, context _: Context) {
        let request = URLRequest(url: url)
        webView.load(request)
    }

    class Coordinator: NSObject, WKNavigationDelegate {}
}

struct AdaptiveContentView: View {
    let url: URL
    @Environment(\.dismiss) var dismiss
    @StateObject private var contentResource: ContentResource

    init(url: URL) {
        self.url = url
        _contentResource = StateObject(wrappedValue: ContentResource(url: url))
    }

    var body: some View {
        NavigationView {
            Group {
                switch contentResource.type {
                case .youtubeVideo, .otherVideo:
                    VideoPlayerView(resource: contentResource)
                case .webpage:
                    WebContentView(url: url)
                }
            }
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
