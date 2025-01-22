//
//  DigitalResourcesView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 12/10/24.
//

import SwiftUI

struct DigitalResourcesView: View {
    var covers: [Cover]?
    @State private var selectedLink: URLItem?
    @State private var showWebView = false
    @Environment(\.dismiss) private var dismiss // iOS 15+ environment dismiss action

    var body: some View {
        NavigationView {
            if let covers = covers, !covers.isEmpty {
                List {
                    ForEach(covers) { cover in
                        Section(header: Text(cover.desc)
                            .font(.title3)
                            // .fontWeight(.bold)
                            .foregroundColor(.black)
                            .textCase(nil))
                        {
                            if let videos = cover.videos, !videos.isEmpty {
                                Section(header: Text("Videos")
                                    .font(.system(size: 12))
                                    .foregroundColor(.gray)
                                    .textCase(.uppercase))
                                {
                                    ForEach(videos) { video in
                                        Button(video.title) {
                                            if let url = URL(string: video.link) {
                                                selectedLink = URLItem(url: url)
                                                showWebView = true
                                            }
                                        }
                                    }
                                }
                            }

                            if let references = cover.references, !references.isEmpty {
                                Section(header: Text("References")
                                    .font(.system(size: 12))
                                    .foregroundColor(.gray)
                                    .textCase(.uppercase))
                                {
                                    ForEach(references) { reference in
                                        Button(reference.title) {
                                            if let url = URL(string: reference.link) {
                                                selectedLink = URLItem(url: url)
                                                showWebView = true
                                            }
                                        }
                                    }
                                }
                            }

                            if cover.videos?.isEmpty ?? true, cover.references?.isEmpty ?? true {
                                Text("No Videos or References Available")
                            }
                        }
                    }
                }
                .navigationTitle("Digital Resources")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") {
                            dismiss()
                        }
                    }
                }
                .fullScreenCover(item: $selectedLink, onDismiss: {
                    print("Content view dismissed. Cleaning up resources.")
                    showWebView = false
                }, content: { linkItem in
                    AdaptiveContentView(url: linkItem.url)
                })

            } else {
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
    }
}
