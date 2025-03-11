//
//  DigitalResourcesView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 12/10/24.
//

import SwiftUI

struct URLItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct DigitalResourcesView: View {
    var covers: [Cover]?
    @State private var selectedLink: URLItem?
    @Environment(\.dismiss) private var dismiss // iOS 15+ dismiss action

    var body: some View {
        NavigationView {
            List {
                if let covers = covers, !covers.isEmpty {
                    ForEach(covers, id: \.id) { cover in
                        coverSection(cover)
                    }
                } else {
                    emptyStateView()
                }
            }
            .navigationTitle("Digital Resources")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .fullScreenCover(item: $selectedLink) { linkItem in
                AdaptiveContentView(url: linkItem.url)
            }
        }
    }

    // MARK: - Helper Views

    @ViewBuilder
    private func coverSection(_ cover: Cover) -> some View {
        Section(header: Text(cover.desc)
            .font(.title3)
            .foregroundColor(.black)
            .textCase(nil))
        {
            videoSection(cover.videos)
            referenceSection(cover.references)

            if cover.videos?.isEmpty ?? true, cover.references?.isEmpty ?? true {
                Text("No Videos or References Available").foregroundColor(.gray)
            }
        }
    }

    @ViewBuilder
    private func videoSection(_ videos: [Video]?) -> some View {
        if let videos = videos, !videos.isEmpty {
            Section(header: Text("Videos")
                .font(.system(size: 12))
                .foregroundColor(.gray)
                .textCase(.uppercase))
            {
                ForEach(videos, id: \.link) { video in
                    linkButton(title: video.title, urlString: video.link)
                }
            }
        }
    }

    @ViewBuilder
    private func referenceSection(_ references: [Reference]?) -> some View {
        if let references = references, !references.isEmpty {
            Section(header: Text("References")
                .font(.system(size: 12))
                .foregroundColor(.gray)
                .textCase(.uppercase))
            {
                ForEach(references, id: \.link) { reference in
                    linkButton(title: reference.title, urlString: reference.link)
                }
            }
        }
    }

    @ViewBuilder
    private func linkButton(title: String, urlString: String) -> some View {
        if let url = URL(string: urlString) {
            Button(title) {
                selectedLink = URLItem(url: url)
            }
        }
    }

    @ViewBuilder
    private func emptyStateView() -> some View {
        VStack {
            Text("No Digital Resources Available")
                .foregroundColor(.gray)
                .padding()
            Button("Close") {
                dismiss()
            }
            .padding()
        }
    }
}
