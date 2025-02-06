//
//  SidebarView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import SwiftUI

struct SidebarView: View {
    @Binding var workbooks: [Workbook]?
    @Binding var selectedWorkbookID: String?

    var body: some View {
        if let workbooks = workbooks {
            List(workbooks, selection: $selectedWorkbookID) { workbook in
                HStack {
                    Image(systemName: "icloud.and.arrow.down")
                        .font(.caption)
                        .foregroundColor(.blue)
                    Text(workbook.id)
                        .tag(workbook.id)
                }
            }
        } else {
            ProgressView("Fetching Workbooks")
        }
    }
}
