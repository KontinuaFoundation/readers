//
//  BookmarkView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import SwiftUI

struct BookmarkView: View {
    @Binding var currentPage: Int
    var currentPdfFileName: String?
    var bookmarkLookup: [String: Set<Int>]

    var body: some View {
        if let currentPdfFileName = currentPdfFileName,
           let bookmarks = bookmarkLookup[currentPdfFileName]
        {
            List(Array(bookmarks).sorted(), id: \.self) { bookmark in
                HStack {
                    Text("Page \(bookmark + 1)")
                    Spacer()
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    currentPage = bookmark
                }
            }
        } else {
            Text("No bookmarks available")
                .font(.callout)
                .foregroundColor(.gray)
        }
    }
}
