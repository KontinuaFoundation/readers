//
//  ReaderIOSApp.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 10/22/24.
//

import SwiftUI

@main
struct ReaderIOSApp: App {
    @StateObject private var initVM = InitializationViewModel()

    var body: some Scene {
        WindowGroup {
            if initVM.isInitialized {
                // Pass any initial data to SplitView as needed.
                SplitView(initialWorkbooks: initVM.workbooks,
                          initialPDFDocument: initVM.pdfDocument)
            } else {
                SplashView()
            }
        }
    }
}

#Preview {
    SplashView()
}
