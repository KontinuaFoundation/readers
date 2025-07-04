//
//  BookmarkSearchView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import SwiftUI

struct BookmarkSearchView: View {
    @Binding var currentPage: Int
    var currentWorkbook: Workbook?
    @ObservedObject var bookmarkManager: BookmarkManager

    var body: some View {
        if let currentWorkbook = currentWorkbook,
           let bookmarks = bookmarkManager.bookmarkLookup[currentWorkbook.id],
           !bookmarks.isEmpty
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
