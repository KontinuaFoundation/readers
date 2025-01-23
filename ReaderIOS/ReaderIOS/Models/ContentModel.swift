//
//  ContentModel.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 01/20/25.
//

import Foundation

enum ContentType {
    case youtubeVideo
    case otherVideo
    case webpage
}

class ContentResource: ObservableObject {
    @Published var url: URL
    @Published var type: ContentType

    var embedURL: URL {
        switch type {
        case .youtubeVideo:
            convertToYouTubeEmbed(url)
        case .otherVideo, .webpage:
            url
        }
    }

    init(url: URL) {
        self.url = url
        type = Self.detectContentType(for: url)
    }

    private static func detectContentType(for url: URL) -> ContentType {
        let urlString = url.absoluteString.lowercased()

        // YouTube detection
        if urlString.contains("youtube.com/watch") || urlString.contains("youtu.be/") {
            return .youtubeVideo
        }

        // Other video platforms
        let videoHosts = [
            "vimeo.com",
            "dailymotion.com",
            "twitch.tv"
        ]

        if let host = url.host?.lowercased(),
           videoHosts.contains(where: { host.contains($0) })
        {
            return .otherVideo
        }

        // Video file extensions
        let videoExtensions = ["mp4", "mov", "m4v", "avi", "mkv", "webm"]
        if let fileExtension = url.pathExtension.lowercased() as String?,
           videoExtensions.contains(fileExtension)
        {
            return .otherVideo
        }

        return .webpage
    }

    private func convertToYouTubeEmbed(_ url: URL) -> URL {
        let urlString = url.absoluteString

        // Handle youtu.be links
        if urlString.contains("youtu.be/") {
            let id = urlString.components(separatedBy: "youtu.be/")[1].components(separatedBy: "?")[0]
            return URL(string: "https://www.youtube.com/embed/\(id)")!
        }

        // Handle regular youtube.com links
        if urlString.contains("watch?v=") {
            let id = urlString.components(separatedBy: "watch?v=")[1].components(separatedBy: "&")[0]
            return URL(string: "https://www.youtube.com/embed/\(id)")!
        }

        return url
    }
}
